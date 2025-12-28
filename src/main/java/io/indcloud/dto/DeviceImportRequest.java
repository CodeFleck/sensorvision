package io.indcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceImportRequest {
    private String externalId;
    private String name;
    private String location;
    private String sensorType;
    private String firmwareVersion;
    private String status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal altitude;
}
