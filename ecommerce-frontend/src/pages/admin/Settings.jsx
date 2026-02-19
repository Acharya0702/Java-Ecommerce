import React, { useState } from 'react';
import {
    Save,
    Mail,
    Bell,
    Shield,
    Globe,
    CreditCard,
    Truck,
    Users,
    Settings as SettingsIcon,
    Lock,
    Eye,
    EyeOff
} from 'lucide-react';
import { toast } from 'react-toastify';

const Settings = () => {
    const [activeTab, setActiveTab] = useState('general');
    const [showPassword, setShowPassword] = useState({});

    // Settings state
    const [settings, setSettings] = useState({
        // General Settings
        storeName: 'E-commerce Store',
        storeEmail: 'admin@ecommerce.com',
        storePhone: '+1 234 567 890',
        storeAddress: '123 Main St, City, State 12345',
        currency: 'USD',
        timezone: 'America/New_York',

        // Email Settings
        smtpHost: 'smtp.gmail.com',
        smtpPort: '587',
        smtpUsername: 'noreply@ecommerce.com',
        smtpPassword: '',
        smtpEncryption: 'tls',

        // Payment Settings
        paymentMethods: {
            stripe: true,
            paypal: true,
            cod: true
        },
        stripePublicKey: 'pk_test_...',
        stripeSecretKey: 'sk_test_...',
        paypalClientId: '...',

        // Shipping Settings
        shippingMethods: {
            standard: true,
            express: true,
            overnight: false
        },
        standardRate: 5.99,
        expressRate: 14.99,
        overnightRate: 29.99,
        freeShippingThreshold: 50,

        // Security Settings
        twoFactorAuth: false,
        sessionTimeout: 30, // minutes
        maxLoginAttempts: 5,
        passwordMinLength: 8,

        // Notification Settings
        emailNotifications: {
            newOrder: true,
            orderShipped: true,
            orderDelivered: true,
            lowStock: true,
            newUser: false
        },
        adminEmails: ['admin@ecommerce.com'],

        // SEO Settings
        metaTitle: 'E-commerce Store - Best Online Shopping',
        metaDescription: 'Shop the best products at great prices',
        metaKeywords: 'ecommerce, shop, online store',
        googleAnalyticsId: 'UA-XXXXX-Y',

        // User Settings
        allowRegistration: true,
        requireEmailVerification: true,
        defaultUserRole: 'CUSTOMER'
    });

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setSettings(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleNestedChange = (section, field, value) => {
        setSettings(prev => ({
            ...prev,
            [section]: {
                ...prev[section],
                [field]: value
            }
        }));
    };

    const handleSave = () => {
        // Save settings to backend
        toast.success('Settings saved successfully');
    };

    const togglePasswordVisibility = (field) => {
        setShowPassword(prev => ({
            ...prev,
            [field]: !prev[field]
        }));
    };

    const tabs = [
        { id: 'general', label: 'General', icon: SettingsIcon },
        { id: 'email', label: 'Email', icon: Mail },
        { id: 'payment', label: 'Payment', icon: CreditCard },
        { id: 'shipping', label: 'Shipping', icon: Truck },
        { id: 'security', label: 'Security', icon: Shield },
        { id: 'notifications', label: 'Notifications', icon: Bell },
        { id: 'seo', label: 'SEO', icon: Globe },
        { id: 'users', label: 'Users', icon: Users }
    ];

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-semibold text-gray-900">Admin Settings</h1>
                <button
                    onClick={handleSave}
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700 flex items-center"
                >
                    <Save size={16} className="mr-2" />
                    Save Changes
                </button>
            </div>

            {/* Settings Tabs */}
            <div className="bg-white rounded-lg shadow">
                <div className="border-b border-gray-200">
                    <nav className="flex overflow-x-auto px-6">
                        {tabs.map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center px-4 py-4 border-b-2 whitespace-nowrap ${
                                    activeTab === tab.id
                                        ? 'border-blue-500 text-blue-600'
                                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                }`}
                            >
                                <tab.icon size={16} className="mr-2" />
                                {tab.label}
                            </button>
                        ))}
                    </nav>
                </div>

                <div className="p-6">
                    {/* General Settings */}
                    {activeTab === 'general' && (
                        <div className="space-y-6 max-w-2xl">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Store Name
                                </label>
                                <input
                                    type="text"
                                    name="storeName"
                                    value={settings.storeName}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Store Email
                                </label>
                                <input
                                    type="email"
                                    name="storeEmail"
                                    value={settings.storeEmail}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Store Phone
                                </label>
                                <input
                                    type="tel"
                                    name="storePhone"
                                    value={settings.storePhone}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Store Address
                                </label>
                                <textarea
                                    name="storeAddress"
                                    value={settings.storeAddress}
                                    onChange={handleInputChange}
                                    rows="3"
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Currency
                                    </label>
                                    <select
                                        name="currency"
                                        value={settings.currency}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="USD">USD ($)</option>
                                        <option value="EUR">EUR (€)</option>
                                        <option value="GBP">GBP (£)</option>
                                        <option value="JPY">JPY (¥)</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Timezone
                                    </label>
                                    <select
                                        name="timezone"
                                        value={settings.timezone}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="America/New_York">Eastern Time</option>
                                        <option value="America/Chicago">Central Time</option>
                                        <option value="America/Denver">Mountain Time</option>
                                        <option value="America/Los_Angeles">Pacific Time</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Email Settings */}
                    {activeTab === 'email' && (
                        <div className="space-y-6 max-w-2xl">
                            <h3 className="text-lg font-medium">SMTP Configuration</h3>

                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        SMTP Host
                                    </label>
                                    <input
                                        type="text"
                                        name="smtpHost"
                                        value={settings.smtpHost}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        SMTP Port
                                    </label>
                                    <input
                                        type="text"
                                        name="smtpPort"
                                        value={settings.smtpPort}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    SMTP Username
                                </label>
                                <input
                                    type="email"
                                    name="smtpUsername"
                                    value={settings.smtpUsername}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    SMTP Password
                                </label>
                                <div className="relative">
                                    <input
                                        type={showPassword.smtp ? 'text' : 'password'}
                                        name="smtpPassword"
                                        value={settings.smtpPassword}
                                        onChange={handleInputChange}
                                        className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => togglePasswordVisibility('smtp')}
                                        className="absolute right-3 top-1/2 transform -translate-y-1/2"
                                    >
                                        {showPassword.smtp ? <EyeOff size={16} /> : <Eye size={16} />}
                                    </button>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Encryption
                                </label>
                                <select
                                    name="smtpEncryption"
                                    value={settings.smtpEncryption}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    <option value="none">None</option>
                                    <option value="ssl">SSL</option>
                                    <option value="tls">TLS</option>
                                </select>
                            </div>
                        </div>
                    )}

                    {/* Payment Settings */}
                    {activeTab === 'payment' && (
                        <div className="space-y-6 max-w-2xl">
                            <h3 className="text-lg font-medium">Payment Methods</h3>

                            <div className="space-y-3">
                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.paymentMethods.stripe}
                                        onChange={(e) => handleNestedChange('paymentMethods', 'stripe', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">Stripe</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.paymentMethods.paypal}
                                        onChange={(e) => handleNestedChange('paymentMethods', 'paypal', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">PayPal</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.paymentMethods.cod}
                                        onChange={(e) => handleNestedChange('paymentMethods', 'cod', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">Cash on Delivery</span>
                                </label>
                            </div>

                            <div className="border-t pt-4">
                                <h4 className="font-medium mb-3">Stripe Configuration</h4>
                                <div className="space-y-3">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            Publishable Key
                                        </label>
                                        <input
                                            type="text"
                                            value={settings.stripePublicKey}
                                            onChange={(e) => setSettings({ ...settings, stripePublicKey: e.target.value })}
                                            className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">
                                            Secret Key
                                        </label>
                                        <div className="relative">
                                            <input
                                                type={showPassword.stripeSecret ? 'text' : 'password'}
                                                value={settings.stripeSecretKey}
                                                onChange={(e) => setSettings({ ...settings, stripeSecretKey: e.target.value })}
                                                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
                                            />
                                            <button
                                                type="button"
                                                onClick={() => togglePasswordVisibility('stripeSecret')}
                                                className="absolute right-3 top-1/2 transform -translate-y-1/2"
                                            >
                                                {showPassword.stripeSecret ? <EyeOff size={16} /> : <Eye size={16} />}
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Shipping Settings */}
                    {activeTab === 'shipping' && (
                        <div className="space-y-6 max-w-2xl">
                            <h3 className="text-lg font-medium">Shipping Methods</h3>

                            <div className="space-y-4">
                                <div className="flex items-center justify-between p-3 border rounded-lg">
                                    <div className="flex items-center">
                                        <input
                                            type="checkbox"
                                            checked={settings.shippingMethods.standard}
                                            onChange={(e) => handleNestedChange('shippingMethods', 'standard', e.target.checked)}
                                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                        />
                                        <span className="ml-2 text-sm font-medium">Standard Shipping</span>
                                    </div>
                                    <div className="w-32">
                                        <input
                                            type="number"
                                            value={settings.standardRate}
                                            onChange={(e) => setSettings({ ...settings, standardRate: parseFloat(e.target.value) })}
                                            className="w-full px-2 py-1 border rounded text-right"
                                            step="0.01"
                                            min="0"
                                        />
                                    </div>
                                </div>

                                <div className="flex items-center justify-between p-3 border rounded-lg">
                                    <div className="flex items-center">
                                        <input
                                            type="checkbox"
                                            checked={settings.shippingMethods.express}
                                            onChange={(e) => handleNestedChange('shippingMethods', 'express', e.target.checked)}
                                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                        />
                                        <span className="ml-2 text-sm font-medium">Express Shipping</span>
                                    </div>
                                    <div className="w-32">
                                        <input
                                            type="number"
                                            value={settings.expressRate}
                                            onChange={(e) => setSettings({ ...settings, expressRate: parseFloat(e.target.value) })}
                                            className="w-full px-2 py-1 border rounded text-right"
                                            step="0.01"
                                            min="0"
                                        />
                                    </div>
                                </div>

                                <div className="flex items-center justify-between p-3 border rounded-lg">
                                    <div className="flex items-center">
                                        <input
                                            type="checkbox"
                                            checked={settings.shippingMethods.overnight}
                                            onChange={(e) => handleNestedChange('shippingMethods', 'overnight', e.target.checked)}
                                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                        />
                                        <span className="ml-2 text-sm font-medium">Overnight Shipping</span>
                                    </div>
                                    <div className="w-32">
                                        <input
                                            type="number"
                                            value={settings.overnightRate}
                                            onChange={(e) => setSettings({ ...settings, overnightRate: parseFloat(e.target.value) })}
                                            className="w-full px-2 py-1 border rounded text-right"
                                            step="0.01"
                                            min="0"
                                        />
                                    </div>
                                </div>
                            </div>

                            <div className="border-t pt-4">
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Free Shipping Threshold ($)
                                </label>
                                <input
                                    type="number"
                                    value={settings.freeShippingThreshold}
                                    onChange={(e) => setSettings({ ...settings, freeShippingThreshold: parseFloat(e.target.value) })}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    step="0.01"
                                    min="0"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Orders above this amount get free shipping. Set to 0 to disable.
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Security Settings */}
                    {activeTab === 'security' && (
                        <div className="space-y-6 max-w-2xl">
                            <div className="space-y-3">
                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        name="twoFactorAuth"
                                        checked={settings.twoFactorAuth}
                                        onChange={handleInputChange}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">
                    Enable Two-Factor Authentication (2FA)
                  </span>
                                </label>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Session Timeout (minutes)
                                </label>
                                <input
                                    type="number"
                                    name="sessionTimeout"
                                    value={settings.sessionTimeout}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    min="5"
                                    max="480"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Max Login Attempts
                                </label>
                                <input
                                    type="number"
                                    name="maxLoginAttempts"
                                    value={settings.maxLoginAttempts}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    min="3"
                                    max="10"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Minimum Password Length
                                </label>
                                <input
                                    type="number"
                                    name="passwordMinLength"
                                    value={settings.passwordMinLength}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    min="6"
                                    max="20"
                                />
                            </div>
                        </div>
                    )}

                    {/* Notifications Settings */}
                    {activeTab === 'notifications' && (
                        <div className="space-y-6 max-w-2xl">
                            <h3 className="text-lg font-medium">Email Notifications</h3>

                            <div className="space-y-3">
                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.emailNotifications.newOrder}
                                        onChange={(e) => handleNestedChange('emailNotifications', 'newOrder', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">New Order</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.emailNotifications.orderShipped}
                                        onChange={(e) => handleNestedChange('emailNotifications', 'orderShipped', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">Order Shipped</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.emailNotifications.orderDelivered}
                                        onChange={(e) => handleNestedChange('emailNotifications', 'orderDelivered', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">Order Delivered</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.emailNotifications.lowStock}
                                        onChange={(e) => handleNestedChange('emailNotifications', 'lowStock', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">Low Stock Alert</span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        checked={settings.emailNotifications.newUser}
                                        onChange={(e) => handleNestedChange('emailNotifications', 'newUser', e.target.checked)}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">New User Registration</span>
                                </label>
                            </div>

                            <div className="border-t pt-4">
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Admin Email Addresses
                                </label>
                                <textarea
                                    value={settings.adminEmails.join(', ')}
                                    onChange={(e) => setSettings({
                                        ...settings,
                                        adminEmails: e.target.value.split(',').map(email => email.trim())
                                    })}
                                    rows="3"
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    placeholder="admin1@example.com, admin2@example.com"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Separate multiple emails with commas
                                </p>
                            </div>
                        </div>
                    )}

                    {/* SEO Settings */}
                    {activeTab === 'seo' && (
                        <div className="space-y-6 max-w-2xl">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Meta Title
                                </label>
                                <input
                                    type="text"
                                    name="metaTitle"
                                    value={settings.metaTitle}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Recommended length: 50-60 characters
                                </p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Meta Description
                                </label>
                                <textarea
                                    name="metaDescription"
                                    value={settings.metaDescription}
                                    onChange={handleInputChange}
                                    rows="3"
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Recommended length: 150-160 characters
                                </p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Meta Keywords
                                </label>
                                <input
                                    type="text"
                                    name="metaKeywords"
                                    value={settings.metaKeywords}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                                <p className="text-xs text-gray-500 mt-1">
                                    Separate keywords with commas
                                </p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Google Analytics ID
                                </label>
                                <input
                                    type="text"
                                    name="googleAnalyticsId"
                                    value={settings.googleAnalyticsId}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                        </div>
                    )}

                    {/* User Settings */}
                    {activeTab === 'users' && (
                        <div className="space-y-6 max-w-2xl">
                            <div className="space-y-3">
                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        name="allowRegistration"
                                        checked={settings.allowRegistration}
                                        onChange={handleInputChange}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">
                    Allow new user registration
                  </span>
                                </label>

                                <label className="flex items-center">
                                    <input
                                        type="checkbox"
                                        name="requireEmailVerification"
                                        checked={settings.requireEmailVerification}
                                        onChange={handleInputChange}
                                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    />
                                    <span className="ml-2 text-sm text-gray-700">
                    Require email verification
                  </span>
                                </label>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Default User Role
                                </label>
                                <select
                                    name="defaultUserRole"
                                    value={settings.defaultUserRole}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    <option value="CUSTOMER">Customer</option>
                                    <option value="MODERATOR">Moderator</option>
                                </select>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Settings;