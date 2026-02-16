// frontend/src/pages/ProductsPage.jsx
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
    Container,
    Grid,
    Card,
    CardMedia,
    CardContent,
    CardActions,
    Typography,
    Button,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Slider,
    Box,
    Chip,
    Pagination,
    Skeleton,
    Alert,
    Rating,
    IconButton,
    InputAdornment
} from '@mui/material';
import {
    ShoppingCart,
    Search,
    FilterList,
    FavoriteBorder,
    Favorite
} from '@mui/icons-material';
import { fetchProducts } from '../store/slices/productSlice';
import { addToCart } from '../store/slices/cartSlice';

const ProductsPage = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { products, loading, error, totalPages } = useSelector((state) => state.products);
    const auth = useSelector((state) => state.auth);
    const isAuthenticated = !!auth.accessToken;  // Convert to boolean
    const user = auth.user;
    const accessToken = auth.accessToken;

    // Local wishlist state
    const [wishlist, setWishlist] = useState([]);
    const [filters, setFilters] = useState({
        category: '',
        minPrice: 0,
        maxPrice: 1000,
        sortBy: 'id',
        sortDir: 'asc',
        search: ''
    });
    const [page, setPage] = useState(0); // Backend uses 0-based pagination
    const [showFilters, setShowFilters] = useState(false);
    const [priceRange, setPriceRange] = useState([0, 1000]);

    console.log('Auth State:', {
        isAuthenticated,
        user,
        accessToken: accessToken ? 'Present' : 'Missing',
        localStorageToken: localStorage.getItem('accessToken') ? 'Present' : 'Missing'
    });

    // Load wishlist from localStorage on mount
    useEffect(() => {
        const savedWishlist = localStorage.getItem('wishlist');
        if (savedWishlist) {
            setWishlist(JSON.parse(savedWishlist));
        }
    }, []);

    // Fetch products when filters or page change
    useEffect(() => {
        const params = {
            page,
            size: 12,
            sortBy: filters.sortBy,
            sortDir: filters.sortDir,
            ...(filters.category && { categoryId: filters.category }),
            ...(filters.search && { search: filters.search }),
            minPrice: priceRange[0],
            maxPrice: priceRange[1]
        };

        dispatch(fetchProducts(params));
    }, [dispatch, page, filters, priceRange]);

    const handleFilterChange = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }));
        setPage(0); // Reset to first page when filters change
    };

    const handlePriceChange = (event, newValue) => {
        setPriceRange(newValue);
    };

    const handlePriceChangeCommitted = () => {
        setPage(0);
    };

    const handleProductClick = (productId) => {
        navigate(`/products/${productId}`);
    };

    const handleAddToCart = (e, productId) => {
        e.stopPropagation();

        if (!isAuthenticated) {
            alert('Please login to add items to cart');
            return;
        }

        dispatch(addToCart({ productId, quantity: 1 }));
    };

    const handleToggleWishlist = (e, product) => {
        e.stopPropagation();

        if (!isAuthenticated) {
            alert('Please login to add items to wishlist');
            return;
        }

        let updatedWishlist;
        if (wishlist.some(item => item.id === product.id)) {
            updatedWishlist = wishlist.filter(item => item.id !== product.id);
        } else {
            updatedWishlist = [...wishlist, product];
        }

        setWishlist(updatedWishlist);
        localStorage.setItem('wishlist', JSON.stringify(updatedWishlist));
    };

    const isInWishlist = (productId) => {
        return wishlist.some(item => item.id === productId);
    };

    const handlePageChange = (event, value) => {
        setPage(value - 1); // Convert to 0-based for backend
    };

    if (loading && products.length === 0) {
        return (
            <Container maxWidth="lg" sx={{ py: 4 }}>
                <Grid container spacing={3}>
                    {[1, 2, 3, 4, 5, 6].map((item) => (
                        <Grid item xs={12} sm={6} md={4} key={item}>
                            <Skeleton variant="rectangular" height={200} />
                            <Skeleton variant="text" sx={{ mt: 1 }} />
                            <Skeleton variant="text" width="60%" />
                        </Grid>
                    ))}
                </Grid>
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxWidth="lg" sx={{ py: 4 }}>
                <Alert severity="error">{error}</Alert>
            </Container>
        );
    }

    return (
        <Container maxWidth="lg" sx={{ py: 4 }}>
            {/* Header */}
            <Box sx={{ mb: 4 }}>
                <Typography variant="h4" component="h1" gutterBottom>
                    Our Products
                </Typography>
                <Typography variant="body1" color="text.secondary">
                    Discover our amazing collection of products
                </Typography>
            </Box>

            {/* Search and Filter Bar */}
            <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <TextField
                    placeholder="Search products..."
                    value={filters.search}
                    onChange={(e) => handleFilterChange('search', e.target.value)}
                    sx={{ flexGrow: 1 }}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <Search />
                            </InputAdornment>
                        )
                    }}
                />
                <Button
                    variant="outlined"
                    startIcon={<FilterList />}
                    onClick={() => setShowFilters(!showFilters)}
                >
                    Filters
                </Button>
            </Box>

            {/* Filters */}
            {showFilters && (
                <Box sx={{ mb: 3, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} sm={4}>
                            <FormControl fullWidth>
                                <InputLabel>Category</InputLabel>
                                <Select
                                    value={filters.category}
                                    label="Category"
                                    onChange={(e) => handleFilterChange('category', e.target.value)}
                                >
                                    <MenuItem value="">All Categories</MenuItem>
                                    <MenuItem value="1">Electronics</MenuItem>
                                    <MenuItem value="2">Clothing</MenuItem>
                                    <MenuItem value="3">Books</MenuItem>
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <FormControl fullWidth>
                                <InputLabel>Sort By</InputLabel>
                                <Select
                                    value={`${filters.sortBy},${filters.sortDir}`}
                                    label="Sort By"
                                    onChange={(e) => {
                                        const [sortBy, sortDir] = e.target.value.split(',');
                                        handleFilterChange('sortBy', sortBy);
                                        handleFilterChange('sortDir', sortDir);
                                    }}
                                >
                                    <MenuItem value="id,asc">Newest First</MenuItem>
                                    <MenuItem value="price,asc">Price: Low to High</MenuItem>
                                    <MenuItem value="price,desc">Price: High to Low</MenuItem>
                                    <MenuItem value="name,asc">Name: A to Z</MenuItem>
                                    <MenuItem value="name,desc">Name: Z to A</MenuItem>
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={4}>
                            <Typography gutterBottom>Price Range</Typography>
                            <Slider
                                value={priceRange}
                                onChange={handlePriceChange}
                                onChangeCommitted={handlePriceChangeCommitted}
                                valueLabelDisplay="auto"
                                min={0}
                                max={1000}
                            />
                            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                <Typography variant="body2">${priceRange[0]}</Typography>
                                <Typography variant="body2">${priceRange[1]}</Typography>
                            </Box>
                        </Grid>
                    </Grid>
                </Box>
            )}

            {/* Products Grid */}
            <Grid container spacing={3}>
                {products.map((product) => (
                    <Grid item xs={12} sm={6} md={4} key={product.id}>
                        <Card
                            sx={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                position: 'relative',
                                cursor: 'pointer',
                                transition: 'transform 0.2s, box-shadow 0.2s',
                                '&:hover': {
                                    transform: 'translateY(-4px)',
                                    boxShadow: 4
                                }
                            }}
                            onClick={() => handleProductClick(product.id)}
                        >
                            {/* Wishlist Button */}
                            <IconButton
                                sx={{ position: 'absolute', top: 8, right: 8, bgcolor: 'background.paper' }}
                                onClick={(e) => handleToggleWishlist(e, product)}
                            >
                                {isInWishlist(product.id) ? <Favorite color="error" /> : <FavoriteBorder />}
                            </IconButton>

                            {/* Product Image */}
                            <CardMedia
                                component="img"
                                height="200"
                                image={product.imageUrl || 'https://via.placeholder.com/300x200'}
                                alt={product.name}
                                sx={{ objectFit: 'contain', p: 2 }}
                            />

                            <CardContent sx={{ flexGrow: 1 }}>
                                {/* Product Name */}
                                <Typography
                                    gutterBottom
                                    variant="h6"
                                    component="h2"
                                    sx={{
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        display: '-webkit-box',
                                        WebkitLineClamp: 2,
                                        WebkitBoxOrient: 'vertical'
                                    }}
                                >
                                    {product.name}
                                </Typography>

                                {/* Rating */}
                                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                    <Rating
                                        value={product.averageRating || 0}
                                        precision={0.5}
                                        size="small"
                                        readOnly
                                    />
                                    <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                                        ({product.totalReviews || 0})
                                    </Typography>
                                </Box>

                                {/* Price */}
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <Typography variant="h6" color="primary">
                                        ${product.discountedPrice || product.price}
                                    </Typography>
                                    {product.discountedPrice && (
                                        <Typography variant="body2" color="text.secondary" sx={{ textDecoration: 'line-through' }}>
                                            ${product.price}
                                        </Typography>
                                    )}
                                </Box>

                                {/* Stock Status */}
                                <Chip
                                    label={product.inStock ? 'In Stock' : 'Out of Stock'}
                                    color={product.inStock ? 'success' : 'error'}
                                    size="small"
                                    sx={{ mt: 1 }}
                                />
                            </CardContent>

                            <CardActions>
                                <Button
                                    fullWidth
                                    variant="contained"
                                    startIcon={<ShoppingCart />}
                                    disabled={!product.inStock}
                                    onClick={(e) => handleAddToCart(e, product.id)}
                                >
                                    Add to Cart
                                </Button>
                            </CardActions>
                        </Card>
                    </Grid>
                ))}
            </Grid>

            {/* No Products Found */}
            {products.length === 0 && !loading && (
                <Box sx={{ textAlign: 'center', py: 8 }}>
                    <Typography variant="h6" color="text.secondary">
                        No products found
                    </Typography>
                </Box>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                    <Pagination
                        count={totalPages}
                        page={page + 1} // Convert to 1-based for display
                        onChange={handlePageChange}
                        color="primary"
                    />
                </Box>
            )}
        </Container>
    );
};

export default ProductsPage;