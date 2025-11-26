import { useState, useEffect } from 'react';
import { User } from '../types';
import { getInitials, getAvatarColor, getAvatarTextColor, getAvatarUrl } from '../utils/avatarUtils';

interface UserAvatarProps {
  user: User;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  editable?: boolean;
  onClick?: () => void;
}

/**
 * UserAvatar component
 * Displays user avatar image or initials-based fallback
 */
export const UserAvatar = ({ user, size = 'md', editable = false, onClick }: UserAvatarProps) => {
  const [imageError, setImageError] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  // Reset image state when avatar URL or version changes
  useEffect(() => {
    setImageError(false);
    setImageLoaded(false);
  }, [user.avatarUrl, user.avatarVersion]);

  // Size classes
  const sizeClasses = {
    sm: 'h-8 w-8 text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-20 w-20 text-2xl',
    xl: 'h-24 w-24 text-3xl',
  };

  // Check avatarVersion instead of avatarUrl for more reliable detection
  // avatarVersion defaults to 0 and gets set to timestamp when avatar is uploaded
  const hasAvatar = (user.avatarVersion ?? 0) > 0 && !imageError;

  const handleImageError = () => {
    setImageError(true);
  };

  const handleImageLoad = () => {
    setImageLoaded(true);
  };

  const initials = getInitials(user.username);
  const bgColor = getAvatarColor(user.username);
  const textColor = getAvatarTextColor();

  return (
    <div
      className={`
        ${sizeClasses[size]}
        rounded-full
        flex items-center justify-center
        overflow-hidden
        border-2 border-gray-200
        bg-gradient-to-br from-blue-100 to-blue-50
        transition-all duration-200
        ${editable ? 'cursor-pointer hover:border-blue-300 hover:shadow-md' : ''}
        ${onClick ? 'cursor-pointer' : ''}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      aria-label={onClick ? `${user.username}'s profile` : undefined}
    >
      {hasAvatar ? (
        <>
          <img
            src={getAvatarUrl(user.id, user.avatarVersion)}
            alt={`${user.username}'s profile picture`}
            className={`
              h-full w-full object-cover
              transition-opacity duration-300
              ${imageLoaded ? 'opacity-100' : 'opacity-0'}
            `}
            onError={handleImageError}
            onLoad={handleImageLoad}
          />
          {!imageLoaded && (
            <div className={`${bgColor} ${textColor} h-full w-full flex items-center justify-center font-semibold`}>
              {initials}
            </div>
          )}
        </>
      ) : (
        <div className={`${bgColor} ${textColor} h-full w-full flex items-center justify-center font-semibold`}>
          {initials}
        </div>
      )}
    </div>
  );
};
