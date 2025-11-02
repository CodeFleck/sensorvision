import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useAuth } from './AuthContext';

// Extended theme options with dark mode variants
type ThemeMode = 'light' | 'dark' | 'dark-dimmed' | 'dark-high-contrast' | 'system';
type EffectiveTheme = 'light' | 'dark' | 'dark-dimmed' | 'dark-high-contrast';

interface ThemeContextType {
  theme: ThemeMode;
  effectiveTheme: EffectiveTheme;
  setTheme: (theme: ThemeMode) => Promise<void>;
  isLoading: boolean;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

// eslint-disable-next-line react-refresh/only-export-components
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

interface ThemeProviderProps {
  children: ReactNode;
}

// Get system preference
const getSystemTheme = (): 'light' | 'dark' => {
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return 'light';
};

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
  const { user } = useAuth();
  const [theme, setThemeState] = useState<ThemeMode>('system');
  const [systemTheme, setSystemTheme] = useState<'light' | 'dark'>(getSystemTheme());
  const [isLoading, setIsLoading] = useState(true);

  // Determine the effective theme
  const effectiveTheme: EffectiveTheme = theme === 'system' ? systemTheme : theme;

  // Listen for system theme changes
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = (e: MediaQueryListEvent) => {
      setSystemTheme(e.matches ? 'dark' : 'light');
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // Apply theme to DOM with smooth transition
  useEffect(() => {
    const root = window.document.documentElement;

    // Add transitioning class for smooth fade
    root.classList.add('theme-transitioning');

    // Remove all theme classes
    root.classList.remove('light', 'dark', 'dark-dimmed', 'dark-high-contrast');

    // Add new theme class
    root.classList.add(effectiveTheme);

    // Remove transitioning class after animation completes
    const timer = setTimeout(() => {
      root.classList.remove('theme-transitioning');
      setIsLoading(false);
    }, 300);

    return () => clearTimeout(timer);
  }, [effectiveTheme]);

  // Initialize theme from user preference or localStorage
  useEffect(() => {
    const validThemes = ['light', 'dark', 'dark-dimmed', 'dark-high-contrast', 'system'];

    if (user?.themePreference && validThemes.includes(user.themePreference)) {
      setThemeState(user.themePreference as ThemeMode);
    } else {
      const stored = localStorage.getItem('theme') as ThemeMode | null;
      if (stored && validThemes.includes(stored)) {
        setThemeState(stored);
      }
    }
  }, [user]);

  // Update theme and persist to backend if authenticated
  const setTheme = async (newTheme: ThemeMode) => {
    setThemeState(newTheme);
    localStorage.setItem('theme', newTheme);

    // If user is authenticated, update preference on backend
    if (user) {
      try {
        const response = await fetch('/api/v1/auth/preferences', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
          },
          body: JSON.stringify({ themePreference: newTheme }),
        });

        if (!response.ok) {
          console.error('Failed to update theme preference on backend');
        }
      } catch (error) {
        console.error('Error updating theme preference:', error);
      }
    }
  };

  const value = {
    theme,
    effectiveTheme,
    setTheme,
    isLoading,
  };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};
