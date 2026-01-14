import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TrendIndicator, TrendArrow } from './TrendIndicator';

describe('TrendIndicator', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render the component', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} />
      );

      expect(container.firstChild).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} className="custom-class" />
      );

      expect(container.firstChild).toHaveClass('custom-class');
    });
  });

  // ========== Upward Trend Tests ==========

  describe('Upward Trend', () => {
    it('should display positive percentage with + symbol', () => {
      render(<TrendIndicator direction="up" percentChange={5.5} />);

      expect(screen.getByText('+5.5%')).toBeInTheDocument();
    });

    it('should display green color for upward trend', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} />
      );

      const valueElement = container.querySelector('.text-emerald-400');
      expect(valueElement).toBeInTheDocument();
    });

    it('should render TrendingUp icon for upward trend', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} />
      );

      // Lucide icons render as SVG
      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('should display period label for upward trend', () => {
      render(
        <TrendIndicator direction="up" percentChange={5.5} periodLabel="vs 24h ago" />
      );

      expect(screen.getByText('vs 24h ago')).toBeInTheDocument();
    });
  });

  // ========== Downward Trend Tests ==========

  describe('Downward Trend', () => {
    it('should display negative percentage without + symbol', () => {
      render(<TrendIndicator direction="down" percentChange={-3.2} />);

      expect(screen.getByText('3.2%')).toBeInTheDocument();
    });

    it('should display red color for downward trend', () => {
      const { container } = render(
        <TrendIndicator direction="down" percentChange={-3.2} />
      );

      const valueElement = container.querySelector('.text-rose-400');
      expect(valueElement).toBeInTheDocument();
    });

    it('should display period label for downward trend', () => {
      render(
        <TrendIndicator direction="down" percentChange={-3.2} periodLabel="vs 1h ago" />
      );

      expect(screen.getByText('vs 1h ago')).toBeInTheDocument();
    });
  });

  // ========== Stable Trend Tests ==========

  describe('Stable Trend', () => {
    it('should display "Stable" text for stable trend', () => {
      render(<TrendIndicator direction="stable" percentChange={0} />);

      expect(screen.getByText('Stable')).toBeInTheDocument();
    });

    it('should display secondary color for stable trend', () => {
      const { container } = render(
        <TrendIndicator direction="stable" percentChange={0} />
      );

      const valueElement = container.querySelector('.text-secondary');
      expect(valueElement).toBeInTheDocument();
    });

    it('should NOT display period label for stable trend', () => {
      render(
        <TrendIndicator direction="stable" percentChange={0} periodLabel="vs 1h ago" />
      );

      expect(screen.queryByText('vs 1h ago')).not.toBeInTheDocument();
    });
  });

  // ========== Size Variants Tests ==========

  describe('Size Variants', () => {
    it('should render small size variant', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} size="sm" />
      );

      expect(container.firstChild).toHaveClass('px-2', 'py-1');
    });

    it('should render medium size variant (default)', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} size="md" />
      );

      expect(container.firstChild).toHaveClass('px-3', 'py-1.5');
    });

    it('should render large size variant', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} size="lg" />
      );

      expect(container.firstChild).toHaveClass('px-4', 'py-2');
    });
  });

  // ========== Background Tests ==========

  describe('Background', () => {
    it('should show background by default', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} />
      );

      expect(container.firstChild).toHaveClass('bg-emerald-400/10');
    });

    it('should show background when showBackground is true', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} showBackground={true} />
      );

      expect(container.firstChild).toHaveClass('bg-emerald-400/10');
    });

    it('should not show background when showBackground is false', () => {
      const { container } = render(
        <TrendIndicator direction="up" percentChange={5.5} showBackground={false} />
      );

      expect(container.firstChild).not.toHaveClass('bg-emerald-400/10');
    });
  });

  // ========== Period Label Tests ==========

  describe('Period Label', () => {
    it('should use default period label "vs 1h ago"', () => {
      render(<TrendIndicator direction="up" percentChange={5.5} />);

      expect(screen.getByText('vs 1h ago')).toBeInTheDocument();
    });

    it('should use custom period label', () => {
      render(
        <TrendIndicator direction="up" percentChange={5.5} periodLabel="vs yesterday" />
      );

      expect(screen.getByText('vs yesterday')).toBeInTheDocument();
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle zero percentage change with upward direction', () => {
      render(<TrendIndicator direction="up" percentChange={0} />);

      expect(screen.getByText('+0.0%')).toBeInTheDocument();
    });

    it('should handle large percentage values', () => {
      render(<TrendIndicator direction="up" percentChange={150.75} />);

      expect(screen.getByText('+150.8%')).toBeInTheDocument();
    });

    it('should format percentage to one decimal place', () => {
      render(<TrendIndicator direction="up" percentChange={5.567} />);

      expect(screen.getByText('+5.6%')).toBeInTheDocument();
    });

    it('should use absolute value for display', () => {
      render(<TrendIndicator direction="down" percentChange={-10.5} />);

      // Should display without negative sign (absolute)
      expect(screen.getByText('10.5%')).toBeInTheDocument();
    });
  });
});

describe('TrendArrow', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render the component', () => {
      const { container } = render(
        <TrendArrow direction="up" percentChange={5.5} />
      );

      expect(container.firstChild).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const { container } = render(
        <TrendArrow direction="up" percentChange={5.5} className="custom-class" />
      );

      expect(container.firstChild).toHaveClass('custom-class');
    });
  });

  // ========== Arrow Symbol Tests ==========

  describe('Arrow Symbols', () => {
    it('should show up arrow for upward trend', () => {
      render(<TrendArrow direction="up" percentChange={5.5} />);

      expect(screen.getByText('↑')).toBeInTheDocument();
    });

    it('should show down arrow for downward trend', () => {
      render(<TrendArrow direction="down" percentChange={-3.2} />);

      expect(screen.getByText('↓')).toBeInTheDocument();
    });

    it('should show right arrow for stable trend', () => {
      render(<TrendArrow direction="stable" percentChange={0} />);

      expect(screen.getByText('→')).toBeInTheDocument();
    });
  });

  // ========== Color Tests ==========

  describe('Colors', () => {
    it('should use green color for upward trend', () => {
      const { container } = render(
        <TrendArrow direction="up" percentChange={5.5} />
      );

      expect(container.firstChild).toHaveClass('text-emerald-400');
    });

    it('should use red color for downward trend', () => {
      const { container } = render(
        <TrendArrow direction="down" percentChange={-3.2} />
      );

      expect(container.firstChild).toHaveClass('text-rose-400');
    });

    it('should use secondary color for stable trend', () => {
      const { container } = render(
        <TrendArrow direction="stable" percentChange={0} />
      );

      expect(container.firstChild).toHaveClass('text-secondary');
    });
  });

  // ========== Percentage Display Tests ==========

  describe('Percentage Display', () => {
    it('should show percentage for upward trend', () => {
      render(<TrendArrow direction="up" percentChange={5.5} />);

      expect(screen.getByText('5.5%')).toBeInTheDocument();
    });

    it('should show percentage for downward trend (absolute value)', () => {
      render(<TrendArrow direction="down" percentChange={-3.2} />);

      expect(screen.getByText('3.2%')).toBeInTheDocument();
    });

    it('should NOT show percentage for stable trend', () => {
      render(<TrendArrow direction="stable" percentChange={0} />);

      expect(screen.queryByText('0.0%')).not.toBeInTheDocument();
      expect(screen.queryByText('%')).not.toBeInTheDocument();
    });
  });
});
