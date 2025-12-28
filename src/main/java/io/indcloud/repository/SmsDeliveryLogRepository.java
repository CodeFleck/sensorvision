package io.indcloud.repository;

import io.indcloud.model.SmsDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SmsDeliveryLog entities.
 * Tracks SMS delivery history, costs, and status.
 */
@Repository
public interface SmsDeliveryLogRepository extends JpaRepository<SmsDeliveryLog, UUID> {

    /**
     * Find all SMS logs for a specific alert
     */
    List<SmsDeliveryLog> findByAlertId(UUID alertId);

    /**
     * Find SMS logs by phone number
     */
    List<SmsDeliveryLog> findByPhoneNumber(String phoneNumber);

    /**
     * Find SMS logs by status
     */
    List<SmsDeliveryLog> findByStatus(String status);

    /**
     * Find SMS logs sent within a date range
     */
    List<SmsDeliveryLog> findBySentAtBetween(Instant start, Instant end);

    /**
     * Find SMS logs for a phone number within a date range
     */
    List<SmsDeliveryLog> findByPhoneNumberAndSentAtBetween(String phoneNumber, Instant start, Instant end);

    /**
     * Count SMS sent within a date range
     */
    long countBySentAtBetween(Instant start, Instant end);

    /**
     * Count SMS sent to a phone number within a date range (for rate limiting)
     */
    long countByPhoneNumberAndSentAtBetween(String phoneNumber, Instant start, Instant end);

    /**
     * Calculate total cost of SMS sent within a date range
     */
    @Query("SELECT COALESCE(SUM(s.cost), 0) FROM SmsDeliveryLog s WHERE s.sentAt BETWEEN :start AND :end")
    BigDecimal sumCostBySentAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find failed SMS deliveries
     */
    List<SmsDeliveryLog> findByStatusAndSentAtBetween(String status, Instant start, Instant end);
}
