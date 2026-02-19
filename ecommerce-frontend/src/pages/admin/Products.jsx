import React, { useState, useEffect } from 'react';
import {
    Plus,
    Search,
    Filter,
    Edit,
    Trash2,
    Copy,
    Eye,
    EyeOff,
    Download,
    Upload,
    MoreVertical,
    ChevronLeft,
    ChevronRight,
    X,
    Save,
    AlertCircle,
    CheckCircle,
    Package,
    DollarSign,
    TrendingUp,
    BarChart3
} from 'lucide-react';
import { adminApi } from '../../api/adminApi';

const Products = () => {
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [showBulkModal, setShowBulkModal] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [selectedProducts, setSelectedProducts] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [filters, setFilters] = useState({
        category: '',
        inStock: '',
        active: '',
        minPrice: '',
        maxPrice: ''
    });
    const [pagination, setPagination] = useState({
        page: 0,
        size: 10,
        totalPages: 0,
        totalElements: 0
    });
    const [stats, setStats] = useState(null);

    // Form state
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        price: '',
        discountPrice: '',
        sku: '',
        stockQuantity: '',
        categoryId: '',
        isActive: true,
        specifications: {},
        image: null
    });

    useEffect(() => {
        fetchProducts();
        fetchCategories();
        fetchStats();
    }, [pagination.page, searchTerm, filters]);

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAllProducts(
                pagination.page,
                pagination.size,
                filters.category,
                searchTerm,
                filters.inStock === 'true' ? true : filters.inStock === 'false' ? false : null,
                filters.active === 'true' ? true : filters.active === 'false' ? false : null
            );
            setProducts(response.data.content);
            setPagination({
                ...pagination,
                totalPages: response.data.totalPages,
                totalElements: response.data.totalElements
            });
        } catch (error) {
            console.error('Error fetching products:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchCategories = async () => {
        try {
            const response = await adminApi.getAllCategories();
            setCategories(response.data);
        } catch (error) {
            console.error('Error fetching categories:', error);
        }
    };

    const fetchStats = async () => {
        try {
            const response = await adminApi.getProductStats();
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching stats:', error);
        }
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked, files } = e.target;
        if (type === 'file') {
            setFormData({ ...formData, image: files[0] });
        } else if (type === 'checkbox') {
            setFormData({ ...formData, [name]: checked });
        } else {
            setFormData({ ...formData, [name]: value });
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const formDataToSend = new FormData();
            formDataToSend.append('product', JSON.stringify(formData));
            if (formData.image) {
                formDataToSend.append('image', formData.image);
            }

            if (selectedProduct) {
                await adminApi.updateProduct(selectedProduct.id, formDataToSend);
            } else {
                await adminApi.createProduct(formDataToSend);
            }

            fetchProducts();
            fetchStats();
            setShowModal(false);
            resetForm();
        } catch (error) {
            console.error('Error saving product:', error);
        }
    };

    const handleDelete = async () => {
        try {
            if (selectedProducts.length > 0) {
                await adminApi.bulkDeleteProducts(selectedProducts);
                setSelectedProducts([]);
            } else if (selectedProduct) {
                await adminApi.deleteProduct(selectedProduct.id);
            }
            fetchProducts();
            fetchStats();
            setShowDeleteModal(false);
            setSelectedProduct(null);
        } catch (error) {
            console.error('Error deleting product:', error);
        }
    };

    const handleDuplicate = async (id) => {
        try {
            await adminApi.duplicateProduct(id);
            fetchProducts();
        } catch (error) {
            console.error('Error duplicating product:', error);
        }
    };

    const handleToggleStatus = async (id) => {
        try {
            await adminApi.toggleProductStatus(id);
            fetchProducts();
        } catch (error) {
            console.error('Error toggling product status:', error);
        }
    };

    const handleExport = async () => {
        try {
            const response = await adminApi.exportProducts(selectedProducts);
            const dataStr = JSON.stringify(response.data, null, 2);
            const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);
            const exportFileDefaultName = `products_export_${new Date().toISOString()}.json`;

            const linkElement = document.createElement('a');
            linkElement.setAttribute('href', dataUri);
            linkElement.setAttribute('download', exportFileDefaultName);
            linkElement.click();
        } catch (error) {
            console.error('Error exporting products:', error);
        }
    };

    const resetForm = () => {
        setFormData({
            name: '',
            description: '',
            price: '',
            discountPrice: '',
            sku: '',
            stockQuantity: '',
            categoryId: '',
            isActive: true,
            specifications: {},
            image: null
        });
        setSelectedProduct(null);
    };

    const openEditModal = (product) => {
        setSelectedProduct(product);
        setFormData({
            name: product.name || '',
            description: product.description || '',
            price: product.price || '',
            discountPrice: product.discountPrice || '',
            sku: product.sku || '',
            stockQuantity: product.stockQuantity || '',
            categoryId: product.categoryId || '',
            isActive: product.isActive !== undefined ? product.isActive : true,
            specifications: product.specifications || {},
            image: null
        });
        setShowModal(true);
    };

    const toggleProductSelection = (productId) => {
        setSelectedProducts(prev =>
            prev.includes(productId)
                ? prev.filter(id => id !== productId)
                : [...prev, productId]
        );
    };

    const selectAllProducts = () => {
        if (selectedProducts.length === products.length) {
            setSelectedProducts([]);
        } else {
            setSelectedProducts(products.map(p => p.id));
        }
    };

    const StatCard = ({ title, value, icon: Icon, color }) => (
        <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm text-gray-600">{title}</p>
                    <p className="text-2xl font-semibold mt-1">{value}</p>
                </div>
                <div className={`p-3 rounded-full bg-${color}-100`}>
                    <Icon className={`text-${color}-600`} size={24} />
                </div>
            </div>
        </div>
    );

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Products Management</h1>
                <div className="flex space-x-2">
                    <button
                        onClick={() => {
                            resetForm();
                            setShowModal(true);
                        }}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700 flex items-center"
                    >
                        <Plus size={16} className="mr-2" />
                        Add Product
                    </button>
                    {selectedProducts.length > 0 && (
                        <>
                            <button
                                onClick={() => setShowBulkModal(true)}
                                className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-purple-700"
                            >
                                Bulk Update ({selectedProducts.length})
                            </button>
                            <button
                                onClick={handleExport}
                                className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 flex items-center"
                            >
                                <Download size={16} className="mr-2" />
                                Export
                            </button>
                            <button
                                onClick={() => setShowDeleteModal(true)}
                                className="bg-red-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-red-700 flex items-center"
                            >
                                <Trash2 size={16} className="mr-2" />
                                Delete
                            </button>
                        </>
                    )}
                </div>
            </div>

            {/* Stats Cards */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatCard
                        title="Total Products"
                        value={stats.totalProducts}
                        icon={Package}
                        color="blue"
                    />
                    <StatCard
                        title="Active Products"
                        value={stats.activeProducts}
                        icon={Eye}
                        color="green"
                    />
                    <StatCard
                        title="Inventory Value"
                        value={`$${stats.totalInventoryValue?.toLocaleString()}`}
                        icon={DollarSign}
                        color="purple"
                    />
                    <StatCard
                        title="Low Stock Items"
                        value={stats.lowStock}
                        icon={AlertCircle}
                        color="yellow"
                    />
                </div>
            )}

            {/* Search and Filters */}
            <div className="bg-white rounded-lg shadow p-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 min-w-[200px]">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                            <input
                                type="text"
                                placeholder="Search products..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>

                    <select
                        value={filters.category}
                        onChange={(e) => setFilters({ ...filters, category: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Categories</option>
                        {categories.map(cat => (
                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                        ))}
                    </select>

                    <select
                        value={filters.inStock}
                        onChange={(e) => setFilters({ ...filters, inStock: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Stock</option>
                        <option value="true">In Stock</option>
                        <option value="false">Out of Stock</option>
                    </select>

                    <select
                        value={filters.active}
                        onChange={(e) => setFilters({ ...filters, active: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Status</option>
                        <option value="true">Active</option>
                        <option value="false">Inactive</option>
                    </select>

                    <button
                        onClick={() => setFilters({ category: '', inStock: '', active: '', minPrice: '', maxPrice: '' })}
                        className="px-4 py-2 text-gray-600 hover:text-gray-900"
                    >
                        Clear Filters
                    </button>
                </div>
            </div>

            {/* Products Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left">
                                <input
                                    type="checkbox"
                                    checked={selectedProducts.length === products.length && products.length > 0}
                                    onChange={selectAllProducts}
                                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                />
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Product
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                SKU
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Category
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Price
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Stock
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Actions
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {loading ? (
                            <tr>
                                <td colSpan="8" className="px-6 py-12 text-center">
                                    <div className="flex justify-center">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                    </div>
                                </td>
                            </tr>
                        ) : products.length === 0 ? (
                            <tr>
                                <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                                    No products found
                                </td>
                            </tr>
                        ) : (
                            products.map((product) => (
                                <tr key={product.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4">
                                        <input
                                            type="checkbox"
                                            checked={selectedProducts.includes(product.id)}
                                            onChange={() => toggleProductSelection(product.id)}
                                            className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                        />
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center">
                                            <img
                                                src={product.imageUrl || 'https://via.placeholder.com/40'}
                                                alt={product.name}
                                                className="w-10 h-10 rounded-lg object-cover mr-3"
                                            />
                                            <div>
                                                <div className="text-sm font-medium text-gray-900">{product.name}</div>
                                                <div className="text-sm text-gray-500 truncate max-w-xs">
                                                    {product.description}
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-500">{product.sku}</td>
                                    <td className="px-6 py-4 text-sm text-gray-500">{product.categoryName}</td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm font-medium text-gray-900">
                                            ${product.price.toFixed(2)}
                                        </div>
                                        {product.discountPrice && (
                                            <div className="text-sm text-gray-500 line-through">
                                                ${product.discountPrice.toFixed(2)}
                                            </div>
                                        )}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className={`text-sm font-medium ${
                                            product.stockQuantity > 10 ? 'text-green-600' :
                                                product.stockQuantity > 0 ? 'text-yellow-600' :
                                                    'text-red-600'
                                        }`}>
                                            {product.stockQuantity} units
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                          product.isActive
                              ? 'bg-green-100 text-green-800'
                              : 'bg-gray-100 text-gray-800'
                      }`}>
                        {product.isActive ? 'Active' : 'Inactive'}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center space-x-2">
                                            <button
                                                onClick={() => handleToggleStatus(product.id)}
                                                className="p-1 text-gray-600 hover:text-gray-900"
                                                title={product.isActive ? 'Deactivate' : 'Activate'}
                                            >
                                                {product.isActive ? <EyeOff size={18} /> : <Eye size={18} />}
                                            </button>
                                            <button
                                                onClick={() => handleDuplicate(product.id)}
                                                className="p-1 text-gray-600 hover:text-gray-900"
                                                title="Duplicate"
                                            >
                                                <Copy size={18} />
                                            </button>
                                            <button
                                                onClick={() => openEditModal(product)}
                                                className="p-1 text-blue-600 hover:text-blue-900"
                                                title="Edit"
                                            >
                                                <Edit size={18} />
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setSelectedProduct(product);
                                                    setShowDeleteModal(true);
                                                }}
                                                className="p-1 text-red-600 hover:text-red-900"
                                                title="Delete"
                                            >
                                                <Trash2 size={18} />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                {/* Pagination */}
                <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
                    <div className="text-sm text-gray-700">
                        Showing {pagination.page * pagination.size + 1} to{' '}
                        {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                        {pagination.totalElements} products
                    </div>
                    <div className="flex items-center space-x-2">
                        <button
                            onClick={() => setPagination({ ...pagination, page: pagination.page - 1 })}
                            disabled={pagination.page === 0}
                            className="p-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            <ChevronLeft size={20} />
                        </button>
                        <span className="px-4 py-2 bg-gray-100 rounded-lg">
              Page {pagination.page + 1} of {pagination.totalPages}
            </span>
                        <button
                            onClick={() => setPagination({ ...pagination, page: pagination.page + 1 })}
                            disabled={pagination.page === pagination.totalPages - 1}
                            className="p-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
                        >
                            <ChevronRight size={20} />
                        </button>
                    </div>
                </div>
            </div>

            {/* Add/Edit Product Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">
                                {selectedProduct ? 'Edit Product' : 'Add New Product'}
                            </h2>
                            <button
                                onClick={() => {
                                    setShowModal(false);
                                    resetForm();
                                }}
                                className="p-1 hover:bg-gray-100 rounded"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="p-6 space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Product Name *
                                    </label>
                                    <input
                                        type="text"
                                        name="name"
                                        value={formData.name}
                                        onChange={handleInputChange}
                                        required
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Description
                                    </label>
                                    <textarea
                                        name="description"
                                        value={formData.description}
                                        onChange={handleInputChange}
                                        rows="4"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Price *
                                    </label>
                                    <input
                                        type="number"
                                        name="price"
                                        value={formData.price}
                                        onChange={handleInputChange}
                                        step="0.01"
                                        min="0"
                                        required
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Discount Price
                                    </label>
                                    <input
                                        type="number"
                                        name="discountPrice"
                                        value={formData.discountPrice}
                                        onChange={handleInputChange}
                                        step="0.01"
                                        min="0"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        SKU *
                                    </label>
                                    <input
                                        type="text"
                                        name="sku"
                                        value={formData.sku}
                                        onChange={handleInputChange}
                                        required
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Stock Quantity *
                                    </label>
                                    <input
                                        type="number"
                                        name="stockQuantity"
                                        value={formData.stockQuantity}
                                        onChange={handleInputChange}
                                        min="0"
                                        required
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Category
                                    </label>
                                    <select
                                        name="categoryId"
                                        value={formData.categoryId}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="">Select Category</option>
                                        {categories.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </select>
                                </div>

                                <div className="flex items-center">
                                    <input
                                        type="checkbox"
                                        name="isActive"
                                        checked={formData.isActive}
                                        onChange={handleInputChange}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <label className="ml-2 block text-sm text-gray-700">
                                        Active
                                    </label>
                                </div>

                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Product Image
                                    </label>
                                    <input
                                        type="file"
                                        name="image"
                                        onChange={handleInputChange}
                                        accept="image/*"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>

                            <div className="flex justify-end space-x-3 pt-4 border-t">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowModal(false);
                                        resetForm();
                                    }}
                                    className="px-4 py-2 border rounded-lg hover:bg-gray-50"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center"
                                >
                                    <Save size={16} className="mr-2" />
                                    {selectedProduct ? 'Update' : 'Create'} Product
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {showDeleteModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-xl font-semibold text-red-600">Confirm Delete</h2>
                        </div>
                        <div className="p-6">
                            <p className="text-gray-600">
                                Are you sure you want to delete {selectedProducts.length > 0
                                ? `${selectedProducts.length} products`
                                : selectedProduct?.name}? This action cannot be undone.
                            </p>
                        </div>
                        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
                            <button
                                onClick={() => {
                                    setShowDeleteModal(false);
                                    setSelectedProduct(null);
                                }}
                                className="px-4 py-2 border rounded-lg hover:bg-gray-50"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleDelete}
                                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Products;