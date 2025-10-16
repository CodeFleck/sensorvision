package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "geofences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private GeofenceShape shape = GeofenceShape.CIRCLE;

    @Column(name = "center_latitude", precision = 10, scale = 8)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 11, scale = 8)
    private BigDecimal centerLongitude;

    @Column(name = "radius_meters", precision = 10, scale = 2)
    private BigDecimal radiusMeters;

    @Column(name = "polygon_coordinates", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode polygonCoordinates;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "geofence", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GeofenceAssignment> assignments = new ArrayList<>();

    public enum GeofenceShape {
        CIRCLE,
        POLYGON,
        RECTANGLE
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if a point is inside this geofence
     */
    public boolean containsPoint(BigDecimal latitude, BigDecimal longitude) {
        if (!enabled) {
            return false;
        }

        switch (shape) {
            case CIRCLE:
                return isPointInCircle(latitude, longitude);
            case POLYGON:
                return isPointInPolygon(latitude, longitude);
            case RECTANGLE:
                return isPointInRectangle(latitude, longitude);
            default:
                return false;
        }
    }

    private boolean isPointInCircle(BigDecimal lat, BigDecimal lon) {
        if (centerLatitude == null || centerLongitude == null || radiusMeters == null) {
            return false;
        }

        double distance = calculateDistance(
                centerLatitude.doubleValue(),
                centerLongitude.doubleValue(),
                lat.doubleValue(),
                lon.doubleValue()
        );

        return distance <= radiusMeters.doubleValue();
    }

    private boolean isPointInPolygon(BigDecimal lat, BigDecimal lon) {
        // Ray casting algorithm for point-in-polygon test
        if (polygonCoordinates == null || !polygonCoordinates.isArray()) {
            return false;
        }

        double x = lat.doubleValue();
        double y = lon.doubleValue();
        boolean inside = false;

        int n = polygonCoordinates.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygonCoordinates.get(i).get(0).asDouble();
            double yi = polygonCoordinates.get(i).get(1).asDouble();
            double xj = polygonCoordinates.get(j).get(0).asDouble();
            double yj = polygonCoordinates.get(j).get(1).asDouble();

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }

        return inside;
    }

    private boolean isPointInRectangle(BigDecimal lat, BigDecimal lon) {
        // Rectangle is stored as polygon with 4 corners
        return isPointInPolygon(lat, lon);
    }

    /**
     * Calculate distance between two points using Haversine formula (in meters)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
