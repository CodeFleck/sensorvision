package org.sensorvision.repository;

import org.sensorvision.model.FunctionSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FunctionSecretRepository extends JpaRepository<FunctionSecret, Long> {

    List<FunctionSecret> findByFunctionId(Long functionId);

    Optional<FunctionSecret> findByFunctionIdAndSecretKey(Long functionId, String secretKey);

    boolean existsByFunctionIdAndSecretKey(Long functionId, String secretKey);

    void deleteByFunctionIdAndSecretKey(Long functionId, String secretKey);
}
