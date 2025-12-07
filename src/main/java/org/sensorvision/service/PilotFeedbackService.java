package org.sensorvision.service;

import org.sensorvision.config.PilotConfiguration;
import org.sensorvision.controller.PilotController.PilotFeedbackRequest;
import org.sensorvision.model.PilotFeedback;
import org.sensorvision.repository.PilotFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing pilot program feedback collection and analysis
 */
@Service
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(PilotFeedbackService.class);

    @Autowired
    private PilotConfiguration pilotConfig;

    @Autowired
    private PilotFeedbackRepository pilotFeedbackRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private SlackNotificationService slackNotificationService;

    /**
     * Submit feedback for the pilot program
     */
    public void submitFeedback(String userId, String organizationId, PilotFeedbackRequest request) {
        if (!pilotConfig.getSupport().getFeedbackEnabled()) {
            logger.warn("Feedback submission attempted but feedback is disabled");
            return;
        }

        try {
            // Create feedback entity
            PilotFeedback feedback = new PilotFeedback();
            feedback.setUserId(userId);
            feedback.setOrganizationId(organizationId);
            feedback.setCategory(request.getCategory());
            feedback.setRating(request.getRating());
            feedback.setTitle(request.getTitle());
            feedback.setMessage(request.getMessage());
            feedback.setEmail(request.getEmail());
            feedback.setMetadata(request.getMetadata());
            feedback.setSubmittedAt(LocalDateTime.now());
            feedback.setStatus("NEW");

            // Save feedback
            pilotFeedbackRepository.save(feedback);

            logger.info("Pilot feedback submitted: {} rating by user {} in org {}", 
                request.getRating(), userId, organizationId);

            // Send notifications for critical feedback
            if (request.getRating() <= 2 || "bug".equalsIgnoreCase(request.getCategory())) {
                sendCriticalFeedbackNotification(feedback);
            }

            // Send acknowledgment email to user
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                sendFeedbackAcknowledgment(request.getEmail(), feedback);
            }

        } catch (Exception e) {
            logger.error("Error submitting pilot feedback: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to submit feedback", e);
        }
    }

    /**
     * Get feedback summary for administrators
     */
    public Map<String, Object> getFeedbackSummary(int days) {
        logger.info("Generating feedback summary for {} days", days);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PilotFeedback> feedbacks = pilotFeedbackRepository.findBySubmittedAtAfter(since);

        Map<String, Object> summary = new HashMap<>();
        
        // Basic statistics
        summary.put("totalFeedbacks", feedbacks.size());
        summary.put("period", days + " days");
        
        // Rating distribution
        Map<Integer, Long> ratingDistribution = feedbacks.stream()
            .collect(Collectors.groupingBy(
                PilotFeedback::getRating,
                Collectors.counting()
            ));
        summary.put("ratingDistribution", ratingDistribution);
        
        // Average rating
        double avgRating = feedbacks.stream()
            .mapToInt(PilotFeedback::getRating)
            .average()
            .orElse(0.0);
        summary.put("averageRating", Math.round(avgRating * 100.0) / 100.0);
        
        // Category breakdown
        Map<String, Long> categoryBreakdown = feedbacks.stream()
            .collect(Collectors.groupingBy(
                f -> f.getCategory() != null ? f.getCategory() : "uncategorized",
                Collectors.counting()
            ));
        summary.put("categoryBreakdown", categoryBreakdown);
        
        // Organization breakdown
        Map<String, Long> orgBreakdown = feedbacks.stream()
            .collect(Collectors.groupingBy(
                PilotFeedback::getOrganizationId,
                Collectors.counting()
            ));
        summary.put("organizationBreakdown", orgBreakdown);
        
        // Recent feedback (last 10)
        List<Map<String, Object>> recentFeedback = feedbacks.stream()
            .sorted((f1, f2) -> f2.getSubmittedAt().compareTo(f1.getSubmittedAt()))
            .limit(10)
            .map(this::feedbackToMap)
            .collect(Collectors.toList());
        summary.put("recentFeedback", recentFeedback);
        
        // Critical feedback (rating <= 2)
        List<Map<String, Object>> criticalFeedback = feedbacks.stream()
            .filter(f -> f.getRating() <= 2)
            .sorted((f1, f2) -> f2.getSubmittedAt().compareTo(f1.getSubmittedAt()))
            .map(this::feedbackToMap)
            .collect(Collectors.toList());
        summary.put("criticalFeedback", criticalFeedback);
        
        // Net Promoter Score (NPS)
        long promoters = feedbacks.stream().filter(f -> f.getRating() >= 9).count();
        long detractors = feedbacks.stream().filter(f -> f.getRating() <= 6).count();
        long total = feedbacks.size();
        
        double nps = total > 0 ? ((double) (promoters - detractors) / total) * 100 : 0;
        summary.put("netPromoterScore", Math.round(nps * 100.0) / 100.0);
        
        // Sentiment analysis (simplified)
        Map<String, Long> sentiment = analyzeSentiment(feedbacks);
        summary.put("sentiment", sentiment);
        
        return summary;
    }

    /**
     * Get detailed feedback for a specific organization
     */
    public List<Map<String, Object>> getOrganizationFeedback(String organizationId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        return pilotFeedbackRepository.findByOrganizationIdAndSubmittedAtAfter(organizationId, since)
            .stream()
            .map(this::feedbackToMap)
            .collect(Collectors.toList());
    }

    /**
     * Update feedback status (for admin use)
     */
    public void updateFeedbackStatus(Long feedbackId, String status, String adminNotes) {
        Optional<PilotFeedback> feedbackOpt = pilotFeedbackRepository.findById(feedbackId);
        
        if (feedbackOpt.isPresent()) {
            PilotFeedback feedback = feedbackOpt.get();
            feedback.setStatus(status);
            feedback.setAdminNotes(adminNotes);
            feedback.setUpdatedAt(LocalDateTime.now());
            
            pilotFeedbackRepository.save(feedback);
            
            logger.info("Feedback {} status updated to: {}", feedbackId, status);
            
            // Send follow-up email if resolved
            if ("RESOLVED".equals(status) && feedback.getEmail() != null) {
                sendFeedbackResolutionNotification(feedback);
            }
        }
    }

    /**
     * Send critical feedback notification to pilot team
     */
    private void sendCriticalFeedbackNotification(PilotFeedback feedback) {
        try {
            // Email notification
            String subject = String.format("Critical Pilot Feedback - Rating %d", feedback.getRating());
            String body = String.format(
                "Critical feedback received from pilot program:\n\n" +
                "Rating: %d/10\n" +
                "Category: %s\n" +
                "Title: %s\n" +
                "Message: %s\n" +
                "User: %s\n" +
                "Organization: %s\n" +
                "Submitted: %s\n\n" +
                "Please review and respond promptly.",
                feedback.getRating(),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getMessage(),
                feedback.getUserId(),
                feedback.getOrganizationId(),
                feedback.getSubmittedAt()
            );

            emailNotificationService.sendEmail(
                pilotConfig.getSupport().getEmail(),
                subject,
                body
            );

            // Slack notification if configured
            if (pilotConfig.getSupport().getSlackWebhook() != null) {
                String slackMessage = String.format(
                    "🚨 Critical Pilot Feedback Received\n" +
                    "Rating: %d/10 | Category: %s\n" +
                    "Title: %s\n" +
                    "Organization: %s",
                    feedback.getRating(),
                    feedback.getCategory(),
                    feedback.getTitle(),
                    feedback.getOrganizationId()
                );

                slackNotificationService.sendSlackMessage(slackMessage);
            }

        } catch (Exception e) {
            logger.error("Error sending critical feedback notification: {}", e.getMessage());
        }
    }

    /**
     * Send feedback acknowledgment to user
     */
    private void sendFeedbackAcknowledgment(String email, PilotFeedback feedback) {
        try {
            String subject = "Thank you for your SensorVision Pilot feedback";
            String body = String.format(
                "Dear SensorVision Pilot User,\n\n" +
                "Thank you for taking the time to provide feedback on the SensorVision pilot program. " +
                "Your input is invaluable in helping us improve the platform.\n\n" +
                "Feedback Summary:\n" +
                "- Rating: %d/10\n" +
                "- Category: %s\n" +
                "- Title: %s\n" +
                "- Submitted: %s\n\n" +
                "We review all feedback carefully and will follow up if we need additional information. " +
                "If your feedback requires immediate attention, please contact us at %s.\n\n" +
                "Thank you for being part of the SensorVision pilot program!\n\n" +
                "Best regards,\n" +
                "The SensorVision Team",
                feedback.getRating(),
                feedback.getCategory(),
                feedback.getTitle(),
                feedback.getSubmittedAt(),
                pilotConfig.getSupport().getEmail()
            );

            emailNotificationService.sendEmail(email, subject, body);

        } catch (Exception e) {
            logger.error("Error sending feedback acknowledgment: {}", e.getMessage());
        }
    }

    /**
     * Send feedback resolution notification
     */
    private void sendFeedbackResolutionNotification(PilotFeedback feedback) {
        try {
            String subject = "Your SensorVision Pilot feedback has been addressed";
            String body = String.format(
                "Dear SensorVision Pilot User,\n\n" +
                "We wanted to follow up on the feedback you submitted on %s.\n\n" +
                "Original Feedback:\n" +
                "- Title: %s\n" +
                "- Rating: %d/10\n" +
                "- Category: %s\n\n" +
                "Resolution Notes:\n" +
                "%s\n\n" +
                "If you have any questions about this resolution or additional feedback, " +
                "please don't hesitate to contact us at %s.\n\n" +
                "Thank you for helping us improve SensorVision!\n\n" +
                "Best regards,\n" +
                "The SensorVision Team",
                feedback.getSubmittedAt(),
                feedback.getTitle(),
                feedback.getRating(),
                feedback.getCategory(),
                feedback.getAdminNotes() != null ? feedback.getAdminNotes() : "Your feedback has been reviewed and addressed.",
                pilotConfig.getSupport().getEmail()
            );

            emailNotificationService.sendEmail(feedback.getEmail(), subject, body);

        } catch (Exception e) {
            logger.error("Error sending feedback resolution notification: {}", e.getMessage());
        }
    }

    /**
     * Convert feedback entity to map for API responses
     */
    private Map<String, Object> feedbackToMap(PilotFeedback feedback) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", feedback.getId());
        map.put("rating", feedback.getRating());
        map.put("category", feedback.getCategory());
        map.put("title", feedback.getTitle());
        map.put("message", feedback.getMessage());
        map.put("organizationId", feedback.getOrganizationId());
        map.put("submittedAt", feedback.getSubmittedAt());
        map.put("status", feedback.getStatus());
        // Don't include sensitive information like userId or email in API responses
        return map;
    }

    /**
     * Simple sentiment analysis based on keywords
     */
    private Map<String, Long> analyzeSentiment(List<PilotFeedback> feedbacks) {
        long positive = 0;
        long negative = 0;
        long neutral = 0;

        List<String> positiveKeywords = Arrays.asList(
            "great", "excellent", "love", "amazing", "fantastic", "perfect", "awesome", "good", "helpful"
        );
        
        List<String> negativeKeywords = Arrays.asList(
            "terrible", "awful", "hate", "horrible", "bad", "worst", "broken", "useless", "frustrating"
        );

        for (PilotFeedback feedback : feedbacks) {
            String message = (feedback.getMessage() != null ? feedback.getMessage() : "").toLowerCase();
            String title = (feedback.getTitle() != null ? feedback.getTitle() : "").toLowerCase();
            String text = message + " " + title;

            boolean hasPositive = positiveKeywords.stream().anyMatch(text::contains);
            boolean hasNegative = negativeKeywords.stream().anyMatch(text::contains);

            if (hasPositive && !hasNegative) {
                positive++;
            } else if (hasNegative && !hasPositive) {
                negative++;
            } else {
                neutral++;
            }
        }

        Map<String, Long> sentiment = new HashMap<>();
        sentiment.put("positive", positive);
        sentiment.put("negative", negative);
        sentiment.put("neutral", neutral);
        
        return sentiment;
    }
}