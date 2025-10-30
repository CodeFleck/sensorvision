package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.IssueCommentRequest;
import org.sensorvision.model.*;
import org.sensorvision.repository.IssueCommentRepository;
import org.sensorvision.repository.IssueSubmissionRepository;
import org.sensorvision.security.SecurityUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for email notification preferences and async functionality
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationPreferencesTest {

    @Mock
    private IssueCommentRepository commentRepository;

    @Mock
    private IssueSubmissionRepository issueRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Captor
    private ArgumentCaptor<IssueComment> commentCaptor;

    private IssueCommentService commentService;

    private Organization testOrganization;
    private User adminUser;
    private User ticketOwnerWithEmailsEnabled;
    private User ticketOwnerWithEmailsDisabled;
    private IssueSubmission testIssue;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
            .id(1L)
            .name("Test Organization")
            .build();

        adminUser = User.builder()
            .id(2L)
            .username("admin")
            .email("admin@test.com")
            .firstName("Admin")
            .lastName("User")
            .organization(testOrganization)
            .emailNotificationsEnabled(true)
            .build();

        ticketOwnerWithEmailsEnabled = User.builder()
            .id(3L)
            .username("user1")
            .email("user1@test.com")
            .firstName("User")
            .lastName("One")
            .organization(testOrganization)
            .emailNotificationsEnabled(true)
            .build();

        ticketOwnerWithEmailsDisabled = User.builder()
            .id(4L)
            .username("user2")
            .email("user2@test.com")
            .firstName("User")
            .lastName("Two")
            .organization(testOrganization)
            .emailNotificationsEnabled(false)
            .build();

        testIssue = new IssueSubmission();
        testIssue.setId(100L);
        testIssue.setTitle("Test Issue");
        testIssue.setDescription("Test Description");
        testIssue.setCategory(IssueCategory.BUG);
        testIssue.setSeverity(IssueSeverity.MEDIUM);
        testIssue.setStatus(IssueStatus.SUBMITTED);
        testIssue.setOrganization(testOrganization);

        commentService = new IssueCommentService(
            commentRepository,
            issueRepository,
            securityUtils,
            emailNotificationService
        );

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
    }

    @Test
    void addAdminComment_shouldSendEmail_whenUserHasEmailsEnabled() {
        // Given
        testIssue.setUser(ticketOwnerWithEmailsEnabled);

        IssueCommentRequest request = new IssueCommentRequest("Support reply message", false);
        IssueComment savedComment = new IssueComment();
        savedComment.setId(1L);
        savedComment.setIssue(testIssue);
        savedComment.setAuthor(adminUser);
        savedComment.setMessage(request.message());
        savedComment.setInternal(false);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(savedComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(100L, request);

        // Then
        verify(emailNotificationService).sendTicketReplyEmail(
            eq(testIssue),
            eq("Support reply message"),
            eq("user1@test.com")
        );
    }

    @Test
    void addAdminComment_shouldNotSendEmail_whenUserHasEmailsDisabled() {
        // Given
        testIssue.setUser(ticketOwnerWithEmailsDisabled);

        IssueCommentRequest request = new IssueCommentRequest("Support reply message", false);
        IssueComment savedComment = new IssueComment();
        savedComment.setId(1L);
        savedComment.setIssue(testIssue);
        savedComment.setAuthor(adminUser);
        savedComment.setMessage(request.message());
        savedComment.setInternal(false);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(savedComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(100L, request);

        // Then
        verify(emailNotificationService, never()).sendTicketReplyEmail(any(), any(), any());
    }

    @Test
    void addAdminComment_shouldNotSendEmail_whenCommentIsInternal() {
        // Given
        testIssue.setUser(ticketOwnerWithEmailsEnabled);

        IssueCommentRequest request = new IssueCommentRequest("Internal admin note", true);
        IssueComment savedComment = new IssueComment();
        savedComment.setId(1L);
        savedComment.setIssue(testIssue);
        savedComment.setAuthor(adminUser);
        savedComment.setMessage(request.message());
        savedComment.setInternal(true);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(savedComment);

        // When
        commentService.addAdminComment(100L, request);

        // Then
        verify(emailNotificationService, never()).sendTicketReplyEmail(any(), any(), any());
    }

    @Test
    void addAdminComment_shouldHandleEmailFailureGracefully() {
        // Given
        testIssue.setUser(ticketOwnerWithEmailsEnabled);

        IssueCommentRequest request = new IssueCommentRequest("Support reply message", false);
        IssueComment savedComment = new IssueComment();
        savedComment.setId(1L);
        savedComment.setIssue(testIssue);
        savedComment.setAuthor(adminUser);
        savedComment.setMessage(request.message());
        savedComment.setInternal(false);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(savedComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // Simulate email service throwing exception
        doThrow(new RuntimeException("Email service unavailable"))
            .when(emailNotificationService).sendTicketReplyEmail(any(), any(), any());

        // When - should not throw exception
        var response = commentService.addAdminComment(100L, request);

        // Then - comment should still be created
        assertThat(response).isNotNull();
        assertThat(response.message()).isEqualTo("Support reply message");
    }

    @Test
    void addAdminComment_shouldNotSendEmail_whenUserEmailNotificationsIsNull() {
        // Given - user with null emailNotificationsEnabled (shouldn't happen, but defensive)
        User userWithNullPref = User.builder()
            .id(5L)
            .username("user3")
            .email("user3@test.com")
            .organization(testOrganization)
            .emailNotificationsEnabled(null)
            .build();

        testIssue.setUser(userWithNullPref);

        IssueCommentRequest request = new IssueCommentRequest("Support reply message", false);
        IssueComment savedComment = new IssueComment();
        savedComment.setId(1L);
        savedComment.setIssue(testIssue);
        savedComment.setAuthor(adminUser);
        savedComment.setMessage(request.message());
        savedComment.setInternal(false);

        when(issueRepository.findById(100L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(savedComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(100L, request);

        // Then - should not send email when preference is null (defensive coding)
        verify(emailNotificationService, never()).sendTicketReplyEmail(any(), any(), any());
    }
}
