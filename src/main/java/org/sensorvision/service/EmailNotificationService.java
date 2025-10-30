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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@sensorvision.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:3001}")
    private String appBaseUrl;

    @Autowired(required = false)
    public EmailNotificationService(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
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
        // Use enhanced template service for better email design
        return templateService.generateAlertNotificationEmail(alert);
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
        // Use enhanced template service for better email design
        return templateService.generatePasswordResetEmail(resetLink);
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

    /**
     * Send issue report email to support team with screenshot attachment
     */
    public boolean sendIssueReportEmail(org.sensorvision.model.IssueSubmission issue, String supportEmail) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent issue report to: {}", supportEmail);
            return false;
        }

        try {
            String subject = String.format("[SensorVision] %s Issue: %s",
                issue.getSeverity().getDisplayName(),
                issue.getTitle());

            String body = generateIssueReportEmailBody(issue);

            log.info("Sending issue report email to: {}", supportEmail);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(supportEmail);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml

                // Attach screenshot if available
                if (issue.getScreenshotData() != null && issue.getScreenshotData().length > 0) {
                    String filename = issue.getScreenshotFilename() != null ?
                        issue.getScreenshotFilename() : "screenshot.png";
                    helper.addAttachment(filename, new ByteArrayResource(issue.getScreenshotData()));
                    log.info("Screenshot attached: {} ({} bytes)", filename, issue.getScreenshotData().length);
                }

                mailSender.send(message);
                log.info("Issue report email sent successfully to {}", supportEmail);
            } else {
                log.info("JavaMailSender not configured. Issue report would be sent to: {}", supportEmail);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send issue report email to {}", supportEmail, e);
            return false;
        }
    }

    private String generateIssueReportEmailBody(org.sensorvision.model.IssueSubmission issue) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body style='font-family: Arial, sans-serif;'>");
        body.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        body.append("<h2 style='color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px;'>");
        body.append("New Issue Report Submitted");
        body.append("</h2>");

        // Issue Details
        body.append("<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>");

        body.append("<h3 style='color: #007bff; margin-top: 0;'>").append(issue.getTitle()).append("</h3>");

        body.append("<table style='width: 100%; border-collapse: collapse;'>");
        addTableRow(body, "Category", issue.getCategory().getDisplayName());
        addTableRow(body, "Severity", getSeverityWithColor(issue.getSeverity()));
        addTableRow(body, "Status", issue.getStatus().getDisplayName());
        addTableRow(body, "Submitted By", issue.getUser().getUsername());
        addTableRow(body, "Email", issue.getUser().getEmail());
        addTableRow(body, "Organization", issue.getOrganization().getName());
        addTableRow(body, "Submission Time", issue.getCreatedAt().toString());
        body.append("</table>");

        body.append("</div>");

        // Description
        body.append("<div style='margin: 20px 0;'>");
        body.append("<h3 style='color: #333;'>Description:</h3>");
        body.append("<div style='background-color: #fff; padding: 15px; border-left: 4px solid #007bff;'>");
        body.append("<p style='white-space: pre-wrap; margin: 0;'>").append(escapeHtml(issue.getDescription())).append("</p>");
        body.append("</div>");
        body.append("</div>");

        // Technical Details
        if (issue.getBrowserInfo() != null || issue.getPageUrl() != null ||
            issue.getUserAgent() != null || issue.getScreenResolution() != null) {
            body.append("<div style='margin: 20px 0;'>");
            body.append("<h3 style='color: #333;'>Technical Details:</h3>");
            body.append("<table style='width: 100%; border-collapse: collapse; background-color: #f8f9fa;'>");

            if (issue.getPageUrl() != null) {
                addTableRow(body, "Page URL", issue.getPageUrl());
            }
            if (issue.getBrowserInfo() != null) {
                addTableRow(body, "Browser", issue.getBrowserInfo());
            }
            if (issue.getScreenResolution() != null) {
                addTableRow(body, "Screen Resolution", issue.getScreenResolution());
            }
            if (issue.getUserAgent() != null) {
                addTableRow(body, "User Agent", "<code style='font-size: 11px;'>" + escapeHtml(issue.getUserAgent()) + "</code>");
            }

            body.append("</table>");
            body.append("</div>");
        }

        // Screenshot note
        if (issue.getScreenshotData() != null && issue.getScreenshotData().length > 0) {
            body.append("<div style='margin: 20px 0; padding: 15px; background-color: #e7f3ff; border-radius: 5px;'>");
            body.append("<p style='margin: 0;'><strong>📷 Screenshot Attached:</strong> ");
            body.append(issue.getScreenshotFilename());
            body.append(" (").append(String.format("%.2f", issue.getScreenshotData().length / 1024.0)).append(" KB)</p>");
            body.append("</div>");
        }

        // Footer
        body.append("<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>");
        body.append("<p style='color: #666; font-size: 12px; text-align: center;'>");
        body.append("This is an automated issue report from SensorVision IoT Platform<br>");
        body.append("Issue ID: #").append(issue.getId());
        body.append("</p>");

        body.append("</div>");
        body.append("</body></html>");

        return body.toString();
    }

    private void addTableRow(StringBuilder body, String label, String value) {
        body.append("<tr>");
        body.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6; font-weight: bold; width: 35%;'>");
        body.append(label).append(":");
        body.append("</td>");
        body.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6;'>");
        body.append(value);
        body.append("</td>");
        body.append("</tr>");
    }

    private String getSeverityWithColor(org.sensorvision.model.IssueSeverity severity) {
        String color = switch (severity) {
            case LOW -> "#28a745";      // Green
            case MEDIUM -> "#ffc107";   // Yellow/Orange
            case HIGH -> "#fd7e14";     // Orange
            case CRITICAL -> "#dc3545"; // Red
        };

        return String.format("<span style='color: %s; font-weight: bold;'>%s</span>",
            color, severity.getDisplayName());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Send email notification to user when support team replies to their ticket
     * This method runs asynchronously to avoid blocking the HTTP request thread
     */
    @Async
    public void sendTicketReplyEmail(org.sensorvision.model.IssueSubmission issue, String replyMessage, String recipientEmail) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Would have sent ticket reply email to: {}", recipientEmail);
            return;
        }

        log.info("[ASYNC] Sending ticket reply email in background thread: {}", Thread.currentThread().getName());

        try {
            String subject = String.format("[SensorVision] Reply on your support ticket #%d", issue.getId());
            String body = generateTicketReplyEmailBody(issue, replyMessage);

            log.info("Sending ticket reply notification to: {}", recipientEmail);

            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(recipientEmail);
                helper.setSubject(subject);
                helper.setText(body, true); // true = isHtml
                mailSender.send(message);
                log.info("Ticket reply email sent successfully to {}", recipientEmail);
            } else {
                log.info("JavaMailSender not configured. Would send ticket reply to: {}", recipientEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send ticket reply email to {}", recipientEmail, e);
            // Exception will be caught by AsyncUncaughtExceptionHandler
        }
    }

    private String generateTicketReplyEmailBody(org.sensorvision.model.IssueSubmission issue, String replyMessage) {
        // Use configured base URL from application properties
        String ticketUrl = String.format("%s/my-tickets", appBaseUrl);

        StringBuilder body = new StringBuilder();
        body.append("<html><body style='font-family: Arial, sans-serif;'>");
        body.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        // Header
        body.append("<div style='background-color: #007bff; color: white; padding: 20px; border-radius: 5px 5px 0 0;'>");
        body.append("<h2 style='margin: 0;'>🎧 Support Team Reply</h2>");
        body.append("</div>");

        // Content
        body.append("<div style='background-color: #f8f9fa; padding: 20px; border: 1px solid #dee2e6; border-top: none;'>");

        body.append("<p style='font-size: 16px; color: #333;'>Hi ").append(escapeHtml(issue.getUser().getFirstName())).append(",</p>");
        body.append("<p style='color: #666;'>Our support team has replied to your ticket:</p>");

        // Ticket Info
        body.append("<div style='background-color: white; padding: 15px; border-left: 4px solid #007bff; margin: 20px 0;'>");
        body.append("<strong style='color: #007bff; font-size: 14px;'>Ticket #").append(issue.getId()).append("</strong><br>");
        body.append("<h3 style='margin: 10px 0; color: #333;'>").append(escapeHtml(issue.getTitle())).append("</h3>");
        body.append("<p style='margin: 5px 0; color: #666; font-size: 13px;'>");
        body.append("<strong>Status:</strong> <span style='color: ");

        // Status color
        String statusColor = switch (issue.getStatus()) {
            case SUBMITTED -> "#007bff";
            case IN_REVIEW -> "#ffc107";
            case RESOLVED -> "#28a745";
            case CLOSED -> "#6c757d";
        };
        body.append(statusColor).append(";'>").append(issue.getStatus().getDisplayName()).append("</span>");
        body.append("</p>");
        body.append("</div>");

        // Reply Message
        body.append("<div style='background-color: #d4edda; padding: 15px; border-left: 4px solid #28a745; margin: 20px 0;'>");
        body.append("<p style='margin: 0 0 5px 0; font-size: 12px; color: #155724; font-weight: bold;'>🎧 Support Team replied:</p>");
        body.append("<p style='white-space: pre-wrap; margin: 0; color: #155724;'>").append(escapeHtml(replyMessage)).append("</p>");
        body.append("</div>");

        // Call to Action
        body.append("<div style='text-align: center; margin: 30px 0;'>");
        body.append("<a href='").append(ticketUrl).append("' style='display: inline-block; padding: 12px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>View Full Conversation</a>");
        body.append("</div>");

        body.append("<p style='color: #666; font-size: 13px; margin-top: 30px;'>You can reply to this ticket by visiting your support dashboard.</p>");

        body.append("</div>");

        // Footer
        body.append("<div style='background-color: #f8f9fa; padding: 15px; text-align: center; border: 1px solid #dee2e6; border-top: none; border-radius: 0 0 5px 5px;'>");
        body.append("<p style='color: #666; font-size: 12px; margin: 5px 0;'>");
        body.append("This is an automated notification from SensorVision Support<br>");
        body.append("You're receiving this because you submitted ticket #").append(issue.getId());
        body.append("</p>");
        body.append("</div>");

        body.append("</div>");
        body.append("</body></html>");

        return body.toString();
    }
}
