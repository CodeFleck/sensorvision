package io.indcloud.repository;

import io.indcloud.model.Device;
import io.indcloud.model.DeviceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicePropertyRepository extends JpaRepository<DeviceProperty, Long> {

    List<DeviceProperty> findByDeviceId(UUID deviceId);

    Optional<DeviceProperty> findByDeviceIdAndKey(UUID deviceId, String key);

    void deleteByDeviceIdAndKey(UUID deviceId, String key);
}
