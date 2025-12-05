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
    addListener: () => { /* deprecated - intentionally empty */ },
    removeListener: () => { /* deprecated - intentionally empty */ },
    addEventListener: () => { /* mock - intentionally empty */ },
    removeEventListener: () => { /* mock - intentionally empty */ },
    dispatchEvent: () => { /* mock - intentionally empty */ },
  }),
});

// Mock window.location
delete (window as unknown as { location: unknown }).location;
(window as unknown as { location: object }).location = {
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
    writeText: async () => {
      return Promise.resolve();
    },
    readText: async () => {
      return Promise.resolve('');
    },
  },
});

// Mock global alert and confirm
globalThis.alert = () => { /* mock - intentionally empty */ };
globalThis.confirm = () => true;
