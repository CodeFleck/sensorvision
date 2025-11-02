#!/bin/bash

# Script to update remaining widgets with deviceId prop

WIDGETS=(
  "PieChartWidget"
  "AreaChartWidget"
  "ScatterChartWidget"
  "ControlButtonWidget"
  "MapWidget"
  "TableWidget"
)

for widget in "${WIDGETS[@]}"; do
  file="frontend/src/components/widgets/${widget}.tsx"
  
  echo "Updating $widget..."
  
  # Check if file exists
  if [ ! -f "$file" ]; then
    echo "  ERROR: File not found: $file"
    continue
  fi
  
  # Create backup
  cp "$file" "$file.bak"
  
  # Add deviceId to interface (after widget: Widget, before latestData)
  sed -i 's/\(widget: Widget;\)/\1\n  deviceId?: string;/' "$file"
  
  # Add deviceId to component parameters
  sed -i "s/({ widget, latestData })/({ widget, deviceId, latestData })/" "$file"
  sed -i "s/({ widget })/({ widget, deviceId })/" "$file"
  
  # Replace widget.deviceId with deviceId
  sed -i 's/widget\.deviceId/deviceId/g' "$file"
  
  # Fix useEffect dependency arrays (this is widget-specific and needs manual review)
  echo "  ⚠️  Please manually review useEffect dependencies in $file"
  
  echo "  ✓ Updated $widget (backup saved as ${file}.bak)"
done

echo "Done! Please review the changes and manually fix useEffect dependency arrays."
