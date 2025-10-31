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
     * Generate alert notification email body
     * TODO: Integrate with template management system
     */
    public String generateAlertNotificationEmail(Object alert) {
        // Simple implementation - can be enhanced to use custom templates
        return "<html><body><h2>Alert Notification</h2><p>An alert has been triggered in your system.</p></body></html>";
    }

    /**
     * Generate password reset email body with beautiful, modern design
     * TODO: Integrate with template management system
     */
    public String generatePasswordResetEmail(String resetLink) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Reset Your Password - SensorVision</title>
                    <style>
                        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }

                        body {
                            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            line-height: 1.6;
                            color: #1f2937;
                            background-color: #f3f4f6;
                            padding: 20px;
                        }

                        .email-container {
                            max-width: 600px;
                            margin: 0 auto;
                            background-color: #ffffff;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                        }

                        .header {
                            background: linear-gradient(135deg, #0891b2 0%%, #06b6d4 100%%);
                            padding: 40px 30px;
                            text-align: center;
                        }

                        .logo {
                            font-size: 32px;
                            font-weight: 700;
                            color: #ffffff;
                            margin-bottom: 10px;
                            letter-spacing: -0.5px;
                        }

                        .header-subtitle {
                            color: #e0f2fe;
                            font-size: 14px;
                            font-weight: 500;
                        }

                        .icon-container {
                            background-color: rgba(255, 255, 255, 0.2);
                            width: 80px;
                            height: 80px;
                            border-radius: 50%%;
                            margin: 20px auto;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            backdrop-filter: blur(10px);
                        }

                        .icon {
                            font-size: 40px;
                        }

                        .content {
                            padding: 40px 30px;
                        }

                        h1 {
                            font-size: 28px;
                            font-weight: 700;
                            color: #111827;
                            margin-bottom: 16px;
                            text-align: center;
                        }

                        .greeting {
                            font-size: 16px;
                            color: #4b5563;
                            margin-bottom: 24px;
                            text-align: center;
                        }

                        .message {
                            font-size: 15px;
                            color: #6b7280;
                            margin-bottom: 32px;
                            line-height: 1.7;
                        }

                        .button-container {
                            text-align: center;
                            margin: 40px 0;
                        }

                        .reset-button {
                            display: inline-block;
                            padding: 16px 40px;
                            background: linear-gradient(135deg, #0891b2 0%%, #06b6d4 100%%);
                            color: #ffffff !important;
                            text-decoration: none;
                            border-radius: 8px;
                            font-weight: 600;
                            font-size: 16px;
                            transition: transform 0.2s ease, box-shadow 0.2s ease;
                            box-shadow: 0 4px 6px -1px rgba(8, 145, 178, 0.3);
                        }

                        .reset-button:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 10px 15px -3px rgba(8, 145, 178, 0.4);
                        }

                        .security-notice {
                            background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%);
                            border-left: 4px solid #f59e0b;
                            padding: 16px 20px;
                            margin: 32px 0;
                            border-radius: 6px;
                        }

                        .security-notice-title {
                            font-weight: 600;
                            color: #92400e;
                            font-size: 14px;
                            margin-bottom: 8px;
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }

                        .security-notice-text {
                            font-size: 13px;
                            color: #78350f;
                            line-height: 1.6;
                        }

                        .expiry-info {
                            background-color: #f9fafb;
                            border: 1px solid #e5e7eb;
                            padding: 16px;
                            border-radius: 6px;
                            text-align: center;
                            margin: 24px 0;
                        }

                        .expiry-info-text {
                            font-size: 14px;
                            color: #6b7280;
                        }

                        .expiry-time {
                            font-weight: 600;
                            color: #ef4444;
                        }

                        .alternative-link {
                            margin-top: 24px;
                            padding: 16px;
                            background-color: #f9fafb;
                            border-radius: 6px;
                        }

                        .alternative-link-title {
                            font-size: 13px;
                            color: #6b7280;
                            margin-bottom: 8px;
                            font-weight: 500;
                        }

                        .link-text {
                            font-size: 12px;
                            color: #0891b2;
                            word-break: break-all;
                            font-family: 'Courier New', monospace;
                            padding: 8px;
                            background-color: #ffffff;
                            border: 1px solid #e5e7eb;
                            border-radius: 4px;
                        }

                        .divider {
                            height: 1px;
                            background: linear-gradient(to right, transparent, #e5e7eb, transparent);
                            margin: 32px 0;
                        }

                        .help-section {
                            text-align: center;
                            margin-top: 32px;
                        }

                        .help-text {
                            font-size: 14px;
                            color: #6b7280;
                            margin-bottom: 8px;
                        }

                        .support-link {
                            color: #0891b2;
                            text-decoration: none;
                            font-weight: 500;
                        }

                        .footer {
                            background: linear-gradient(135deg, #1e293b 0%%, #334155 100%%);
                            padding: 30px;
                            text-align: center;
                        }

                        .footer-text {
                            color: #cbd5e1;
                            font-size: 13px;
                            margin-bottom: 12px;
                        }

                        .footer-brand {
                            color: #ffffff;
                            font-weight: 600;
                            font-size: 16px;
                            margin-bottom: 8px;
                        }

                        .footer-tagline {
                            color: #94a3b8;
                            font-size: 12px;
                            font-style: italic;
                        }

                        .social-links {
                            margin-top: 20px;
                            padding-top: 20px;
                            border-top: 1px solid rgba(255, 255, 255, 0.1);
                        }

                        .social-links a {
                            color: #94a3b8;
                            text-decoration: none;
                            margin: 0 10px;
                            font-size: 12px;
                        }

                        @media only screen and (max-width: 600px) {
                            .email-container {
                                border-radius: 0;
                            }

                            .content {
                                padding: 30px 20px;
                            }

                            .header {
                                padding: 30px 20px;
                            }

                            h1 {
                                font-size: 24px;
                            }

                            .reset-button {
                                padding: 14px 30px;
                                font-size: 15px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <!-- Header -->
                        <div class="header">
                            <div class="logo">🔐 SensorVision</div>
                            <div class="header-subtitle">IoT Monitoring Platform</div>
                            <div class="icon-container">
                                <div class="icon">🔑</div>
                            </div>
                        </div>

                        <!-- Content -->
                        <div class="content">
                            <h1>Reset Your Password</h1>
                            <p class="greeting">Hello there! 👋</p>

                            <p class="message">
                                We received a request to reset the password for your SensorVision account.
                                If you made this request, click the button below to choose a new password.
                            </p>

                            <!-- Call to Action Button -->
                            <div class="button-container">
                                <a href="%s" class="reset-button">Reset My Password</a>
                            </div>

                            <!-- Expiry Information -->
                            <div class="expiry-info">
                                <p class="expiry-info-text">
                                    ⏱️ This link will expire in <span class="expiry-time">1 hour</span> for security reasons.
                                </p>
                            </div>

                            <!-- Security Notice -->
                            <div class="security-notice">
                                <div class="security-notice-title">
                                    🛡️ Security Notice
                                </div>
                                <p class="security-notice-text">
                                    If you didn't request a password reset, you can safely ignore this email.
                                    Your password will remain unchanged and your account is secure.
                                </p>
                            </div>

                            <!-- Alternative Link -->
                            <div class="alternative-link">
                                <p class="alternative-link-title">Button not working? Copy and paste this link:</p>
                                <p class="link-text">%s</p>
                            </div>

                            <div class="divider"></div>

                            <!-- Help Section -->
                            <div class="help-section">
                                <p class="help-text">Need help or have questions?</p>
                                <a href="mailto:support@sensorvision.com" class="support-link">Contact Support →</a>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div class="footer">
                            <div class="footer-brand">SensorVision</div>
                            <p class="footer-tagline">Real-time IoT Monitoring & Analytics</p>
                            <p class="footer-text">
                                This is an automated security message from SensorVision.<br>
                                © 2025 SensorVision. All rights reserved.
                            </p>
                            <div class="social-links">
                                <a href="https://github.com/CodeFleck/sensorvision">GitHub</a>
                                <a href="#">Documentation</a>
                                <a href="#">Privacy Policy</a>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, resetLink, resetLink);
    }
}
