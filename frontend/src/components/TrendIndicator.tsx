import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { clsx } from 'clsx';
import { TrendDirection } from './Sparkline';

interface TrendIndicatorProps {
  /** The direction of the trend */
  direction: TrendDirection;
  /** The percentage change value */
  percentChange: number;
  /** Label to show after the percentage (e.g., "vs 1h ago") */
  periodLabel?: string;
  /** Size variant */
  size?: 'sm' | 'md' | 'lg';
  /** Whether to show background */
  showBackground?: boolean;
  /** Optional className */
  className?: string;
}

const SIZE_CLASSES = {
  sm: {
    container: 'px-2 py-1',
    icon: 'h-3 w-3',
    value: 'text-xs',
    label: 'text-xs',
  },
  md: {
    container: 'px-3 py-1.5',
    icon: 'h-4 w-4',
    value: 'text-sm',
    label: 'text-xs',
  },
  lg: {
    container: 'px-4 py-2',
    icon: 'h-5 w-5',
    value: 'text-base',
    label: 'text-sm',
  },
};

const TREND_COLORS = {
  up: {
    text: 'text-emerald-400',
    bg: 'bg-emerald-400/10',
    icon: TrendingUp,
    symbol: '+',
  },
  down: {
    text: 'text-rose-400',
    bg: 'bg-rose-400/10',
    icon: TrendingDown,
    symbol: '',
  },
  stable: {
    text: 'text-secondary',
    bg: 'bg-hover',
    icon: Minus,
    symbol: '',
  },
};

export const TrendIndicator = ({
  direction,
  percentChange,
  periodLabel = 'vs 1h ago',
  size = 'md',
  showBackground = true,
  className,
}: TrendIndicatorProps) => {
  const sizeClasses = SIZE_CLASSES[size];
  const trendConfig = TREND_COLORS[direction];
  const Icon = trendConfig.icon;

  // Format the percentage value
  const formattedValue = direction === 'stable'
    ? 'Stable'
    : `${trendConfig.symbol}${Math.abs(percentChange).toFixed(1)}%`;

  return (
    <div
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-lg font-mono',
        showBackground && trendConfig.bg,
        sizeClasses.container,
        className
      )}
    >
      <Icon className={clsx(sizeClasses.icon, trendConfig.text)} />
      <span className={clsx(sizeClasses.value, 'font-semibold', trendConfig.text)}>
        {formattedValue}
      </span>
      {periodLabel && direction !== 'stable' && (
        <span className={clsx(sizeClasses.label, 'text-tertiary')}>
          {periodLabel}
        </span>
      )}
    </div>
  );
};

// Compact version showing just arrow and percentage
interface TrendArrowProps {
  direction: TrendDirection;
  percentChange: number;
  className?: string;
}

export const TrendArrow = ({ direction, percentChange, className }: TrendArrowProps) => {
  const arrows = {
    up: { symbol: '↑', color: 'text-emerald-400' },
    down: { symbol: '↓', color: 'text-rose-400' },
    stable: { symbol: '→', color: 'text-secondary' },
  };

  const config = arrows[direction];
  const formattedValue = direction === 'stable'
    ? ''
    : `${Math.abs(percentChange).toFixed(1)}%`;

  return (
    <span className={clsx('inline-flex items-center gap-0.5 font-mono text-sm', config.color, className)}>
      <span>{config.symbol}</span>
      {formattedValue && <span className="font-semibold">{formattedValue}</span>}
    </span>
  );
};
