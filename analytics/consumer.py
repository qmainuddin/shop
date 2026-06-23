"""
RabbitMQ consumer for analytics-svc.

Connects to the `orders` topic exchange and listens for `order.placed`
routing-key messages.  Increments in-memory counters shared with main.py.
"""

import asyncio
import json
import logging
import os

import aio_pika

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "shop")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "shop")

# Exchange name must match order-svc RabbitConfig.ORDERS_EXCHANGE = "orders"
# Routing key used by order-svc: "order.placed"
EXCHANGE_NAME = "orders"
EXCHANGE_TYPE = aio_pika.ExchangeType.TOPIC
ROUTING_KEY = "order.placed"
QUEUE_NAME = "analytics.order.placed"

_RETRY_DELAY_SECONDS = 5
_MAX_ATTEMPTS = 12


def _amqp_url() -> str:
    return (
        f"amqp://{RABBITMQ_USER}:{RABBITMQ_PASSWORD}"
        f"@{RABBITMQ_HOST}:{RABBITMQ_PORT}/"
    )


def increment_counters(counters: dict, body: bytes) -> None:
    """
    Parse *body* as JSON and update *counters* in-place.

    counters layout:
        {
            "total_orders": int,
            "orders_by_user": {str(userId): int}
        }

    Called both from the live consumer and from tests.
    """
    try:
        data = json.loads(body)
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        logger.warning("analytics: could not parse message body: %s", exc)
        return

    counters["total_orders"] = counters.get("total_orders", 0) + 1

    user_id = data.get("userId")
    if user_id is not None:
        user_id_str = str(user_id)
        by_user: dict = counters.setdefault("orders_by_user", {})
        by_user[user_id_str] = by_user.get(user_id_str, 0) + 1


async def start_consumer(counters: dict) -> None:
    """
    Retry-loop that keeps the consumer alive until it connects successfully
    and then processes messages until the task is cancelled.
    """
    url = _amqp_url()
    for attempt in range(1, _MAX_ATTEMPTS + 1):
        try:
            logger.info(
                "analytics: connecting to RabbitMQ (attempt %d/%d) …",
                attempt,
                _MAX_ATTEMPTS,
            )
            connection: aio_pika.abc.AbstractRobustConnection = (
                await aio_pika.connect_robust(url)
            )
            logger.info("analytics: connected to RabbitMQ")
            break
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                "analytics: RabbitMQ not ready (%s); retrying in %ds",
                exc,
                _RETRY_DELAY_SECONDS,
            )
            if attempt == _MAX_ATTEMPTS:
                logger.error(
                    "analytics: giving up after %d attempts", _MAX_ATTEMPTS
                )
                return
            await asyncio.sleep(_RETRY_DELAY_SECONDS)

    async with connection:
        channel: aio_pika.abc.AbstractChannel = await connection.channel()
        await channel.set_qos(prefetch_count=10)

        exchange: aio_pika.abc.AbstractExchange = await channel.declare_exchange(
            EXCHANGE_NAME, EXCHANGE_TYPE, durable=True
        )

        queue: aio_pika.abc.AbstractQueue = await channel.declare_queue(
            QUEUE_NAME, durable=True
        )
        await queue.bind(exchange, routing_key=ROUTING_KEY)

        logger.info(
            "analytics: listening on queue '%s' bound to exchange '%s' / key '%s'",
            QUEUE_NAME,
            EXCHANGE_NAME,
            ROUTING_KEY,
        )

        async with queue.iterator() as queue_iter:
            async for message in queue_iter:
                async with message.process():
                    increment_counters(counters, message.body)
                    logger.debug(
                        "analytics: processed message, total_orders=%d",
                        counters.get("total_orders", 0),
                    )
