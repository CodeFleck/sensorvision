import { describe, it, expect } from 'vitest';
import { toCamelCase, getTelemetryValue } from './stringUtils';

describe('toCamelCase', () => {
  describe('standard conversions', () => {
    it('converts snake_case to camelCase', () => {
      expect(toCamelCase('kw_consumption')).toBe('kwConsumption');
      expect(toCamelCase('power_factor')).toBe('powerFactor');
    });

    it('handles multiple underscores', () => {
      expect(toCamelCase('under_score_test')).toBe('underScoreTest');
      expect(toCamelCase('a_b_c_d')).toBe('aBCD');
    });
  });

  describe('already camelCase strings', () => {
    it('returns camelCase unchanged', () => {
      expect(toCamelCase('kwConsumption')).toBe('kwConsumption');
      expect(toCamelCase('powerFactor')).toBe('powerFactor');
    });
  });

  describe('strings without underscores', () => {
    it('returns single words unchanged', () => {
      expect(toCamelCase('temperature')).toBe('temperature');
      expect(toCamelCase('voltage')).toBe('voltage');
      expect(toCamelCase('current')).toBe('current');
    });
  });

  describe('edge cases', () => {
    it('handles empty string', () => {
      expect(toCamelCase('')).toBe('');
    });

    it('handles single underscore', () => {
      expect(toCamelCase('_')).toBe('_');
    });

    it('handles leading underscore followed by lowercase', () => {
      // Leading underscore with lowercase letter gets converted
      expect(toCamelCase('_leading')).toBe('Leading');
    });

    it('handles trailing underscore', () => {
      expect(toCamelCase('trailing_')).toBe('trailing_');
    });

    it('handles consecutive underscores', () => {
      // Only underscore followed by lowercase letter is converted
      expect(toCamelCase('double__underscore')).toBe('double_Underscore');
    });

    it('handles underscore followed by uppercase (no conversion)', () => {
      // Regex only matches lowercase after underscore
      expect(toCamelCase('kw_CONSUMPTION')).toBe('kw_CONSUMPTION');
    });

    it('handles underscore followed by number (no conversion)', () => {
      expect(toCamelCase('value_123')).toBe('value_123');
    });
  });
});

describe('getTelemetryValue', () => {
  describe('camelCase property retrieval', () => {
    it('retrieves value from camelCase property', () => {
      const data = { kwConsumption: 42.5 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(42.5);
    });

    it('retrieves value when varName is already camelCase', () => {
      const data = { kwConsumption: 42.5 };
      expect(getTelemetryValue(data, 'kwConsumption')).toBe(42.5);
    });
  });

  describe('snake_case fallback', () => {
    it('falls back to snake_case property when camelCase not found', () => {
      const data = { kw_consumption: 42.5 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(42.5);
    });

    it('prefers camelCase over snake_case when both exist', () => {
      const data = { kwConsumption: 100, kw_consumption: 42.5 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(100);
    });
  });

  describe('missing properties', () => {
    it('returns undefined for missing property', () => {
      const data = { other: 100 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for empty object', () => {
      expect(getTelemetryValue({}, 'kw_consumption')).toBeUndefined();
    });
  });

  describe('non-numeric values', () => {
    it('returns undefined for string value', () => {
      const data = { kwConsumption: 'not a number' };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for null value', () => {
      const data = { kwConsumption: null };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for undefined value', () => {
      const data = { kwConsumption: undefined };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for object value', () => {
      const data = { kwConsumption: { nested: 42 } };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for array value', () => {
      const data = { kwConsumption: [42, 43] };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });

    it('returns undefined for boolean value', () => {
      const data = { kwConsumption: true };
      expect(getTelemetryValue(data, 'kw_consumption')).toBeUndefined();
    });
  });

  describe('numeric edge cases', () => {
    it('handles zero value', () => {
      const data = { kwConsumption: 0 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(0);
    });

    it('handles negative value', () => {
      const data = { kwConsumption: -42.5 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(-42.5);
    });

    it('handles very large value', () => {
      const data = { kwConsumption: 1e10 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(1e10);
    });

    it('handles very small value', () => {
      const data = { kwConsumption: 0.0001 };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(0.0001);
    });

    it('handles NaN (typeof is number but value is NaN)', () => {
      const data = { kwConsumption: NaN };
      // NaN is typeof 'number', so it gets returned
      expect(getTelemetryValue(data, 'kw_consumption')).toBeNaN();
    });

    it('handles Infinity', () => {
      const data = { kwConsumption: Infinity };
      expect(getTelemetryValue(data, 'kw_consumption')).toBe(Infinity);
    });
  });
});
