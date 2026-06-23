import { useCart } from '../context/CartContext';

export default function ProductCard({ product }) {
  const { addToCart } = useCart();

  const price =
    typeof product.price === 'number'
      ? product.price.toFixed(2)
      : parseFloat(product.price || 0).toFixed(2);

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow flex flex-col">
      <div className="bg-indigo-50 rounded-t-xl h-40 flex items-center justify-center">
        <span className="text-5xl text-indigo-300 select-none">
          {product.name ? product.name.charAt(0).toUpperCase() : '?'}
        </span>
      </div>
      <div className="p-4 flex flex-col flex-1">
        <h3 className="font-semibold text-gray-900 text-base leading-snug mb-1">
          {product.name}
        </h3>
        <p className="text-gray-500 text-sm flex-1 mb-3 line-clamp-3">
          {product.description || 'No description available.'}
        </p>
        <div className="flex items-center justify-between mt-auto">
          <span className="text-indigo-700 font-bold text-lg">${price}</span>
          <button
            onClick={() => addToCart(product)}
            className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm px-3 py-1.5 rounded-lg transition-colors"
          >
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
}
