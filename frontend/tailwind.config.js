/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class', // Enable dark mode with class strategy
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Satoshi', 'system-ui', 'sans-serif'],
        display: ['Satoshi', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Menlo', 'monospace'],
      },
      colors: {
        // Map Tailwind colors to CSS variables
        canvas: 'var(--bg-canvas)',
        primary: 'var(--bg-primary)',
        secondary: 'var(--bg-secondary)',
        tertiary: 'var(--bg-tertiary)',
        card: 'var(--bg-card)',
        modal: 'var(--bg-modal)',
        dropdown: 'var(--bg-dropdown)',

        // Common color mappings
        white: 'var(--bg-primary)',
        gray: {
          50: 'var(--bg-secondary)',
          100: 'var(--bg-tertiary)',
          200: 'var(--border-subtle)',
          300: 'var(--border-muted)',
          400: 'var(--border-default)',
          500: 'var(--text-tertiary)',
          600: 'var(--text-secondary)',
          700: 'var(--text-secondary)',
          800: 'var(--text-primary)',
          900: 'var(--text-primary)',
        },

        blue: {
          500: 'var(--accent-primary)',
          600: 'var(--text-link)',
          700: 'var(--text-link-hover)',
        },

        red: {
          500: 'var(--accent-danger)',
          600: 'var(--text-danger)',
        },

        green: {
          500: 'var(--accent-success)',
          600: 'var(--text-success)',
        },

        yellow: {
          500: 'var(--accent-warning)',
          600: 'var(--text-warning)',
        },
      },
      backgroundColor: {
        // Background utilities
        canvas: 'var(--bg-canvas)',
        primary: 'var(--bg-primary)',
        secondary: 'var(--bg-secondary)',
        tertiary: 'var(--bg-tertiary)',
        overlay: 'var(--bg-overlay)',
        hover: 'var(--bg-hover)',
        active: 'var(--bg-active)',
        disabled: 'var(--bg-disabled)',
        card: 'var(--bg-card)',
        modal: 'var(--bg-modal)',
        dropdown: 'var(--bg-dropdown)',
      },
      textColor: {
        // Text color utilities
        primary: 'var(--text-primary)',
        secondary: 'var(--text-secondary)',
        tertiary: 'var(--text-tertiary)',
        disabled: 'var(--text-disabled)',
        link: 'var(--text-link)',
        'link-hover': 'var(--text-link-hover)',
        danger: 'var(--text-danger)',
        success: 'var(--text-success)',
        warning: 'var(--text-warning)',
        info: 'var(--text-info)',
      },
      borderColor: {
        // Border utilities
        DEFAULT: 'var(--border-default)',
        muted: 'var(--border-muted)',
        subtle: 'var(--border-subtle)',
        focus: 'var(--border-focus)',
        error: 'var(--border-error)',
      },
      boxShadow: {
        sm: 'var(--shadow-sm)',
        DEFAULT: 'var(--shadow-md)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
        xl: 'var(--shadow-xl)',
        '2xl': 'var(--shadow-2xl)',
      },
      spacing: {
        xs: 'var(--spacing-xs)',
        sm: 'var(--spacing-sm)',
        md: 'var(--spacing-md)',
        lg: 'var(--spacing-lg)',
        xl: 'var(--spacing-xl)',
        '2xl': 'var(--spacing-2xl)',
        '3xl': 'var(--spacing-3xl)',
      },
      fontSize: {
        xs: 'var(--font-size-xs)',
        sm: 'var(--font-size-sm)',
        base: 'var(--font-size-base)',
        lg: 'var(--font-size-lg)',
        xl: 'var(--font-size-xl)',
        '2xl': 'var(--font-size-2xl)',
        '3xl': 'var(--font-size-3xl)',
        '4xl': 'var(--font-size-4xl)',
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
        '2xl': 'var(--radius-2xl)',
        full: 'var(--radius-full)',
      },
    },
  },
  plugins: [],
}