import axios from 'axios';

const API_BASE_URL = '/api/v1';

/**
 * Upload avatar image for a user
 */
export const uploadAvatar = async (userId: number, file: File): Promise<any> => {
  const formData = new FormData();
  formData.append('file', file);

  const token = localStorage.getItem('accessToken');

  const response = await axios.post(
    `${API_BASE_URL}/users/${userId}/avatar`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  return response.data;
};

/**
 * Delete avatar for a user
 */
export const deleteAvatar = async (userId: number): Promise<any> => {
  const token = localStorage.getItem('accessToken');

  const response = await axios.delete(
    `${API_BASE_URL}/users/${userId}/avatar`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  return response.data;
};

/**
 * Check avatar status for a user
 */
export const getAvatarStatus = async (userId: number): Promise<any> => {
  const token = localStorage.getItem('accessToken');

  const response = await axios.get(
    `${API_BASE_URL}/users/${userId}/avatar/status`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  return response.data;
};
