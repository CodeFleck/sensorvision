import { expect, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

// Extend Vitest's expect with jest-dom matchers
expect.extend(matchers);

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {}, // deprecated
    removeListener: () => {}, // deprecated
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
});

// Mock window.location
delete (window as any).location;
(window as any).location = {
  origin: 'http://localhost:3000',
  protocol: 'http:',
  host: 'localhost:3000',
  href: 'http://localhost:3000',
};

// Mock navigator.clipboard - make it configurable so userEvent can override it
Object.defineProperty(navigator, 'clipboard', {
  writable: true,
  configurable: true, // Allow userEvent to redefine this
  value: {
    writeText: async (_text: string) => {
      return Promise.resolve();
    },
    readText: async () => {
      return Promise.resolve('');
    },
  },
});

// Mock global alert and confirm
globalThis.alert = () => {};
globalThis.confirm = () => true;
