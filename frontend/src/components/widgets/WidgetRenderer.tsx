import React from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { WidgetContainer } from './WidgetContainer';
import { GaugeWidget } from './GaugeWidget';
import { MetricCardWidget } from './MetricCardWidget';
import { LineChartWidget } from './LineChartWidget';
import { BarChartWidget } from './BarChartWidget';
import { TableWidget } from './TableWidget';
import { PieChartWidget } from './PieChartWidget';
import { AreaChartWidget } from './AreaChartWidget';
import { ScatterChartWidget } from './ScatterChartWidget';
import { IndicatorWidget } from './IndicatorWidget';
import { ControlButtonWidget } from './ControlButtonWidget';
import { MapWidget } from './MapWidget';

interface WidgetRendererProps {
  widget: Widget;
  contextDeviceId?: string;
  latestData?: TelemetryPoint;
  onDelete?: () => void;
  onEdit?: () => void;
}

export const WidgetRenderer: React.FC<WidgetRendererProps> = ({ widget, contextDeviceId, latestData, onDelete, onEdit }) => {
  // Determine effective device ID: use context device if widget is configured for it
  // TODO: Use this for fetching device-specific data in widgets
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const effectiveDeviceId = widget.useContextDevice && contextDeviceId
    ? contextDeviceId
    : widget.deviceId;

  const renderWidgetContent = () => {
    switch (widget.type) {
      case 'GAUGE':
        return <GaugeWidget widget={widget} latestData={latestData} />;
      case 'METRIC_CARD':
        return <MetricCardWidget widget={widget} latestData={latestData} />;
      case 'LINE_CHART':
        return <LineChartWidget widget={widget} latestData={latestData} />;
      case 'BAR_CHART':
        return <BarChartWidget widget={widget} latestData={latestData} />;
      case 'PIE_CHART':
        return <PieChartWidget widget={widget} latestData={latestData} />;
      case 'AREA_CHART':
        return <AreaChartWidget widget={widget} latestData={latestData} />;
      case 'SCATTER_CHART':
        return <ScatterChartWidget widget={widget} latestData={latestData} />;
      case 'INDICATOR':
        return <IndicatorWidget widget={widget} latestData={latestData} />;
      case 'CONTROL_BUTTON':
        return <ControlButtonWidget widget={widget} />;
      case 'MAP':
        return <MapWidget widget={widget} latestData={latestData} />;
      case 'TABLE':
        return <TableWidget widget={widget} latestData={latestData} />;
      default:
        return (
          <div className="text-gray-400">
            Unsupported widget type: {widget.type}
          </div>
        );
    }
  };

  return (
    <WidgetContainer widget={widget} onDelete={onDelete} onEdit={onEdit}>
      {renderWidgetContent()}
    </WidgetContainer>
  );
};
