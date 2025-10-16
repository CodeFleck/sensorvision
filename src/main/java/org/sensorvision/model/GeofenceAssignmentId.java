package org.sensorvision.model;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GeofenceAssignmentId implements Serializable {
    private Long geofence;
    private UUID device;
}
