import React, { useState, useEffect } from 'react';
import {
    Search,
    Filter,
    Eye,
    Edit,
    Trash2,
    UserCheck,
    UserX,
    Mail,
    Shield,
    Calendar,
    DollarSign,
    Package,
    ChevronLeft,
    ChevronRight,
    RefreshCw,
    AlertCircle,
    X
} from 'lucide-react';
import { adminApi } from '../../api/adminApi';
import { format } from 'date-fns';

const Users = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedUser, setSelectedUser] = useState(null);
    const [showUserModal, setShowUserModal] = useState(false);
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [newRole, setNewRole] = useState('');
    const [searchTerm, setSearchTerm] = useState('');
    const [filters, setFilters] = useState({
        role: '',
        status: ''
    });
    const [pagination, setPagination] = useState({
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0
    });
    const [stats, setStats] = useState(null);

    const roleColors = {
        ADMIN: 'bg-purple-100 text-purple-800',
        CUSTOMER: 'bg-blue-100 text-blue-800',
        MODERATOR: 'bg-green-100 text-green-800',
        SELLER: 'bg-yellow-100 text-yellow-800'
    };

    const roleOptions = ['CUSTOMER', 'ADMIN', 'MODERATOR', 'SELLER'];

    useEffect(() => {
        fetchUsers();
        fetchStats();
    }, [pagination.page, searchTerm, filters]);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const response = await adminApi.getAllUsers(
                pagination.page,
                pagination.size,
                filters.role,
                searchTerm
            );
            setUsers(response.data.content);
            setPagination({
                ...pagination,
                totalPages: response.data.totalPages,
                totalElements: response.data.totalElements
            });
        } catch (error) {
            console.error('Error fetching users:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchStats = async () => {
        try {
            const response = await adminApi.getUserStats();
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching user stats:', error);
        }
    };

    const handleViewUser = async (id) => {
        try {
            const response = await adminApi.getUserDetails(id);
            setSelectedUser(response.data);
            setShowUserModal(true);
        } catch (error) {
            console.error('Error fetching user details:', error);
        }
    };

    const handleUpdateRole = async (id, role) => {
        try {
            await adminApi.updateUserRole(id, role);
            fetchUsers();
            setShowRoleModal(false);
            setSelectedUser(null);
        } catch (error) {
            console.error('Error updating user role:', error);
        }
    };

    const handleToggleStatus = async (id, currentStatus) => {
        try {
            await adminApi.toggleUserStatus(id);
            fetchUsers();
        } catch (error) {
            console.error('Error toggling user status:', error);
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
                <h1 className="text-2xl font-semibold text-gray-900">User Management</h1>
                <button
                    onClick={fetchUsers}
                    className="p-2 border rounded-lg hover:bg-gray-50"
                >
                    <RefreshCw size={20} />
                </button>
            </div>

            {/* Stats Cards */}
            {stats && (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    <StatCard
                        title="Total Users"
                        value={stats.totalUsers}
                        icon={UserCheck}
                        color="blue"
                    />
                    <StatCard
                        title="Active Users"
                        value={stats.activeUsers}
                        icon={UserCheck}
                        color="green"
                    />
                    <StatCard
                        title="Customers"
                        value={stats.totalCustomers}
                        icon={UserCheck}
                        color="purple"
                    />
                    <StatCard
                        title="Admins"
                        value={stats.totalAdmins}
                        icon={Shield}
                        color="red"
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
                                placeholder="Search by name, email, phone..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>

                    <select
                        value={filters.role}
                        onChange={(e) => setFilters({ ...filters, role: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Roles</option>
                        {roleOptions.map(role => (
                            <option key={role} value={role}>{role}</option>
                        ))}
                    </select>

                    <select
                        value={filters.status}
                        onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">All Status</option>
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                    </select>

                    <button
                        onClick={() => setFilters({ role: '', status: '' })}
                        className="px-4 py-2 text-gray-600 hover:text-gray-900"
                    >
                        Clear Filters
                    </button>
                </div>
            </div>

            {/* Users Table */}
            <div className="bg-white rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                User
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Contact
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Role
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Joined
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Orders
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                Actions
                            </th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                        {loading ? (
                            <tr>
                                <td colSpan="7" className="px-6 py-12 text-center">
                                    <div className="flex justify-center">
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                    </div>
                                </td>
                            </tr>
                        ) : users.length === 0 ? (
                            <tr>
                                <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                                    No users found
                                </td>
                            </tr>
                        ) : (
                            users.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4">
                                        <div className="flex items-center">
                                            <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                                                {user.profileImageUrl ? (
                                                    <img
                                                        src={user.profileImageUrl}
                                                        alt={user.fullName}
                                                        className="w-10 h-10 rounded-full object-cover"
                                                    />
                                                ) : (
                                                    <span className="text-lg font-semibold text-gray-600">
                              {user.fullName?.charAt(0)}
                            </span>
                                                )}
                                            </div>
                                            <div className="ml-3">
                                                <div className="text-sm font-medium text-gray-900">
                                                    {user.fullName}
                                                </div>
                                                <div className="text-sm text-gray-500">
                                                    @{user.email?.split('@')[0]}
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm text-gray-900">{user.email}</div>
                                        <div className="text-sm text-gray-500">{user.phone || 'No phone'}</div>
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${roleColors[user.role]}`}>
                        {user.role}
                      </span>
                                    </td>
                                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                          user.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                      }`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                                        {!user.isEmailVerified && (
                                            <span className="ml-2 px-2 py-1 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800">
                          Unverified
                        </span>
                                        )}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {user.createdAt ? format(new Date(user.createdAt), 'MMM dd, yyyy') : 'N/A'}
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm font-medium text-gray-900">
                                            {user.totalOrders || 0} orders
                                        </div>
                                        <div className="text-sm text-gray-500">
                                            ${user.totalSpent?.toFixed(2) || '0.00'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center space-x-2">
                                            <button
                                                onClick={() => handleViewUser(user.id)}
                                                className="p-1 text-blue-600 hover:text-blue-900"
                                                title="View Details"
                                            >
                                                <Eye size={18} />
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setSelectedUser(user);
                                                    setNewRole(user.role);
                                                    setShowRoleModal(true);
                                                }}
                                                className="p-1 text-purple-600 hover:text-purple-900"
                                                title="Change Role"
                                            >
                                                <Shield size={18} />
                                            </button>
                                            <button
                                                onClick={() => handleToggleStatus(user.id, user.isActive)}
                                                className={`p-1 ${user.isActive ? 'text-red-600 hover:text-red-900' : 'text-green-600 hover:text-green-900'}`}
                                                title={user.isActive ? 'Deactivate' : 'Activate'}
                                            >
                                                {user.isActive ? <UserX size={18} /> : <UserCheck size={18} />}
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
                        {pagination.totalElements} users
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

            {/* User Details Modal */}
            {showUserModal && selectedUser && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
                        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                            <h2 className="text-xl font-semibold">User Details</h2>
                            <button
                                onClick={() => setShowUserModal(false)}
                                className="p-1 hover:bg-gray-100 rounded"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <div className="p-6 space-y-6">
                            {/* User Header */}
                            <div className="flex items-center space-x-4">
                                <div className="w-20 h-20 bg-gray-200 rounded-full flex items-center justify-center">
                                    {selectedUser.profileImageUrl ? (
                                        <img
                                            src={selectedUser.profileImageUrl}
                                            alt={selectedUser.fullName}
                                            className="w-20 h-20 rounded-full object-cover"
                                        />
                                    ) : (
                                        <span className="text-3xl font-semibold text-gray-600">
                      {selectedUser.fullName?.charAt(0)}
                    </span>
                                    )}
                                </div>
                                <div>
                                    <h3 className="text-2xl font-semibold">{selectedUser.fullName}</h3>
                                    <div className="flex items-center space-x-2 mt-1">
                    <span className={`px-2 py-1 text-xs font-medium rounded-full ${roleColors[selectedUser.role]}`}>
                      {selectedUser.role}
                    </span>
                                        <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                            selectedUser.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                                        }`}>
                      {selectedUser.isActive ? 'Active' : 'Inactive'}
                    </span>
                                    </div>
                                </div>
                            </div>

                            {/* User Info Grid */}
                            <div className="grid grid-cols-2 gap-6">
                                <div>
                                    <h4 className="font-semibold mb-3">Personal Information</h4>
                                    <div className="space-y-2">
                                        <div>
                                            <p className="text-sm text-gray-500">Email</p>
                                            <p className="text-sm font-medium flex items-center">
                                                <Mail size={14} className="mr-1" />
                                                {selectedUser.email}
                                            </p>
                                        </div>
                                        <div>
                                            <p className="text-sm text-gray-500">Phone</p>
                                            <p className="text-sm font-medium">{selectedUser.phone || 'Not provided'}</p>
                                        </div>
                                        <div>
                                            <p className="text-sm text-gray-500">Email Verified</p>
                                            <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                                selectedUser.isEmailVerified ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                                            }`}>
                        {selectedUser.isEmailVerified ? 'Verified' : 'Unverified'}
                      </span>
                                        </div>
                                    </div>
                                </div>

                                <div>
                                    <h4 className="font-semibold mb-3">Address</h4>
                                    <div className="space-y-2">
                                        <p className="text-sm text-gray-600">
                                            {selectedUser.address || 'No address provided'}<br />
                                            {selectedUser.city && `${selectedUser.city}, `}{selectedUser.state && `${selectedUser.state} `}{selectedUser.zipCode}<br />
                                            {selectedUser.country}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Account Stats */}
                            <div className="border-t pt-4">
                                <h4 className="font-semibold mb-3">Account Statistics</h4>
                                <div className="grid grid-cols-3 gap-4">
                                    <div className="bg-gray-50 p-3 rounded">
                                        <p className="text-sm text-gray-500">Total Orders</p>
                                        <p className="text-xl font-semibold">{selectedUser.totalOrders || 0}</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded">
                                        <p className="text-sm text-gray-500">Total Spent</p>
                                        <p className="text-xl font-semibold">${selectedUser.totalSpent?.toFixed(2) || '0.00'}</p>
                                    </div>
                                    <div className="bg-gray-50 p-3 rounded">
                                        <p className="text-sm text-gray-500">Member Since</p>
                                        <p className="text-sm font-semibold">
                                            {selectedUser.createdAt ? format(new Date(selectedUser.createdAt), 'MMM dd, yyyy') : 'N/A'}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Preferences */}
                            <div className="border-t pt-4">
                                <h4 className="font-semibold mb-3">Preferences</h4>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <p className="text-sm text-gray-500">Preferred Language</p>
                                        <p className="text-sm font-medium">{selectedUser.preferredLanguage || 'en'}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-gray-500">Currency</p>
                                        <p className="text-sm font-medium">{selectedUser.currency || 'USD'}</p>
                                    </div>
                                    <div>
                                        <p className="text-sm text-gray-500">Newsletter</p>
                                        <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                                            selectedUser.newsletterSubscribed ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                                        }`}>
                      {selectedUser.newsletterSubscribed ? 'Subscribed' : 'Not Subscribed'}
                    </span>
                                    </div>
                                </div>
                            </div>

                            {/* Recent Activity */}
                            <div className="border-t pt-4">
                                <h4 className="font-semibold mb-3">Recent Activity</h4>
                                <div className="space-y-2">
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-500">Last Login</span>
                                        <span className="font-medium">
                      {selectedUser.lastLogin ? format(new Date(selectedUser.lastLogin), 'MMM dd, yyyy hh:mm a') : 'Never'}
                    </span>
                                    </div>
                                    <div className="flex justify-between text-sm">
                                        <span className="text-gray-500">Account Created</span>
                                        <span className="font-medium">
                      {selectedUser.createdAt ? format(new Date(selectedUser.createdAt), 'MMM dd, yyyy hh:mm a') : 'N/A'}
                    </span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
                            <button
                                onClick={() => setShowUserModal(false)}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Change Role Modal */}
            {showRoleModal && selectedUser && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-xl font-semibold">Change User Role</h2>
                        </div>
                        <div className="p-6">
                            <p className="mb-4">
                                Change role for <span className="font-semibold">{selectedUser.fullName}</span>
                            </p>
                            <select
                                value={newRole}
                                onChange={(e) => setNewRole(e.target.value)}
                                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            >
                                {roleOptions.map(role => (
                                    <option key={role} value={role}>{role}</option>
                                ))}
                            </select>
                        </div>
                        <div className="px-6 py-4 border-t border-gray-200 flex justify-end space-x-3">
                            <button
                                onClick={() => {
                                    setShowRoleModal(false);
                                    setSelectedUser(null);
                                }}
                                className="px-4 py-2 border rounded-lg hover:bg-gray-50"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={() => handleUpdateRole(selectedUser.id, newRole)}
                                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                            >
                                Update Role
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Users;