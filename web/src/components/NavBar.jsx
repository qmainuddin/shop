import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function NavBar() {
  const { username, logout } = useAuth();
  const { totalCount } = useCart();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="bg-indigo-700 text-white shadow-md">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/" className="text-xl font-bold tracking-tight hover:text-indigo-200 transition-colors">
          Shop
        </Link>
        <div className="flex items-center gap-4 text-sm font-medium">
          <Link to="/" className="hover:text-indigo-200 transition-colors">
            Products
          </Link>
          <Link to="/cart" className="relative hover:text-indigo-200 transition-colors">
            Cart
            {totalCount > 0 && (
              <span className="ml-1 bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5">
                {totalCount}
              </span>
            )}
          </Link>
          {username ? (
            <>
              <span className="text-indigo-200">Hi, {username}</span>
              <button
                onClick={handleLogout}
                className="bg-indigo-600 hover:bg-indigo-500 px-3 py-1.5 rounded transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="hover:text-indigo-200 transition-colors">
                Login
              </Link>
              <Link
                to="/register"
                className="bg-indigo-600 hover:bg-indigo-500 px-3 py-1.5 rounded transition-colors"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
