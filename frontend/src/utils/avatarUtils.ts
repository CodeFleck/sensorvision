/**
 * Utility functions for avatar display and generation
 */

/**
 * Get initials from username
 * Takes first 2 characters, uppercase
 */
export const getInitials = (username: string): string => {
  if (!username) return 'U';
  return username.slice(0, 2).toUpperCase();
};

/**
 * Get a consistent color for a username
 * Uses a simple hash function to map username to a color
 */
export const getAvatarColor = (username: string): string => {
  const colors = [
    'bg-blue-500',
    'bg-purple-500',
    'bg-green-500',
    'bg-yellow-500',
    'bg-pink-500',
    'bg-indigo-500',
    'bg-red-500',
    'bg-teal-500',
    'bg-orange-500',
    'bg-cyan-500',
  ];

  if (!username) return colors[0];

  // Simple hash function
  const hash = username.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + acc;
  }, 0);

  return colors[hash % colors.length];
};

/**
 * Get text color that contrasts with the background
 * All our background colors are vibrant, so white text works best
 */
export const getAvatarTextColor = (): string => {
  return 'text-white';
};

/**
 * Get avatar URL with cache busting
 */
export const getAvatarUrl = (userId: number, avatarVersion?: number): string => {
  const baseUrl = `/api/v1/users/${userId}/avatar`;
  return avatarVersion ? `${baseUrl}?v=${avatarVersion}` : baseUrl;
};
