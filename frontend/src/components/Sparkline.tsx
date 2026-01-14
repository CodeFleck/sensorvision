import { useMemo } from 'react';
import { clsx } from 'clsx';

export type TrendDirection = 'up' | 'down' | 'stable';

interface SparklineProps {
  /** Array of numeric values to plot */
  data: number[];
  /** Width of the sparkline SVG */
  width?: number;
  /** Height of the sparkline SVG */
  height?: number;
  /** Stroke width of the line */
  strokeWidth?: number;
  /** Color of the line (defaults based on trend) */
  color?: string;
  /** Whether to show the area fill under the line */
  showArea?: boolean;
  /** Optional className */
  className?: string;
  /** Whether to animate the line drawing */
  animate?: boolean;
}

export const Sparkline = ({
  data,
  width = 100,
  height = 40,
  strokeWidth = 2,
  color,
  showArea = true,
  className,
  animate = true,
}: SparklineProps) => {
  // Calculate trend and path
  const { path, areaPath, trend, lineColor } = useMemo(() => {
    if (data.length < 2) {
      return { path: '', areaPath: '', trend: 'stable' as TrendDirection, lineColor: '#8888a0' };
    }

    const padding = strokeWidth;
    const chartWidth = width - padding * 2;
    const chartHeight = height - padding * 2;

    // Find min/max for scaling
    const min = Math.min(...data);
    const max = Math.max(...data);
    const range = max - min || 1;

    // Calculate normalized points
    const points = data.map((value, index) => {
      const x = padding + (index / (data.length - 1)) * chartWidth;
      const y = padding + chartHeight - ((value - min) / range) * chartHeight;
      return { x, y };
    });

    // Create SVG path
    const path = points
      .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`)
      .join(' ');

    // Create area path
    const areaPath = `${path} L ${points[points.length - 1].x.toFixed(2)} ${height - padding} L ${padding} ${height - padding} Z`;

    // Determine trend
    const firstValue = data[0];
    const lastValue = data[data.length - 1];
    const percentChange = ((lastValue - firstValue) / firstValue) * 100;

    let trend: TrendDirection;
    if (percentChange > 1) {
      trend = 'up';
    } else if (percentChange < -1) {
      trend = 'down';
    } else {
      trend = 'stable';
    }

    // Determine line color based on trend
    const lineColor = color || (trend === 'up' ? '#10b981' : trend === 'down' ? '#f43f5e' : '#00d4ff');

    return { path, areaPath, trend, lineColor };
  }, [data, width, height, strokeWidth, color]);

  if (data.length < 2) {
    return (
      <div className={clsx('flex items-center justify-center', className)} style={{ width, height }}>
        <span className="text-xs text-secondary">No data</span>
      </div>
    );
  }

  // Generate unique gradient ID
  const gradientId = `sparkline-gradient-${Math.random().toString(36).substr(2, 9)}`;

  return (
    <svg
      width={width}
      height={height}
      className={clsx('sparkline-svg', className)}
      role="img"
      aria-label={`Trend: ${trend}`}
    >
      <defs>
        <linearGradient id={gradientId} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor={lineColor} stopOpacity="0.3" />
          <stop offset="100%" stopColor={lineColor} stopOpacity="0" />
        </linearGradient>
      </defs>
      {/* Area fill */}
      {showArea && (
        <path
          d={areaPath}
          fill={`url(#${gradientId})`}
          className="sparkline-area"
        />
      )}
      {/* Line */}
      <path
        d={path}
        fill="none"
        stroke={lineColor}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeLinejoin="round"
        className={clsx(
          'sparkline-path',
          animate && 'sparkline-animate'
        )}
        style={{
          filter: `drop-shadow(0 0 6px ${lineColor}80)`,
        }}
      />
    </svg>
  );
};

// Helper function to calculate trend from data
export const calculateTrend = (data: number[]): { direction: TrendDirection; percentChange: number } => {
  if (data.length < 2) {
    return { direction: 'stable', percentChange: 0 };
  }

  const firstValue = data[0];
  const lastValue = data[data.length - 1];

  if (firstValue === 0) {
    return { direction: lastValue > 0 ? 'up' : 'stable', percentChange: 0 };
  }

  const percentChange = ((lastValue - firstValue) / Math.abs(firstValue)) * 100;

  let direction: TrendDirection;
  if (percentChange > 1) {
    direction = 'up';
  } else if (percentChange < -1) {
    direction = 'down';
  } else {
    direction = 'stable';
  }

  return { direction, percentChange };
};
