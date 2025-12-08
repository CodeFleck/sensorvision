package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.EmailTemplateRequest;
import org.sensorvision.dto.EmailTemplateResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.EmailTemplate;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.EmailTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

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
            String replacement = value != null ? value.toString() : "{{" + variableName + "}}";
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
     * Generate alert notification email body with Dark OLED Luxury design
     * Matches the premium UI aesthetic with teal accents and glassmorphism
     */
    public String generateAlertNotificationEmail(String alertType, String deviceName, String message, String severity, String dashboardLink) {
        String severityColor = switch (severity.toUpperCase()) {
            case "CRITICAL" -> "#ef4444";
            case "HIGH" -> "#f97316";
            case "MEDIUM" -> "#eab308";
            default -> "#14b8a6";
        };

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Alert - SensorVision</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #000000;">
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #000000;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; max-width: 600px;">
                                    <!-- Header with gradient -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #0a0a0a 0%%, #111111 100%%); border: 1px solid rgba(20, 184, 166, 0.2); border-bottom: none; border-radius: 16px 16px 0 0; padding: 40px 40px 30px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="text-align: center;">
                                                        <!-- Logo -->
                                                        <div style="display: inline-block; background: linear-gradient(135deg, rgba(20, 184, 166, 0.2), rgba(20, 184, 166, 0.1)); border-radius: 12px; padding: 12px 20px; margin-bottom: 20px;">
                                                            <span style="font-size: 24px; font-weight: 700; color: #14b8a6; letter-spacing: -0.5px;">SensorVision</span>
                                                        </div>
                                                        <h1 style="margin: 0 0 8px; font-size: 28px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">Alert Triggered</h1>
                                                        <p style="margin: 0; font-size: 14px; color: #6b7280;">Real-time IoT Monitoring</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Alert Badge -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 0 40px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="text-align: center; padding: 20px 0;">
                                                        <span style="display: inline-block; background: %s; color: #ffffff; font-size: 12px; font-weight: 600; padding: 6px 16px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Content Card -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 0 40px 30px;">
                                            <!-- Glass Card -->
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(20, 20, 20, 0.8); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 12px;">
                                                <tr>
                                                    <td style="padding: 24px;">
                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                            <!-- Alert Type -->
                                                            <tr>
                                                                <td style="padding-bottom: 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.06);">
                                                                    <p style="margin: 0 0 4px; font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px;">Alert Type</p>
                                                                    <p style="margin: 0; font-size: 16px; color: #ffffff; font-weight: 600;">%s</p>
                                                                </td>
                                                            </tr>
                                                            <!-- Device -->
                                                            <tr>
                                                                <td style="padding: 16px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.06);">
                                                                    <p style="margin: 0 0 4px; font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px;">Device</p>
                                                                    <p style="margin: 0; font-size: 16px; color: #14b8a6; font-weight: 500;">%s</p>
                                                                </td>
                                                            </tr>
                                                            <!-- Message -->
                                                            <tr>
                                                                <td style="padding-top: 16px;">
                                                                    <p style="margin: 0 0 4px; font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px;">Message</p>
                                                                    <p style="margin: 0; font-size: 15px; color: #d1d5db; line-height: 1.6;">%s</p>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- CTA Button -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 10px 40px 40px; text-align: center;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #14b8a6 0%%, #0d9488 100%%); color: #ffffff; font-size: 14px; font-weight: 600; text-decoration: none; padding: 14px 32px; border-radius: 8px; box-shadow: 0 4px 14px rgba(20, 184, 166, 0.3);">View Dashboard</a>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #050505 0%%, #0a0a0a 100%%); border: 1px solid rgba(20, 184, 166, 0.1); border-top: none; border-radius: 0 0 16px 16px; padding: 30px 40px; text-align: center;">
                                            <p style="margin: 0 0 8px; font-size: 14px; color: #6b7280;">This is an automated alert from SensorVision.</p>
                                            <p style="margin: 0; font-size: 12px; color: #4b5563;">© 2025 SensorVision. All rights reserved.</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, severityColor, severity, alertType, deviceName, message, dashboardLink);
    }

    /**
     * Simplified alert notification for backwards compatibility
     */
    public String generateAlertNotificationEmail(Object alert) {
        return generateAlertNotificationEmail(
            "System Alert",
            "Unknown Device",
            "An alert has been triggered in your system.",
            "MEDIUM",
            "http://35.88.65.186.nip.io:8080/dashboard"
        );
    }

    /**
     * Generate password reset email with Dark OLED Luxury design
     * Features true black background, teal accents, and glassmorphism matching UI
     */
    public String generatePasswordResetEmail(String resetLink) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset Your Password - SensorVision</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #000000;">
                    <!-- Wrapper table for dark background -->
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #000000;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <!-- Main container -->
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; max-width: 600px;">

                                    <!-- Header Section with Gradient Border -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #0a0a0a 0%%, #111111 100%%); border: 1px solid rgba(20, 184, 166, 0.2); border-bottom: none; border-radius: 16px 16px 0 0; padding: 48px 40px 32px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="text-align: center;">
                                                        <!-- Logo with glow effect -->
                                                        <div style="display: inline-block; background: linear-gradient(135deg, rgba(20, 184, 166, 0.2), rgba(20, 184, 166, 0.1)); border-radius: 12px; padding: 12px 24px; margin-bottom: 24px;">
                                                            <span style="font-size: 28px; font-weight: 700; color: #14b8a6; letter-spacing: -0.5px;">SensorVision</span>
                                                        </div>
                                                        <h1 style="margin: 0 0 8px; font-size: 32px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">Reset Your Password</h1>
                                                        <p style="margin: 0; font-size: 15px; color: #6b7280;">Secure access to your IoT dashboard</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Main Content -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 32px 40px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td>
                                                        <p style="margin: 0 0 24px; font-size: 16px; color: #d1d5db; line-height: 1.7;">
                                                            We received a request to reset the password for your SensorVision account. Click the button below to create a new password.
                                                        </p>
                                                    </td>
                                                </tr>

                                                <!-- CTA Button -->
                                                <tr>
                                                    <td style="padding: 16px 0 32px; text-align: center;">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #14b8a6 0%%, #0d9488 100%%); color: #ffffff; font-size: 16px; font-weight: 600; text-decoration: none; padding: 16px 40px; border-radius: 10px; box-shadow: 0 4px 20px rgba(20, 184, 166, 0.35), 0 0 40px rgba(20, 184, 166, 0.1); letter-spacing: 0.3px;">Reset Password</a>
                                                    </td>
                                                </tr>

                                                <!-- Expiry Notice -->
                                                <tr>
                                                    <td>
                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(20, 184, 166, 0.08); border: 1px solid rgba(20, 184, 166, 0.2); border-radius: 10px;">
                                                            <tr>
                                                                <td style="padding: 16px 20px; text-align: center;">
                                                                    <p style="margin: 0; font-size: 14px; color: #9ca3af;">
                                                                        This link expires in <span style="color: #14b8a6; font-weight: 600;">1 hour</span> for security
                                                                    </p>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Security Notice -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 0 40px 32px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(251, 191, 36, 0.08); border: 1px solid rgba(251, 191, 36, 0.2); border-radius: 10px;">
                                                <tr>
                                                    <td style="padding: 20px;">
                                                        <p style="margin: 0 0 8px; font-size: 13px; font-weight: 600; color: #fbbf24; text-transform: uppercase; letter-spacing: 0.5px;">Security Notice</p>
                                                        <p style="margin: 0; font-size: 14px; color: #9ca3af; line-height: 1.6;">
                                                            If you didn't request this password reset, please ignore this email. Your account remains secure.
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Alternative Link -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 0 40px 32px;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(255, 255, 255, 0.02); border: 1px solid rgba(255, 255, 255, 0.06); border-radius: 10px;">
                                                <tr>
                                                    <td style="padding: 16px 20px;">
                                                        <p style="margin: 0 0 8px; font-size: 12px; color: #6b7280;">Button not working? Copy this link:</p>
                                                        <p style="margin: 0; font-size: 12px; color: #14b8a6; word-break: break-all; font-family: 'Courier New', monospace; background: rgba(0, 0, 0, 0.3); padding: 10px; border-radius: 6px; border: 1px solid rgba(20, 184, 166, 0.15);">%s</p>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #050505 0%%, #0a0a0a 100%%); border: 1px solid rgba(20, 184, 166, 0.1); border-top: none; border-radius: 0 0 16px 16px; padding: 32px 40px; text-align: center;">
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="padding-bottom: 16px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);">
                                                        <p style="margin: 0; font-size: 18px; font-weight: 600; color: #14b8a6;">SensorVision</p>
                                                        <p style="margin: 4px 0 0; font-size: 13px; color: #6b7280;">Real-time IoT Monitoring & Analytics</p>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding-top: 16px;">
                                                        <p style="margin: 0; font-size: 12px; color: #4b5563;">This is an automated security message.</p>
                                                        <p style="margin: 8px 0 0; font-size: 12px; color: #374151;">© 2025 SensorVision. All rights reserved.</p>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding-top: 20px;">
                                                        <a href="https://github.com/CodeFleck/sensorvision" style="color: #6b7280; text-decoration: none; font-size: 12px; margin: 0 12px;">GitHub</a>
                                                        <a href="#" style="color: #6b7280; text-decoration: none; font-size: 12px; margin: 0 12px;">Documentation</a>
                                                        <a href="#" style="color: #6b7280; text-decoration: none; font-size: 12px; margin: 0 12px;">Privacy</a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, resetLink, resetLink);
    }

    /**
     * Generate welcome email for new users with Dark OLED Luxury design
     */
    public String generateWelcomeEmail(String username, String dashboardLink) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Welcome to SensorVision</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #000000;">
                    <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #000000;">
                        <tr>
                            <td style="padding: 40px 20px;">
                                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="margin: 0 auto; max-width: 600px;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #0a0a0a 0%%, #111111 100%%); border: 1px solid rgba(20, 184, 166, 0.2); border-bottom: none; border-radius: 16px 16px 0 0; padding: 48px 40px 32px; text-align: center;">
                                            <div style="display: inline-block; background: linear-gradient(135deg, rgba(20, 184, 166, 0.2), rgba(20, 184, 166, 0.1)); border-radius: 12px; padding: 12px 24px; margin-bottom: 24px;">
                                                <span style="font-size: 28px; font-weight: 700; color: #14b8a6; letter-spacing: -0.5px;">SensorVision</span>
                                            </div>
                                            <h1 style="margin: 0 0 8px; font-size: 32px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">Welcome, %s!</h1>
                                            <p style="margin: 0; font-size: 15px; color: #6b7280;">Your IoT monitoring journey begins now</p>
                                        </td>
                                    </tr>

                                    <!-- Content -->
                                    <tr>
                                        <td style="background: #0a0a0a; border-left: 1px solid rgba(20, 184, 166, 0.2); border-right: 1px solid rgba(20, 184, 166, 0.2); padding: 32px 40px;">
                                            <p style="margin: 0 0 24px; font-size: 16px; color: #d1d5db; line-height: 1.7;">
                                                Thank you for joining SensorVision. You now have access to powerful real-time IoT monitoring and analytics.
                                            </p>

                                            <!-- Features Grid -->
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="margin-bottom: 24px;">
                                                <tr>
                                                    <td style="width: 50%%; padding-right: 8px; vertical-align: top;">
                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(20, 184, 166, 0.08); border: 1px solid rgba(20, 184, 166, 0.15); border-radius: 10px;">
                                                            <tr>
                                                                <td style="padding: 20px;">
                                                                    <p style="margin: 0 0 4px; font-size: 14px; font-weight: 600; color: #14b8a6;">Real-time Data</p>
                                                                    <p style="margin: 0; font-size: 13px; color: #9ca3af;">Live telemetry streaming</p>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                    <td style="width: 50%%; padding-left: 8px; vertical-align: top;">
                                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background: rgba(59, 130, 246, 0.08); border: 1px solid rgba(59, 130, 246, 0.15); border-radius: 10px;">
                                                            <tr>
                                                                <td style="padding: 20px;">
                                                                    <p style="margin: 0 0 4px; font-size: 14px; font-weight: 600; color: #3b82f6;">Smart Alerts</p>
                                                                    <p style="margin: 0; font-size: 13px; color: #9ca3af;">Configurable notifications</p>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- CTA -->
                                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                                <tr>
                                                    <td style="text-align: center; padding: 16px 0;">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #14b8a6 0%%, #0d9488 100%%); color: #ffffff; font-size: 16px; font-weight: 600; text-decoration: none; padding: 16px 40px; border-radius: 10px; box-shadow: 0 4px 20px rgba(20, 184, 166, 0.35);">Go to Dashboard</a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #050505 0%%, #0a0a0a 100%%); border: 1px solid rgba(20, 184, 166, 0.1); border-top: none; border-radius: 0 0 16px 16px; padding: 32px 40px; text-align: center;">
                                            <p style="margin: 0; font-size: 18px; font-weight: 600; color: #14b8a6;">SensorVision</p>
                                            <p style="margin: 4px 0 16px; font-size: 13px; color: #6b7280;">Real-time IoT Monitoring & Analytics</p>
                                            <p style="margin: 0; font-size: 12px; color: #374151;">© 2025 SensorVision. All rights reserved.</p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, username, dashboardLink);
    }
}
