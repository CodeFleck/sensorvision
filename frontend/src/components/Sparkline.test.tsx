import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Sparkline, calculateTrend } from './Sparkline';

describe('Sparkline', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render an SVG element', () => {
      const data = [10, 20, 15, 25, 30];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should render with default dimensions', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toHaveAttribute('width', '100');
      expect(svg).toHaveAttribute('height', '40');
    });

    it('should render with custom dimensions', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} width={200} height={60} />);

      const svg = container.querySelector('svg');
      expect(svg).toHaveAttribute('width', '200');
      expect(svg).toHaveAttribute('height', '60');
    });

    it('should have accessible role and aria-label', () => {
      const data = [10, 20, 30];
      render(<Sparkline data={data} />);

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', expect.stringContaining('Trend:'));
    });

    it('should apply custom className', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} className="custom-class" />);

      const svg = container.querySelector('svg');
      expect(svg).toHaveClass('custom-class');
    });
  });

  // ========== Path Generation Tests ==========

  describe('Path Generation', () => {
    it('should render a path element for the line', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} />);

      const paths = container.querySelectorAll('path');
      // Should have area path and line path
      expect(paths.length).toBeGreaterThanOrEqual(1);
    });

    it('should render area fill when showArea is true (default)', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} showArea={true} />);

      const areaPath = container.querySelector('.sparkline-area');
      expect(areaPath).toBeInTheDocument();
    });

    it('should not render area fill when showArea is false', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} showArea={false} />);

      const areaPath = container.querySelector('.sparkline-area');
      expect(areaPath).not.toBeInTheDocument();
    });

    it('should render line path with sparkline-path class', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toBeInTheDocument();
      expect(linePath).toHaveAttribute('fill', 'none');
    });
  });

  // ========== Insufficient Data Tests ==========

  describe('Insufficient Data Handling', () => {
    it('should show "No data" message with empty array', () => {
      render(<Sparkline data={[]} />);

      expect(screen.getByText('No data')).toBeInTheDocument();
    });

    it('should show "No data" message with single value', () => {
      render(<Sparkline data={[42]} />);

      expect(screen.getByText('No data')).toBeInTheDocument();
    });

    it('should not render SVG with insufficient data', () => {
      const { container } = render(<Sparkline data={[]} />);

      const svg = container.querySelector('svg');
      expect(svg).not.toBeInTheDocument();
    });
  });

  // ========== Trend Direction Tests ==========

  describe('Trend Direction', () => {
    it('should show upward trend aria-label when values increase', () => {
      const data = [10, 15, 20, 25, 30];
      render(<Sparkline data={data} />);

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', 'Trend: up');
    });

    it('should show downward trend aria-label when values decrease', () => {
      const data = [30, 25, 20, 15, 10];
      render(<Sparkline data={data} />);

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', 'Trend: down');
    });

    it('should show stable trend aria-label when values are similar', () => {
      const data = [20, 20.1, 19.9, 20, 20.05];
      render(<Sparkline data={data} />);

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', 'Trend: stable');
    });
  });

  // ========== Color Tests ==========

  describe('Colors', () => {
    it('should use custom color when provided', () => {
      const data = [10, 20, 30];
      const customColor = '#ff5500';
      const { container } = render(<Sparkline data={data} color={customColor} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke', customColor);
    });

    it('should use green color for upward trend (default)', () => {
      const data = [10, 15, 20, 25, 30];
      const { container } = render(<Sparkline data={data} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke', '#10b981');
    });

    it('should use red color for downward trend (default)', () => {
      const data = [30, 25, 20, 15, 10];
      const { container } = render(<Sparkline data={data} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke', '#f43f5e');
    });

    it('should use cyan color for stable trend (default)', () => {
      const data = [20, 20.1, 19.9, 20, 20.05];
      const { container } = render(<Sparkline data={data} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke', '#00d4ff');
    });
  });

  // ========== Animation Tests ==========

  describe('Animation', () => {
    it('should have animation class when animate is true (default)', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} animate={true} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveClass('sparkline-animate');
    });

    it('should not have animation class when animate is false', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} animate={false} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).not.toHaveClass('sparkline-animate');
    });
  });

  // ========== Stroke Width Tests ==========

  describe('Stroke Width', () => {
    it('should render with default stroke width', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke-width', '2');
    });

    it('should render with custom stroke width', () => {
      const data = [10, 20, 30];
      const { container } = render(<Sparkline data={data} strokeWidth={4} />);

      const linePath = container.querySelector('.sparkline-path');
      expect(linePath).toHaveAttribute('stroke-width', '4');
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle all same values', () => {
      const data = [50, 50, 50, 50, 50];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should handle negative values', () => {
      const data = [-10, -5, 0, 5, 10];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should handle very large values', () => {
      const data = [1000000, 2000000, 1500000, 3000000];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should handle very small decimal values', () => {
      const data = [0.001, 0.002, 0.0015, 0.003];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should handle two data points', () => {
      const data = [10, 20];
      const { container } = render(<Sparkline data={data} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });
  });
});

describe('calculateTrend', () => {
  // ========== Basic Trend Calculation ==========

  describe('Basic Calculation', () => {
    it('should return "up" direction when values increase significantly', () => {
      const result = calculateTrend([10, 15, 20, 25, 30]);
      expect(result.direction).toBe('up');
      expect(result.percentChange).toBeGreaterThan(1);
    });

    it('should return "down" direction when values decrease significantly', () => {
      const result = calculateTrend([30, 25, 20, 15, 10]);
      expect(result.direction).toBe('down');
      expect(result.percentChange).toBeLessThan(-1);
    });

    it('should return "stable" direction when values are similar', () => {
      const result = calculateTrend([20, 20.1, 19.9, 20, 20.05]);
      expect(result.direction).toBe('stable');
      expect(Math.abs(result.percentChange)).toBeLessThanOrEqual(1);
    });
  });

  // ========== Percentage Calculation ==========

  describe('Percentage Calculation', () => {
    it('should calculate correct percentage for 100% increase', () => {
      const result = calculateTrend([10, 20]);
      expect(result.percentChange).toBe(100);
    });

    it('should calculate correct percentage for 50% decrease', () => {
      const result = calculateTrend([100, 50]);
      expect(result.percentChange).toBe(-50);
    });

    it('should calculate correct percentage for no change', () => {
      const result = calculateTrend([50, 50]);
      expect(result.percentChange).toBe(0);
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should return stable for empty array', () => {
      const result = calculateTrend([]);
      expect(result.direction).toBe('stable');
      expect(result.percentChange).toBe(0);
    });

    it('should return stable for single value', () => {
      const result = calculateTrend([42]);
      expect(result.direction).toBe('stable');
      expect(result.percentChange).toBe(0);
    });

    it('should handle first value being zero', () => {
      const result = calculateTrend([0, 10, 20]);
      // When first value is 0, special handling
      expect(result.direction).toBe('up');
      expect(result.percentChange).toBe(0); // Cannot calculate % from zero
    });

    it('should handle negative values correctly', () => {
      const result = calculateTrend([-20, -10]);
      expect(result.direction).toBe('up');
      expect(result.percentChange).toBe(50); // -10 is 50% more than -20
    });

    it('should handle very small changes as stable', () => {
      const result = calculateTrend([100, 100.5]);
      expect(result.direction).toBe('stable');
    });
  });
});
