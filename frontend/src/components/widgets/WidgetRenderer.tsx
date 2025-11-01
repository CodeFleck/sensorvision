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
  const effectiveDeviceId = widget.useContextDevice && contextDeviceId
    ? contextDeviceId
    : widget.deviceId;

  const renderWidgetContent = () => {
    switch (widget.type) {
      case 'GAUGE':
        return <GaugeWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'METRIC_CARD':
        return <MetricCardWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'LINE_CHART':
        return <LineChartWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'BAR_CHART':
        return <BarChartWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'PIE_CHART':
        return <PieChartWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'AREA_CHART':
        return <AreaChartWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'SCATTER_CHART':
        return <ScatterChartWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'INDICATOR':
        return <IndicatorWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'CONTROL_BUTTON':
        return <ControlButtonWidget widget={widget} deviceId={effectiveDeviceId} />;
      case 'MAP':
        return <MapWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
      case 'TABLE':
        return <TableWidget widget={widget} deviceId={effectiveDeviceId} latestData={latestData} />;
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
