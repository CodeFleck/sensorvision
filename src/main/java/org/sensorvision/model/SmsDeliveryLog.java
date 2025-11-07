package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_delivery_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "twilio_sid", length = 50)
    private String twilioSid;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_code", length = 10)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "cost", precision = 10, scale = 4)
    private BigDecimal cost;

    @Column(name = "sent_at")
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
