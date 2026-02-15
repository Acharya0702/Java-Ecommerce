import React from 'react';
import { FaStar, FaStarHalfAlt, FaRegStar } from 'react-icons/fa';

const StarRating = ({ rating, size = 20, color = '#ffc107', editable = false, onChange, maxRating = 5 }) => {
    const [hoverRating, setHoverRating] = React.useState(0);

    const renderStars = () => {
        const stars = [];
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.5;

        for (let i = 1; i <= maxRating; i++) {
            if (i <= fullStars) {
                stars.push(
                    <FaStar
                        key={i}
                        size={size}
                        color={color}
                        style={{ cursor: editable ? 'pointer' : 'default' }}
                        onMouseEnter={() => editable && setHoverRating(i)}
                        onMouseLeave={() => editable && setHoverRating(0)}
                        onClick={() => editable && onChange && onChange(i)}
                    />
                );
            } else if (i === fullStars + 1 && hasHalfStar) {
                stars.push(
                    <FaStarHalfAlt
                        key={i}
                        size={size}
                        color={color}
                        style={{ cursor: editable ? 'pointer' : 'default' }}
                        onMouseEnter={() => editable && setHoverRating(i)}
                        onMouseLeave={() => editable && setHoverRating(0)}
                        onClick={() => editable && onChange && onChange(i)}
                    />
                );
            } else {
                stars.push(
                    <FaRegStar
                        key={i}
                        size={size}
                        color={i <= hoverRating ? color : '#ddd'}
                        style={{ cursor: editable ? 'pointer' : 'default' }}
                        onMouseEnter={() => editable && setHoverRating(i)}
                        onMouseLeave={() => editable && setHoverRating(0)}
                        onClick={() => editable && onChange && onChange(i)}
                    />
                );
            }
        }
        return stars;
    };

    return (
        <div className="star-rating" style={{ display: 'inline-flex', alignItems: 'center' }}>
            {renderStars()}
            {rating > 0 && (
                <span style={{ marginLeft: '8px', fontSize: '0.9em', color: '#666' }}>
                    {rating.toFixed(1)}
                </span>
            )}
        </div>
    );
};

export default StarRating;