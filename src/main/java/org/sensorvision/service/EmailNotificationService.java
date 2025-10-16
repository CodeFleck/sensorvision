package org.sensorvision.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.ReportExecution;
import org.sensorvision.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@sensorvision.com}")
    private String fromEmail;

    @Autowired(required = false)
    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send email notification for an alert
     *
     * Note: This is a stub implementation. In production, you would:
     * 1. Add spring-boot-starter-mail dependency
     * 2. Configure SMTP settings in application.properties
     * 3. Use JavaMailSender to send actual emails
     * 4. Implement HTML email templates
     */
    public boolean sendAlertEmail(User user, Alert alert, String emailAddress) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent email to: {}", emailAddress);
            return false;
        }

        try {
            String subject = generateEmailSubject(alert);
            String body = generateEmailBody(alert);

            log.info("Sending email notification:");
            log.info("  To: {}", emailAddress);
            log.info("  From: {}", fromEmail);
            log.info("  Subject: {}", subject);

            if (mailSender != null) {
                // Send actual email
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(emailAddress);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml
                mailSender.send(message);
                log.info("Email sent successfully to {}", emailAddress);
            } else {
                // Fallback: Log only (when mail sender is not configured)
                log.info("JavaMailSender not configured. Email content:");
                log.info("  Body: {}", body);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send email notification to {}", emailAddress, e);
            return false;
        }
    }

    private String generateEmailSubject(Alert alert) {
        return String.format("[SensorVision] %s Alert: %s",
                alert.getSeverity(),
                alert.getRule().getName());
    }

    private String generateEmailBody(Alert alert) {
        return String.format("""
                <html>
                <body>
                    <h2>Alert Notification</h2>
                    <p><strong>Rule:</strong> %s</p>
                    <p><strong>Device:</strong> %s</p>
                    <p><strong>Severity:</strong> %s</p>
                    <p><strong>Message:</strong> %s</p>
                    <p><strong>Triggered Value:</strong> %s</p>
                    <p><strong>Time:</strong> %s</p>
                    <hr>
                    <p><small>This is an automated message from SensorVision IoT Platform</small></p>
                </body>
                </html>
                """,
                alert.getRule().getName(),
                alert.getDevice().getName(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getTriggeredValue(),
                alert.getCreatedAt());
    }

    /**
     * Send scheduled report via email with attachment
     */
    public boolean sendReportEmail(String recipientEmail, String reportName, String reportDescription,
                                    String fileName, byte[] reportData, ReportExecution execution) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent report to: {}", recipientEmail);
            return false;
        }

        try {
            String subject = String.format("[SensorVision] Scheduled Report: %s", reportName);
            String body = generateReportEmailBody(reportName, reportDescription, execution);

            log.info("Sending report email to: {}", recipientEmail);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml

                // Attach the report file
                helper.addAttachment(fileName, new ByteArrayResource(reportData));

                mailSender.send(message);
                log.info("Report email sent successfully to {} with attachment {}", recipientEmail, fileName);
            } else {
                log.info("JavaMailSender not configured. Report email would be sent to: {}", recipientEmail);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send report email to {}", recipientEmail, e);
            return false;
        }
    }

    private String generateReportEmailBody(String reportName, String reportDescription,
                                             ReportExecution execution) {
        return String.format("""
                <html>
                <body>
                    <h2>Scheduled Report: %s</h2>
                    <p>%s</p>
                    <h3>Report Details:</h3>
                    <ul>
                        <li><strong>Generated At:</strong> %s</li>
                        <li><strong>Status:</strong> %s</li>
                        <li><strong>Record Count:</strong> %s</li>
                        <li><strong>File Size:</strong> %s KB</li>
                    </ul>
                    <p>The report is attached to this email.</p>
                    <hr>
                    <p><small>This is an automated message from SensorVision IoT Platform</small></p>
                </body>
                </html>
                """,
                reportName,
                reportDescription != null ? reportDescription : "No description provided",
                execution.getStartedAt(),
                execution.getStatus(),
                execution.getRecordCount() != null ? execution.getRecordCount() : "N/A",
                execution.getFileSizeBytes() != null ? String.format("%.2f", execution.getFileSizeBytes() / 1024.0) : "N/A");
    }

    /**
     * Send password reset email with reset link
     */
    public boolean sendPasswordResetEmail(String email, String resetToken) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent password reset email to: {}", email);
            return false;
        }

        try {
            String subject = "[SensorVision] Password Reset Request";
            String resetLink = generatePasswordResetLink(resetToken);
            String body = generatePasswordResetEmailBody(resetLink);

            log.info("Sending password reset email to: {}", email);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml
                mailSender.send(message);
                log.info("Password reset email sent successfully to {}", email);
            } else {
                log.info("JavaMailSender not configured. Password reset link: {}", resetLink);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", email, e);
            return false;
        }
    }

    /**
     * Send email verification email with verification link
     */
    public boolean sendVerificationEmail(String email, String verificationToken) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent verification email to: {}", email);
            return false;
        }

        try {
            String subject = "[SensorVision] Email Verification";
            String verificationLink = generateVerificationLink(verificationToken);
            String body = generateVerificationEmailBody(verificationLink);

            log.info("Sending email verification to: {}", email);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml
                mailSender.send(message);
                log.info("Verification email sent successfully to {}", email);
            } else {
                log.info("JavaMailSender not configured. Verification link: {}", verificationLink);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", email, e);
            return false;
        }
    }

    private String generatePasswordResetLink(String resetToken) {
        // In production, use configured base URL
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3001";
        }
        return String.format("%s/reset-password?token=%s", baseUrl, resetToken);
    }

    private String generateVerificationLink(String verificationToken) {
        // In production, use configured base URL
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:3001";
        }
        return String.format("%s/verify-email?token=%s", baseUrl, verificationToken);
    }

    private String generatePasswordResetEmailBody(String resetLink) {
        return String.format("""
                <html>
                <body>
                    <h2>Password Reset Request</h2>
                    <p>We received a request to reset your password for your SensorVision account.</p>
                    <p>Click the link below to reset your password:</p>
                    <p><a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;">Reset Password</a></p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p>%s</p>
                    <p><strong>This link will expire in 1 hour.</strong></p>
                    <p>If you didn't request a password reset, you can safely ignore this email.</p>
                    <hr>
                    <p><small>This is an automated message from SensorVision IoT Platform</small></p>
                </body>
                </html>
                """, resetLink, resetLink);
    }

    private String generateVerificationEmailBody(String verificationLink) {
        return String.format("""
                <html>
                <body>
                    <h2>Welcome to SensorVision!</h2>
                    <p>Thank you for registering with SensorVision IoT Platform.</p>
                    <p>Please verify your email address by clicking the link below:</p>
                    <p><a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px;">Verify Email</a></p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p>%s</p>
                    <p><strong>This link will expire in 24 hours.</strong></p>
                    <p>If you didn't create an account, you can safely ignore this email.</p>
                    <hr>
                    <p><small>This is an automated message from SensorVision IoT Platform</small></p>
                </body>
                </html>
                """, verificationLink, verificationLink);
    }
}
