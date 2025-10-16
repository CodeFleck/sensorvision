import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../types';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Decode JWT to extract roles
function decodeToken(token: string): { roles: string[] } | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const payload = JSON.parse(jsonPayload);
    return {
      roles: payload.roles || []
    };
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
}

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [userRoles, setUserRoles] = useState<string[]>([]);

  // Check for existing token and fetch user on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      // Decode token to get roles immediately
      const decoded = decodeToken(token);
      if (decoded) {
        setUserRoles(decoded.roles);
      }
      fetchCurrentUser();
    } else {
      setLoading(false);
    }
  }, []);

  const fetchCurrentUser = async () => {
    try {
      const response = await fetch('/api/v1/auth/me', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        },
      });

      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
      } else {
        // Token is invalid, clear it
        localStorage.removeItem('accessToken');
      }
    } catch (error) {
      console.error('Failed to fetch current user:', error);
      localStorage.removeItem('accessToken');
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
      });

      if (!response.ok) {
        const contentType = response.headers.get('content-type');
        let errorMessage = 'Login failed';

        if (contentType && contentType.includes('application/json')) {
          const error = await response.json();
          console.error('Login error response:', error);
          errorMessage = error.message || error.detail || `Login failed: ${response.status} ${response.statusText}`;
        } else {
          const errorText = await response.text();
          console.error('Login error (non-JSON):', errorText);
          errorMessage = `Login failed: ${response.status} ${response.statusText}`;
        }

        throw new Error(errorMessage);
      }

      const authResponse: AuthResponse = await response.json();
      localStorage.setItem('accessToken', authResponse.accessToken);

      // Decode token to get roles
      const decoded = decodeToken(authResponse.accessToken);
      if (decoded) {
        setUserRoles(decoded.roles);
      }

      // Fetch full user details
      await fetchCurrentUser();
    } catch (error) {
      console.error('Login exception:', error);
      throw error;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      console.log('Attempting registration with data:', { ...data, password: '[REDACTED]' });

      const response = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      });

      console.log('Registration response status:', response.status, response.statusText);

      if (!response.ok) {
        const contentType = response.headers.get('content-type');
        let errorMessage = 'Registration failed';

        if (contentType && contentType.includes('application/json')) {
          const error = await response.json();
          console.error('Registration error response:', error);
          errorMessage = error.message || error.detail || `Registration failed: ${response.status} ${response.statusText}`;
        } else {
          const errorText = await response.text();
          console.error('Registration error (non-JSON):', errorText);
          errorMessage = `Registration failed: ${response.status} ${response.statusText}`;
        }

        throw new Error(errorMessage);
      }

      const authResponse: AuthResponse = await response.json();
      console.log('Registration successful, token received');
      localStorage.setItem('accessToken', authResponse.accessToken);

      // Decode token to get roles
      const decoded = decodeToken(authResponse.accessToken);
      if (decoded) {
        setUserRoles(decoded.roles);
      }

      // Fetch full user details
      await fetchCurrentUser();
    } catch (error) {
      console.error('Registration exception:', error);
      throw error;
    }
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setUser(null);
    setUserRoles([]);
    window.location.href = '/login';
  };

  const hasRole = (role: string): boolean => {
    return userRoles.includes(role);
  };

  const isAdmin = userRoles.includes('ROLE_ADMIN');

  const value = {
    user,
    loading,
    login,
    register,
    logout,
    isAuthenticated: !!user,
    isAdmin,
    hasRole,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
