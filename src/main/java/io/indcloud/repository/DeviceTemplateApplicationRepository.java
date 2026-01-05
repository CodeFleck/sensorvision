package io.indcloud.repository;

import io.indcloud.model.DeviceTemplateApplication;
import io.indcloud.model.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTemplateApplicationRepository extends JpaRepository<DeviceTemplateApplication, Long> {

    List<DeviceTemplateApplication> findByDeviceId(UUID deviceId);

    Optional<DeviceTemplateApplication> findByDeviceIdAndDeviceType(UUID deviceId, DeviceType deviceType);

    boolean existsByDeviceIdAndDeviceType(UUID deviceId, DeviceType deviceType);

    List<DeviceTemplateApplication> findByDeviceType(DeviceType deviceType);

    long countByDeviceType(DeviceType deviceType);
}
