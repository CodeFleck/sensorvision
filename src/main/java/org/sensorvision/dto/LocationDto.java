package org.sensorvision.dto;

import java.math.BigDecimal;

/**
 * GPS location data transfer object
 */
public record LocationDto(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal altitude  // Optional, in meters
) {
    public boolean isValid() {
        return latitude != null && longitude != null &&
               latitude.compareTo(BigDecimal.valueOf(-90)) >= 0 &&
               latitude.compareTo(BigDecimal.valueOf(90)) <= 0 &&
               longitude.compareTo(BigDecimal.valueOf(-180)) >= 0 &&
               longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    public double distanceTo(LocationDto other) {
        if (other == null) return Double.MAX_VALUE;

        // Haversine formula for calculating distance between two points on Earth
        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lon1 = Math.toRadians(this.longitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double lon2 = Math.toRadians(other.longitude.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Earth radius in meters
        return 6371000 * c;
    }
}
