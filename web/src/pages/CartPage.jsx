import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function CartPage() {
  const { token } = useAuth();
  const { items, removeFromCart, clearCart } = useCart();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [orderId, setOrderId] = useState(null);

  const total = items.reduce((sum, item) => {
    const price = typeof item.price === 'number' ? item.price : parseFloat(item.price || 0);
    return sum + price * item.quantity;
  }, 0);

  async function handlePlaceOrder() {
    if (!token) {
      navigate('/login');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const orderPayload = {
        items: items.map((i) => ({
          productId: i.id,
          quantity: i.quantity,
          price: typeof i.price === 'number' ? i.price : parseFloat(i.price || 0),
        })),
        totalAmount: total,
      };
      const data = await api.post('/api/orders', orderPayload);
      const id = data.id || data.orderId || data.order_id || data;
      setOrderId(id);
      clearCart();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (orderId) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <div className="bg-green-50 border border-green-200 rounded-xl p-8">
          <div className="text-5xl mb-4">&#10003;</div>
          <h2 className="text-2xl font-bold text-green-800 mb-2">Order Placed!</h2>
          <p className="text-green-700 mb-1">Your order has been submitted successfully.</p>
          <p className="text-green-600 text-sm font-mono bg-green-100 inline-block px-3 py-1 rounded mt-2">
            Order ID: {String(orderId)}
          </p>
          <div className="mt-6">
            <Link
              to="/"
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-6 py-2 rounded-lg transition-colors inline-block"
            >
              Continue Shopping
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Your Cart</h1>

      {items.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-500 mb-4">Your cart is empty.</p>
          <Link
            to="/"
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold px-6 py-2 rounded-lg transition-colors inline-block"
          >
            Browse Products
          </Link>
        </div>
      ) : (
        <>
          <div className="space-y-3 mb-6">
            {items.map((item) => {
              const price =
                typeof item.price === 'number'
                  ? item.price.toFixed(2)
                  : parseFloat(item.price || 0).toFixed(2);
              return (
                <div
                  key={item.id}
                  className="flex items-center justify-between bg-white border border-gray-200 rounded-lg px-4 py-3 shadow-sm"
                >
                  <div>
                    <p className="font-medium text-gray-900">{item.name}</p>
                    <p className="text-sm text-gray-500">
                      ${price} x {item.quantity}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-semibold text-gray-800">
                      ${(parseFloat(price) * item.quantity).toFixed(2)}
                    </span>
                    <button
                      onClick={() => removeFromCart(item.id)}
                      className="text-red-400 hover:text-red-600 text-sm transition-colors"
                      aria-label="Remove item"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="border-t border-gray-200 pt-4 mb-6 flex justify-between items-center">
            <span className="font-semibold text-gray-700">Total</span>
            <span className="text-xl font-bold text-indigo-700">${total.toFixed(2)}</span>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
              {error}
            </div>
          )}

          {!token && (
            <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4">
              You must be{' '}
              <Link to="/login" className="underline font-medium">
                signed in
              </Link>{' '}
              to place an order.
            </p>
          )}

          <button
            onClick={handlePlaceOrder}
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold py-3 rounded-lg transition-colors"
          >
            {loading ? 'Placing Order...' : 'Place Order'}
          </button>
        </>
      )}
    </div>
  );
}
