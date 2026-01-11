package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.EmailTemplateRequest;
import io.indcloud.dto.EmailTemplateResponse;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.EmailTemplate;
import io.indcloud.model.Organization;
import io.indcloud.model.User;
import io.indcloud.repository.EmailTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    // Allowed URL schemes for links in emails
    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http://", "https://");

    /**
     * Escape HTML special characters to prevent XSS attacks in email content
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Validate and sanitize URL to prevent javascript: and other injection attacks
     */
    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "#";
        }
        String trimmedUrl = url.trim();
        // Only allow http:// and https:// URLs
        for (String scheme : ALLOWED_URL_SCHEMES) {
            if (trimmedUrl.toLowerCase().startsWith(scheme)) {
                return escapeHtml(trimmedUrl);
            }
        }
        // If not a valid URL scheme, return safe fallback
        log.warn("Invalid URL scheme detected and sanitized: {}", trimmedUrl.substring(0, Math.min(trimmedUrl.length(), 20)));
        return "#";
    }

    @Transactional
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request, Organization organization, User user) {
        if (request.isDefault()) {
            emailTemplateRepository.findByOrganizationIdAndTemplateTypeAndIsDefault(
                    organization.getId(),
                    request.templateType(),
                    true
            ).ifPresent(existing -> {
                throw new IllegalStateException(
                        "A default template already exists for type: " + request.templateType()
                );
            });
        }

        EmailTemplate template = EmailTemplate.builder()
                .organization(organization)
                .name(request.name())
                .description(request.description())
                .templateType(request.templateType())
                .subject(request.subject())
                .body(request.body())
                .variables(request.variables() != null ? request.variables() : objectMapper.createArrayNode())
                .isDefault(request.isDefault())
                .active(request.active())
                .createdBy(user)
                .build();

        template = emailTemplateRepository.save(template);
        log.info("Created email template {} for organization {}", template.getId(), organization.getId());

        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public Page<EmailTemplateResponse> getTemplates(
            Organization organization,
            String templateType,
            Boolean active,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EmailTemplate> templates = emailTemplateRepository.findByFilters(
                organization.getId(),
                templateType,
                active,
                pageable
        );

        return templates.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(Long id, Organization organization) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with id: " + id));

        return mapToResponse(template);
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(Long id, EmailTemplateRequest request, Organization organization) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with id: " + id));

        if (request.isDefault() && !template.getIsDefault()) {
            if (emailTemplateRepository.existsByOrganizationIdAndTemplateTypeAndIsDefaultAndIdNot(
                    organization.getId(),
                    request.templateType(),
                    true,
                    id
            )) {
                throw new IllegalStateException(
                        "Another default template exists for type: " + request.templateType()
                );
            }
        }

        template.setName(request.name());
        template.setDescription(request.description());
        template.setTemplateType(request.templateType());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setVariables(request.variables() != null ? request.variables() : objectMapper.createArrayNode());
        template.setIsDefault(request.isDefault());
        template.setActive(request.active());

        template = emailTemplateRepository.save(template);
        log.info("Updated email template {} for organization {}", template.getId(), organization.getId());

        return mapToResponse(template);
    }

    @Transactional
    public void deleteTemplate(Long id, Organization organization) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with id: " + id));

        emailTemplateRepository.delete(template);
        log.info("Deleted email template {} for organization {}", id, organization.getId());
    }

    public String renderTemplate(String templateText, Map<String, Object> variables) {
        if (templateText == null || variables == null) {
            return templateText;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(templateText);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            // HTML-escape all variable values to prevent XSS
            String replacement = value != null ? escapeHtml(value.toString()) : "{{" + variableName + "}}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, String> previewTemplate(Long id, Map<String, Object> sampleData, Organization organization) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(organization.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with id: " + id));

        String renderedSubject = renderTemplate(template.getSubject(), sampleData);
        String renderedBody = renderTemplate(template.getBody(), sampleData);

        return Map.of(
                "subject", renderedSubject,
                "body", renderedBody
        );
    }

    private EmailTemplateResponse mapToResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .subject(template.getSubject())
                .body(template.getBody())
                .variables(template.getVariables())
                .isDefault(template.getIsDefault())
                .active(template.getActive())
                .createdBy(template.getCreatedBy() != null ? template.getCreatedBy().getUsername() : null)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    /**
     * Generate alert notification email with clean, professional design
     */
    public String generateAlertNotificationEmail(String alertType, String deviceName, String message, String severity, String dashboardLink) {
        String safeAlertType = escapeHtml(alertType);
        String safeDeviceName = escapeHtml(deviceName);
        String safeMessage = escapeHtml(message);
        String safeSeverity = escapeHtml(severity);
        String safeDashboardLink = sanitizeUrl(dashboardLink);

        String severityColor = switch (severity != null ? severity.toUpperCase() : "") {
            case "CRITICAL" -> "#dc2626";
            case "HIGH" -> "#ea580c";
            case "MEDIUM" -> "#ca8a04";
            default -> "#0f766e";
        };

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Alert</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f5f5f5;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 0 auto; max-width: 480px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="padding: 40px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td>
                                                        <span style="display: inline-block; background-color: %s; color: #ffffff; font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 4px; text-transform: uppercase;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 16px 0 8px; font-size: 18px; font-weight: 600; color: #111827;">%s</p>
                                            <p style="margin: 0 0 20px; font-size: 14px; color: #6b7280;">Device: %s</p>
                                            <p style="margin: 0 0 24px; font-size: 15px; color: #4b5563; line-height: 1.6;">%s</p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td>
                                                        <a href="%s" style="display: inline-block; background-color: #0f766e; color: #ffffff; font-size: 14px; font-weight: 500; text-decoration: none; padding: 12px 24px; border-radius: 6px;">View dashboard</a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 24px auto 0; max-width: 480px;">
                                    <tr>
                                        <td style="text-align: center;">
                                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">Industrial Cloud</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, severityColor, safeSeverity, safeAlertType, safeDeviceName, safeMessage, safeDashboardLink);
    }

    /**
     * Generate alert notification email from Alert entity
     * Extracts all relevant information for a detailed, professional email
     */
    public String generateAlertNotificationEmail(Object alertObj) {
        if (alertObj instanceof io.indcloud.model.Alert alert) {
            String ruleName = alert.getRule() != null ? alert.getRule().getName() : "Unknown Rule";
            String deviceName = alert.getDevice() != null ? alert.getDevice().getName() : "Unknown Device";
            String deviceExternalId = alert.getDevice() != null ? alert.getDevice().getExternalId() : "";
            String severity = alert.getSeverity() != null ? alert.getSeverity().name() : "MEDIUM";
            String message = alert.getMessage() != null ? alert.getMessage() : "An alert was triggered.";
            String triggeredValue = alert.getTriggeredValue() != null ? alert.getTriggeredValue().toString() : "N/A";
            String variable = alert.getRule() != null ? alert.getRule().getVariable() : "unknown";
            String operator = alert.getRule() != null && alert.getRule().getOperator() != null
                ? alert.getRule().getOperator().getSymbol() : "";
            String threshold = alert.getRule() != null && alert.getRule().getThreshold() != null
                ? alert.getRule().getThreshold().toString() : "N/A";
            String triggeredAt = alert.getTriggeredAt() != null
                ? alert.getTriggeredAt().toString() : java.time.LocalDateTime.now().toString();

            return generateLuxuryAlertEmail(
                ruleName,
                deviceName,
                deviceExternalId,
                severity,
                message,
                variable,
                operator,
                threshold,
                triggeredValue,
                triggeredAt,
                "https://indcloud.io/alerts"
            );
        }

        // Fallback for non-Alert objects
        return generateAlertNotificationEmail(
            "System Alert",
            "Unknown Device",
            "An alert has been triggered in your system.",
            "MEDIUM",
            "https://indcloud.io/dashboard"
        );
    }

    /**
     * Generate luxury-themed alert email with comprehensive details
     */
    private String generateLuxuryAlertEmail(String ruleName, String deviceName, String deviceExternalId,
            String severity, String message, String variable, String operator, String threshold,
            String triggeredValue, String triggeredAt, String dashboardLink) {

        String safeRuleName = escapeHtml(ruleName);
        String safeDeviceName = escapeHtml(deviceName);
        String safeDeviceId = escapeHtml(deviceExternalId);
        String safeSeverity = escapeHtml(severity);
        String safeMessage = escapeHtml(message);
        String safeVariable = escapeHtml(variable);
        String safeOperator = escapeHtml(operator);
        String safeThreshold = escapeHtml(threshold);
        String safeTriggeredValue = escapeHtml(triggeredValue);
        String safeTriggeredAt = escapeHtml(triggeredAt);
        String safeDashboardLink = sanitizeUrl(dashboardLink);

        // Luxury color scheme
        String severityColor = switch (severity != null ? severity.toUpperCase() : "") {
            case "CRITICAL" -> "#991b1b"; // Deep red
            case "HIGH" -> "#c2410c"; // Dark orange
            case "MEDIUM" -> "#a16207"; // Dark amber
            default -> "#166534"; // Dark green
        };

        String severityBgColor = switch (severity != null ? severity.toUpperCase() : "") {
            case "CRITICAL" -> "#fef2f2";
            case "HIGH" -> "#fff7ed";
            case "MEDIUM" -> "#fffbeb";
            default -> "#f0fdf4";
        };

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Alert Notification</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #0a0a0a;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #0a0a0a;">
                    <tr>
                        <td style="padding: 48px 24px;">
                            <!-- Main Card -->
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="560" style="margin: 0 auto; max-width: 560px; background: linear-gradient(145deg, #1a1a1a 0%%, #0f0f0f 100%%); border-radius: 16px; border: 1px solid #2a2a2a; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);">
                                <!-- Header with Logo -->
                                <tr>
                                    <td style="padding: 32px 40px 24px; border-bottom: 1px solid #2a2a2a;">
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                            <tr>
                                                <td>
                                                    <span style="font-size: 20px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">SensorVision</span>
                                                </td>
                                                <td style="text-align: right;">
                                                    <span style="display: inline-block; background-color: %s; color: #ffffff; font-size: 11px; font-weight: 700; padding: 6px 14px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px;">%s</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Alert Content -->
                                <tr>
                                    <td style="padding: 32px 40px;">
                                        <!-- Rule Name -->
                                        <h1 style="margin: 0 0 8px; font-size: 24px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">%s</h1>

                                        <!-- Device Info -->
                                        <p style="margin: 0 0 24px; font-size: 14px; color: #737373;">
                                            Device: <span style="color: #d4d4d4; font-weight: 500;">%s</span>
                                            <span style="color: #525252;"> · </span>
                                            <span style="color: #737373;">ID: %s</span>
                                        </p>

                                        <!-- Alert Message Box -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="margin-bottom: 24px;">
                                            <tr>
                                                <td style="background-color: %s; border-radius: 12px; padding: 20px; border-left: 4px solid %s;">
                                                    <p style="margin: 0; font-size: 15px; color: %s; line-height: 1.6; font-weight: 500;">%s</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- Details Grid -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="margin-bottom: 32px;">
                                            <tr>
                                                <td style="padding: 16px; background-color: #1a1a1a; border-radius: 12px 0 0 0; border: 1px solid #2a2a2a; border-right: none; width: 50%%;">
                                                    <p style="margin: 0 0 4px; font-size: 11px; color: #737373; text-transform: uppercase; letter-spacing: 0.5px;">Variable</p>
                                                    <p style="margin: 0; font-size: 16px; color: #ffffff; font-weight: 600;">%s</p>
                                                </td>
                                                <td style="padding: 16px; background-color: #1a1a1a; border-radius: 0 12px 0 0; border: 1px solid #2a2a2a; width: 50%%;">
                                                    <p style="margin: 0 0 4px; font-size: 11px; color: #737373; text-transform: uppercase; letter-spacing: 0.5px;">Condition</p>
                                                    <p style="margin: 0; font-size: 16px; color: #ffffff; font-weight: 600;">%s %s</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 16px; background-color: #1a1a1a; border-radius: 0 0 0 12px; border: 1px solid #2a2a2a; border-right: none; border-top: none;">
                                                    <p style="margin: 0 0 4px; font-size: 11px; color: #737373; text-transform: uppercase; letter-spacing: 0.5px;">Triggered Value</p>
                                                    <p style="margin: 0; font-size: 20px; color: %s; font-weight: 700;">%s</p>
                                                </td>
                                                <td style="padding: 16px; background-color: #1a1a1a; border-radius: 0 0 12px 0; border: 1px solid #2a2a2a; border-top: none;">
                                                    <p style="margin: 0 0 4px; font-size: 11px; color: #737373; text-transform: uppercase; letter-spacing: 0.5px;">Time</p>
                                                    <p style="margin: 0; font-size: 14px; color: #d4d4d4; font-weight: 500;">%s</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <!-- CTA Button -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                            <tr>
                                                <td>
                                                    <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #0d9488 0%%, #0f766e 100%%); color: #ffffff; font-size: 14px; font-weight: 600; text-decoration: none; padding: 14px 28px; border-radius: 10px; box-shadow: 0 4px 14px rgba(13, 148, 136, 0.4);">View Alert Details</a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 24px 40px; border-top: 1px solid #2a2a2a;">
                                        <p style="margin: 0; font-size: 12px; color: #525252; text-align: center;">
                                            This is an automated alert from your SensorVision monitoring system.
                                            <br>
                                            <a href="https://indcloud.io/settings/notifications" style="color: #737373; text-decoration: underline;">Manage notification preferences</a>
                                        </p>
                                    </td>
                                </tr>
                            </table>

                            <!-- Footer Logo -->
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="560" style="margin: 32px auto 0; max-width: 560px;">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="margin: 0; font-size: 13px; color: #404040;">Powered by <span style="color: #737373; font-weight: 600;">Industrial Cloud</span></p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
            severityColor, safeSeverity,  // Header badge
            safeRuleName,  // Title
            safeDeviceName, safeDeviceId,  // Device info
            severityBgColor, severityColor, severityColor, safeMessage,  // Alert message box
            safeVariable,  // Variable
            safeOperator, safeThreshold,  // Condition
            severityColor, safeTriggeredValue,  // Triggered value
            safeTriggeredAt,  // Time
            safeDashboardLink  // CTA link
        );
    }

    /**
     * Generate password reset email with clean, professional design
     */
    public String generatePasswordResetEmail(String resetLink) {
        String safeResetLink = sanitizeUrl(resetLink);
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset Your Password</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f5f5f5;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 0 auto; max-width: 480px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="margin: 0 0 24px; font-size: 20px; font-weight: 600; color: #111827;">Reset your password</p>
                                            <p style="margin: 0 0 24px; font-size: 15px; color: #4b5563; line-height: 1.6;">
                                                We received a request to reset your password. Click the button below to choose a new one.
                                            </p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="padding: 8px 0 32px;">
                                                        <a href="%s" style="display: inline-block; background-color: #0f766e; color: #ffffff; font-size: 14px; font-weight: 500; text-decoration: none; padding: 12px 24px; border-radius: 6px;">Reset password</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 0 0 16px; font-size: 14px; color: #6b7280; line-height: 1.5;">
                                                This link will expire in 1 hour. If you didn't request this, you can safely ignore this email.
                                            </p>
                                            <p style="margin: 0; padding-top: 16px; border-top: 1px solid #e5e7eb; font-size: 12px; color: #9ca3af;">
                                                Button not working? Copy this link:<br>
                                                <span style="color: #6b7280; word-break: break-all;">%s</span>
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 24px auto 0; max-width: 480px;">
                                    <tr>
                                        <td style="text-align: center;">
                                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">Industrial Cloud</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, safeResetLink, safeResetLink);
    }

    /**
     * Generate welcome email with clean, professional design
     */
    public String generateWelcomeEmail(String username, String dashboardLink) {
        String safeUsername = escapeHtml(username);
        String safeDashboardLink = sanitizeUrl(dashboardLink);
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Welcome to Industrial Cloud</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f5f5f5;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 0 auto; max-width: 480px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="margin: 0 0 24px; font-size: 20px; font-weight: 600; color: #111827;">Welcome, %s</p>
                                            <p style="margin: 0 0 24px; font-size: 15px; color: #4b5563; line-height: 1.6;">
                                                Thanks for signing up for Industrial Cloud. Your account is ready to use.
                                            </p>
                                            <p style="margin: 0 0 24px; font-size: 15px; color: #4b5563; line-height: 1.6;">
                                                Get started by connecting your first device or exploring the dashboard.
                                            </p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="padding: 8px 0 24px;">
                                                        <a href="%s" style="display: inline-block; background-color: #0f766e; color: #ffffff; font-size: 14px; font-weight: 500; text-decoration: none; padding: 12px 24px; border-radius: 6px;">Go to dashboard</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin: 0; font-size: 14px; color: #6b7280;">
                                                Questions? Just reply to this email.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="480" style="margin: 24px auto 0; max-width: 480px;">
                                    <tr>
                                        <td style="text-align: center;">
                                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">Industrial Cloud</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, safeUsername, safeDashboardLink);
    }
}
