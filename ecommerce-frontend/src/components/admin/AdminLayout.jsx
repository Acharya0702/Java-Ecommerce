import React, { useState, useEffect } from 'react';
import { Link, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
    LayoutDashboard,
    ShoppingBag,
    Package,
    Users,
    Settings,
    LogOut,
    Menu,
    X,
    BarChart3,
    Layers,
    Bell,
    User,
    ChevronDown,
    Home
} from 'lucide-react';
import { logout, selectCurrentUser } from '../../store/slices/authSlice';

const AdminLayout = () => {
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [profileMenuOpen, setProfileMenuOpen] = useState(false);
    const user = useSelector(selectCurrentUser);
    const navigate = useNavigate();
    const location = useLocation();
    const dispatch = useDispatch();

    // Close sidebar on mobile when route changes
    useEffect(() => {
        if (window.innerWidth < 1024) {
            setSidebarOpen(false);
        }
    }, [location.pathname]);

    const menuItems = [
        { path: '/admin', icon: LayoutDashboard, label: 'Dashboard', end: true },
        { path: '/admin/orders', icon: ShoppingBag, label: 'Orders' },
        { path: '/admin/products', icon: Package, label: 'Products' },
        { path: '/admin/categories', icon: Layers, label: 'Categories' },
        { path: '/admin/users', icon: Users, label: 'Users' },
        { path: '/admin/reports', icon: BarChart3, label: 'Reports' },
        { path: '/admin/settings', icon: Settings, label: 'Settings' },
    ];

    const handleLogout = async () => {
        try {
            await dispatch(logout()).unwrap();
            navigate('/login');
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    const isActive = (path, end = false) => {
        if (end) {
            return location.pathname === path;
        }
        return location.pathname.startsWith(path) && location.pathname !== '/admin';
    };

    return (
        <div className="min-h-screen bg-gray-100">
            {/* Sidebar */}
            <div className={`fixed inset-y-0 left-0 z-50 w-64 bg-gray-900 transform transition-transform duration-300 ease-in-out ${
                sidebarOpen ? 'translate-x-0' : '-translate-x-full'
            } lg:translate-x-0`}>
                <div className="flex items-center justify-between h-16 px-4 bg-gray-800">
                    <h1 className="text-xl font-bold text-white">Admin Panel</h1>
                    <button
                        onClick={() => setSidebarOpen(false)}
                        className="p-1 text-gray-400 hover:text-white lg:hidden"
                    >
                        <X size={20} />
                    </button>
                </div>

                <nav className="mt-5 px-2">
                    {menuItems.map((item) => {
                        const Icon = item.icon;
                        const active = isActive(item.path, item.end);

                        return (
                            <Link
                                key={item.path}
                                to={item.path}
                                className={`group flex items-center px-2 py-2 text-sm font-medium rounded-md mb-1 ${
                                    active
                                        ? 'bg-gray-800 text-white'
                                        : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                                }`}
                            >
                                <Icon className="mr-3 h-5 w-5" />
                                {item.label}
                            </Link>
                        );
                    })}

                    {/* Return to Store Link */}
                    <Link
                        to="/"
                        className="group flex items-center px-2 py-2 text-sm font-medium rounded-md text-gray-300 hover:bg-gray-700 hover:text-white mt-4 border-t border-gray-700 pt-4"
                    >
                        <Home className="mr-3 h-5 w-5" />
                        Return to Store
                    </Link>
                </nav>
            </div>

            {/* Mobile sidebar overlay */}
            {sidebarOpen && (
                <div
                    className="fixed inset-0 z-40 bg-gray-600 bg-opacity-75 lg:hidden"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            {/* Main content */}
            <div className={`flex-1 transition-margin duration-300 ease-in-out lg:ml-64 ${
                sidebarOpen ? 'ml-64' : 'ml-0'
            }`}>
                {/* Top navbar */}
                <nav className="bg-white shadow-sm h-16 fixed top-0 right-0 left-0 z-30 lg:static lg:ml-64">
                    <div className="flex items-center justify-between h-full px-4">
                        <div className="flex items-center">
                            <button
                                onClick={() => setSidebarOpen(!sidebarOpen)}
                                className="p-2 text-gray-600 hover:text-gray-900 lg:hidden"
                            >
                                <Menu size={24} />
                            </button>
                        </div>

                        <div className="flex items-center space-x-4">
                            {/* Notifications */}
                            <button className="p-2 text-gray-600 hover:text-gray-900 relative">
                                <Bell size={20} />
                                <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
                            </button>

                            {/* Profile dropdown */}
                            <div className="relative">
                                <button
                                    onClick={() => setProfileMenuOpen(!profileMenuOpen)}
                                    className="flex items-center space-x-2 focus:outline-none"
                                >
                                    <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center overflow-hidden">
                                        {user?.profileImageUrl ? (
                                            <img
                                                src={user.profileImageUrl}
                                                alt={user?.firstName}
                                                className="w-full h-full object-cover"
                                            />
                                        ) : (
                                            <User size={16} className="text-gray-600" />
                                        )}
                                    </div>
                                    <div className="hidden md:block text-left">
                                        <p className="text-sm font-medium text-gray-700">
                                            {user?.firstName} {user?.lastName}
                                        </p>
                                        <p className="text-xs text-gray-500">{user?.role}</p>
                                    </div>
                                    <ChevronDown size={16} className="text-gray-500 hidden md:block" />
                                </button>

                                {profileMenuOpen && (
                                    <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-50 border">
                                        <Link
                                            to="/admin/profile"
                                            className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                            onClick={() => setProfileMenuOpen(false)}
                                        >
                                            Your Profile
                                        </Link>
                                        <Link
                                            to="/admin/settings"
                                            className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                            onClick={() => setProfileMenuOpen(false)}
                                        >
                                            Settings
                                        </Link>
                                        <hr className="my-1" />
                                        <button
                                            onClick={() => {
                                                setProfileMenuOpen(false);
                                                handleLogout();
                                            }}
                                            className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
                                        >
                                            <LogOut size={16} className="inline mr-2" />
                                            Logout
                                        </button>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </nav>

                {/* Page content with padding for fixed header on mobile */}
                <main className="p-6 mt-16 lg:mt-0">
                    <Outlet />
                </main>
            </div>
        </div>
    );
};

export default AdminLayout;