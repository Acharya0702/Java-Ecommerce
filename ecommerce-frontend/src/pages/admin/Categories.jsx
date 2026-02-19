import React, { useState, useEffect } from 'react';
import {
    Plus,
    Search,
    Edit,
    Trash2,
    Eye,
    EyeOff,
    ChevronLeft,
    ChevronRight,
    X,
    Save,
    FolderTree,
    MoveUp,
    MoveDown
} from 'lucide-react';
import { adminApi } from '../../api/adminApi';

const Categories = () => {
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        slug: '',
        imageUrl: '',
        parentId: '',
        isActive: true,
        displayOrder: 0
    });

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAllCategories();
            setCategories(response.data);
        } catch (error) {
            console.error('Error fetching categories:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({
            ...formData,
            [name]: type === 'checkbox' ? checked : value,
            // Auto-generate slug from name if slug is empty
            ...(name === 'name' && !formData.slug && {
                slug: value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
            })
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (selectedCategory) {
                await adminApi.updateCategory(selectedCategory.id, formData);
            } else {
                await adminApi.createCategory(formData);
            }
            fetchCategories();
            setShowModal(false);
            resetForm();
        } catch (error) {
            console.error('Error saving category:', error);
        }
    };

    const handleDelete = async () => {
        try {
            if (selectedCategory) {
                await adminApi.deleteCategory(selectedCategory.id);
                fetchCategories();
                setShowDeleteModal(false);
                setSelectedCategory(null);
            }
        } catch (error) {
            console.error('Error deleting category:', error);
            alert(error.response?.data?.message || 'Cannot delete category with products or subcategories');
        }
    };

    const handleToggleStatus = async (id, currentStatus) => {
        try {
            // Toggle status by updating category
            const category = categories.find(c => c.id === id);
            await adminApi.updateCategory(id, { ...category, isActive: !currentStatus });
            fetchCategories();
        } catch (error) {
            console.error('Error toggling category status:', error);
        }
    };

    const resetForm = () => {
        setFormData({
            name: '',
            description: '',
            slug: '',
            imageUrl: '',
            parentId: '',
            isActive: true,
            displayOrder: 0
        });
        setSelectedCategory(null);
    };

    const openEditModal = (category) => {
        setSelectedCategory(category);
        setFormData({
            name: category.name || '',
            description: category.description || '',
            slug: category.slug || '',
            imageUrl: category.imageUrl || '',
            parentId: category.parentId || '',
            isActive: category.isActive !== undefined ? category.isActive : true,
            displayOrder: category.displayOrder || 0
        });
        setShowModal(true);
    };

    const filteredCategories = categories.filter(cat =>
        cat.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cat.description?.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const parentCategories = categories.filter(c => !c.parentId);

    const CategoryRow = ({ category, depth = 0 }) => {
        const children = categories.filter(c => c.parentId === category.id);

        return (
            <>
                <tr className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                        <div className="flex items-center" style={{ paddingLeft: `${depth * 20}px` }}>
                            <FolderTree size={16} className="text-gray-400 mr-2" />
                            <span className="text-sm font-medium text-gray-900">{category.name}</span>
                        </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">{category.slug}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{category.productCount || 0}</td>
                    <td className="px-6 py-4">
            <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                category.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
            }`}>
              {category.isActive ? 'Active' : 'Inactive'}
            </span>
                    </td>
                    <td className="px-6 py-4">
                        <div className="flex items-center space-x-2">
                            <button
                                onClick={() => handleToggleStatus(category.id, category.isActive)}
                                className="p-1 text-gray-600 hover:text-gray-900"
                                title={category.isActive ? 'Deactivate' : 'Activate'}
                            >
                                {category.isActive ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                            <button
                                onClick={() => openEditModal(category)}
                                className="p-1 text-blue-600 hover:text-blue-900"
                                title="Edit"
                            >
                                <Edit size={18} />
                            </button>
                            <button
                                onClick={() => {
                                    setSelectedCategory(category);
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
                {children.map(child => (
                    <CategoryRow key={child.id} category={child} depth={depth + 1} />
                ))}
            </>
        );
    };

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Category Management</h1>
                <button
                    onClick={() => {
                        resetForm();
                        setShowModal(true);
                    }}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700 flex items-center"
                >
                    <Plus size={16} className="mr-2" />
                    Add Category
                </button>
            </div>

            {/* Search */}
            <div className="bg-white rounded-lg shadow p-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 min-w-[200px]">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                            <input
                                type="text"
                                placeholder="Search categories..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>
                </div>
            </div>

            {/* Categories Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Category Name
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Slug
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Products
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
                                <td colSpan="5" className="px-6 py-12 text-center">
                                    <div className="flex justify-center">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                    </div>
                                </td>
                            </tr>
                        ) : filteredCategories.length === 0 ? (
                            <tr>
                                <td colSpan="5" className="px-6 py-12 text-center text-gray-500">
                                    No categories found
                                </td>
                            </tr>
                        ) : (
                            parentCategories.map(category => (
                                <CategoryRow key={category.id} category={category} />
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Add/Edit Category Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full">
                        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">
                                {selectedCategory ? 'Edit Category' : 'Add New Category'}
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
                                        Category Name *
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
                                        rows="3"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Slug
                                    </label>
                                    <input
                                        type="text"
                                        name="slug"
                                        value={formData.slug}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                    <p className="text-xs text-gray-500 mt-1">
                                        URL-friendly name (auto-generated from name if left empty)
                                    </p>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Parent Category
                                    </label>
                                    <select
                                        name="parentId"
                                        value={formData.parentId}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="">None (Top Level)</option>
                                        {categories
                                            .filter(c => !selectedCategory || c.id !== selectedCategory.id)
                                            .map(cat => (
                                                <option key={cat.id} value={cat.id}>{cat.name}</option>
                                            ))
                                        }
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Display Order
                                    </label>
                                    <input
                                        type="number"
                                        name="displayOrder"
                                        value={formData.displayOrder}
                                        onChange={handleInputChange}
                                        min="0"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div className="col-span-2">
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Image URL
                                    </label>
                                    <input
                                        type="url"
                                        name="imageUrl"
                                        value={formData.imageUrl}
                                        onChange={handleInputChange}
                                        placeholder="https://example.com/image.jpg"
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
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
                                    {selectedCategory ? 'Update' : 'Create'} Category
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
                                Are you sure you want to delete category "{selectedCategory?.name}"?
                                This action cannot be undone.
                            </p>
                            {(selectedCategory?.productCount > 0 || selectedCategory?.subCategoryCount > 0) && (
                                <div className="mt-3 p-3 bg-yellow-50 rounded-lg">
                                    <p className="text-sm text-yellow-800">
                                        ⚠️ This category has {selectedCategory?.productCount} products and {selectedCategory?.subCategoryCount} subcategories.
                                        You must reassign or delete them first.
                                    </p>
                                </div>
                            )}
                        </div>
                        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
                            <button
                                onClick={() => {
                                    setShowDeleteModal(false);
                                    setSelectedCategory(null);
                                }}
                                className="px-4 py-2 border rounded-lg hover:bg-gray-50"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleDelete}
                                disabled={selectedCategory?.productCount > 0 || selectedCategory?.subCategoryCount > 0}
                                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
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

export default Categories;