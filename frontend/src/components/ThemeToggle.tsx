import React, { useState } from 'react';
import { useTheme } from '../contexts/ThemeContext';

const ThemeToggle: React.FC = () => {
  const { theme, setTheme, effectiveTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(false);

  const themes = [
    {
      value: 'light-luxury' as const,
      label: 'Light Luxury',
      description: 'Premium light with teal accents',
      icon: '✨',
      colors: ['#fafafa', '#ffffff', '#0d9488'],
    },
    {
      value: 'dark-luxury' as const,
      label: 'Dark Luxury',
      description: 'Premium OLED dark with teal',
      icon: '🌌',
      colors: ['#000000', '#0a0a0a', '#14b8a6'],
    },
    {
      value: 'light' as const,
      label: 'Light',
      description: 'Classic bright mode',
      icon: '☀️',
      colors: ['#ffffff', '#f6f8fa', '#0969da'],
    },
    {
      value: 'dark' as const,
      label: 'Dark',
      description: 'Balanced contrast',
      icon: '🌙',
      colors: ['#0d1117', '#161b22', '#58a6ff'],
    },
    {
      value: 'dark-dimmed' as const,
      label: 'Dark Dimmed',
      description: 'Softer, warmer tones',
      icon: '🌆',
      colors: ['#22272e', '#2d333b', '#539bf5'],
    },
    {
      value: 'dark-high-contrast' as const,
      label: 'Dark (High Contrast)',
      description: 'Maximum readability',
      icon: '🔆',
      colors: ['#010409', '#0a0c10', '#71b7ff'],
    },
    {
      value: 'system' as const,
      label: 'System',
      description: 'Follows OS settings',
      icon: '💻',
      colors: ['#808080', '#a0a0a0', '#606060'],
    },
  ];

  const currentTheme = themes.find(t => t.value === theme) || themes[4];

  const handleThemeChange = async (newTheme: typeof theme) => {
    await setTheme(newTheme);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="group flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-all duration-200 hover:scale-105"
        style={{
          backgroundColor: 'var(--bg-secondary)',
          color: 'var(--text-primary)',
          border: '1px solid var(--border-muted)',
        }}
        title={`Current theme: ${currentTheme.label}`}
      >
        <span className="text-lg group-hover:scale-110 transition-transform duration-200">
          {currentTheme.icon}
        </span>
        <span className="hidden md:inline">{currentTheme.label}</span>
        <svg
          className={`w-4 h-4 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          <div
            className="absolute right-0 mt-2 w-80 rounded-xl shadow-2xl border z-20 overflow-hidden animate-slideIn"
            style={{
              backgroundColor: 'var(--bg-primary)',
              borderColor: 'var(--border-default)',
              boxShadow: 'var(--shadow-xl)',
            }}
          >
            <div className="px-4 py-3 border-b" style={{ borderColor: 'var(--border-muted)' }}>
              <h3
                className="text-sm font-semibold"
                style={{ color: 'var(--text-primary)' }}
              >
                Choose Theme
              </h3>
              <p
                className="text-xs mt-0.5"
                style={{ color: 'var(--text-secondary)' }}
              >
                Select your preferred color scheme
              </p>
            </div>

            <div className="p-2 max-h-96 overflow-y-auto">
              {themes.map((themeOption, index) => {
                const isSelected = theme === themeOption.value;
                return (
                  <button
                    key={themeOption.value}
                    onClick={() => handleThemeChange(themeOption.value)}
                    className="w-full group rounded-lg p-3 mb-1 transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]"
                    style={{
                      backgroundColor: isSelected ? 'var(--bg-hover)' : 'transparent',
                      border: `2px solid ${isSelected ? 'var(--accent-primary)' : 'transparent'}`,
                    }}
                  >
                    <div className="flex items-start gap-3">
                      <div className="flex-shrink-0 text-2xl group-hover:scale-110 transition-transform duration-200">
                        {themeOption.icon}
                      </div>

                      <div className="flex-1 text-left">
                        <div className="flex items-center gap-2">
                          <span
                            className="text-sm font-semibold"
                            style={{ color: 'var(--text-primary)' }}
                          >
                            {themeOption.label}
                          </span>
                          {isSelected && (
                            <svg
                              className="w-4 h-4 animate-fadeIn"
                              style={{ color: 'var(--accent-primary)' }}
                              fill="currentColor"
                              viewBox="0 0 20 20"
                            >
                              <path
                                fillRule="evenodd"
                                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                clipRule="evenodd"
                              />
                            </svg>
                          )}
                        </div>
                        <p
                          className="text-xs mt-0.5"
                          style={{ color: 'var(--text-secondary)' }}
                        >
                          {themeOption.description}
                        </p>

                        {/* Color Swatches */}
                        <div className="flex gap-1.5 mt-2">
                          {themeOption.colors.map((color, i) => (
                            <div
                              key={i}
                              className="w-6 h-6 rounded-md shadow-sm ring-1 ring-black/10 group-hover:scale-110 transition-transform duration-200"
                              style={{
                                backgroundColor: color,
                                animationDelay: `${index * 50 + i * 30}ms`,
                              }}
                              title={color}
                            />
                          ))}
                        </div>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>

            <div
              className="px-4 py-2 border-t text-xs"
              style={{
                borderColor: 'var(--border-muted)',
                color: 'var(--text-tertiary)',
              }}
            >
              <div className="flex items-center gap-1.5">
                <span className="animate-pulse-slow">✨</span>
                <span>Current: <strong style={{ color: 'var(--text-primary)' }}>{effectiveTheme}</strong></span>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ThemeToggle;
