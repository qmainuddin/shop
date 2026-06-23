"""
analytics-svc — FastAPI entry point.

Endpoints
---------
GET /health   → {"status": "UP"}
GET /metrics  → {"total_orders": int, "orders_by_user": {userId: int}}

The RabbitMQ consumer is started as a background asyncio task via the
FastAPI lifespan context manager.
"""

import asyncio
import logging
import os
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI

from consumer import increment_counters, start_consumer  # noqa: F401 (re-exported for tests)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Shared in-memory counters — written by consumer, read by /metrics.
_counters: dict = {
    "total_orders": 0,
    "orders_by_user": {},
}


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Start the RabbitMQ consumer as a background task."""
    task = asyncio.create_task(start_consumer(_counters))
    logger.info("analytics: consumer task started")
    try:
        yield
    finally:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass
        logger.info("analytics: consumer task stopped")


app = FastAPI(title="analytics-svc", lifespan=lifespan)


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/metrics")
def metrics() -> dict:
    return {
        "total_orders": _counters["total_orders"],
        "orders_by_user": dict(_counters["orders_by_user"]),
    }


# Expose a helper so tests can inject events without a real broker.
def _get_counters() -> dict:
    """Return the live counters dict (used by tests)."""
    return _counters


def _reset_counters() -> None:
    """Reset counters to zero (used by tests)."""
    _counters["total_orders"] = 0
    _counters["orders_by_user"] = {}


if __name__ == "__main__":
    import uvicorn

    port = int(os.getenv("ANALYTICS_PORT", "8085"))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False)
