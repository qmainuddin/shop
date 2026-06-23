"""
Unit tests for analytics-svc.

RabbitMQ is fully mocked — no broker required.
Run with:  python -m pytest test_main.py -v
"""

import json

import pytest
from httpx import ASGITransport, AsyncClient

import main as svc
from consumer import increment_counters


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def reset_counters():
    """Ensure counters start at zero before each test."""
    svc._reset_counters()
    yield
    svc._reset_counters()


# Prevent the real lifespan (which would try to connect to RabbitMQ) from
# running during HTTP tests.  We replace it with a no-op context manager.
from contextlib import asynccontextmanager

@asynccontextmanager
async def _noop_lifespan(app):
    yield


@pytest.fixture()
def test_app():
    """Return a FastAPI app instance with the lifespan patched out."""
    svc.app.router.lifespan_context = _noop_lifespan
    return svc.app


@pytest.fixture()
async def client(test_app):
    transport = ASGITransport(app=test_app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


# ---------------------------------------------------------------------------
# Test 1 — GET /health
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_health_returns_200_and_status_up(client):
    response = await client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


# ---------------------------------------------------------------------------
# Test 2 — GET /metrics returns valid schema
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_metrics_returns_200_with_integer_total_orders(client):
    response = await client.get("/metrics")
    assert response.status_code == 200
    body = response.json()
    assert "total_orders" in body
    assert isinstance(body["total_orders"], int)
    assert "orders_by_user" in body
    assert isinstance(body["orders_by_user"], dict)


# ---------------------------------------------------------------------------
# Test 3 — counter increment is reflected in /metrics
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_metrics_reflects_incremented_counters(client):
    counters = svc._get_counters()

    # Simulate two order.placed messages
    msg1 = json.dumps({"orderId": "aaa-111", "userId": "user-1", "totalAmount": "49.99"}).encode()
    msg2 = json.dumps({"orderId": "bbb-222", "userId": "user-2", "totalAmount": "19.99"}).encode()
    msg3 = json.dumps({"orderId": "ccc-333", "userId": "user-1", "totalAmount": "9.99"}).encode()

    increment_counters(counters, msg1)
    increment_counters(counters, msg2)
    increment_counters(counters, msg3)

    response = await client.get("/metrics")
    assert response.status_code == 200
    body = response.json()

    assert body["total_orders"] == 3
    assert body["orders_by_user"]["user-1"] == 2
    assert body["orders_by_user"]["user-2"] == 1


# ---------------------------------------------------------------------------
# Test 4 — increment_counters handles missing userId gracefully
# ---------------------------------------------------------------------------

def test_increment_counters_tolerates_missing_user_id():
    counters = {"total_orders": 0, "orders_by_user": {}}
    msg = json.dumps({"orderId": "xyz-999"}).encode()  # no userId field
    increment_counters(counters, msg)

    assert counters["total_orders"] == 1
    assert counters["orders_by_user"] == {}


# ---------------------------------------------------------------------------
# Test 5 — increment_counters handles malformed JSON gracefully
# ---------------------------------------------------------------------------

def test_increment_counters_tolerates_bad_json():
    counters = {"total_orders": 5, "orders_by_user": {}}
    increment_counters(counters, b"not-valid-json{{{")

    # Counter must NOT change on bad input
    assert counters["total_orders"] == 5
