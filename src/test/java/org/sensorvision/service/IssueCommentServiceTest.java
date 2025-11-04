package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.IssueCommentDto;
import org.sensorvision.dto.IssueCommentRequest;
import org.sensorvision.model.IssueComment;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.repository.IssueCommentRepository;
import org.sensorvision.repository.IssueSubmissionRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IssueCommentService.
 * Tests comment CRUD, HTML sanitization, file attachments, and authorization.
 */
@ExtendWith(MockitoExtension.class)
class IssueCommentServiceTest {

    @Mock
    private IssueCommentRepository commentRepository;

    @Mock
    private IssueSubmissionRepository issueRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private IssueCommentService commentService;

    @Captor
    private ArgumentCaptor<IssueComment> commentCaptor;

    @Captor
    private ArgumentCaptor<IssueSubmission> issueCaptor;

    private User testUser;
    private User adminUser;
    private IssueSubmission testIssue;
    private IssueComment testComment;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setEmailNotificationsEnabled(true);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        adminUser.setRoles(Collections.singleton(adminRole));

        testIssue = new IssueSubmission();
        testIssue.setId(1L);
        testIssue.setTitle("Test Issue");
        testIssue.setDescription("Test description");
        testIssue.setUser(testUser);

        testComment = new IssueComment();
        testComment.setId(1L);
        testComment.setIssue(testIssue);
        testComment.setAuthor(adminUser);
        testComment.setMessage("Test comment");
        testComment.setInternal(false);
    }

    // ========== User Comment Tests ==========

    @Test
    void addUserComment_shouldCreateComment_withSanitizedHtml() {
        // Given
        String htmlMessage = "<p>Hello <strong>Support</strong>!</p><script>alert('xss')</script>";
        IssueCommentRequest request = new IssueCommentRequest(htmlMessage, false);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        IssueCommentDto result = commentService.addUserComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        // Script tag should be removed by sanitization
        assertThat(savedComment.getMessage()).contains("<p>Hello <strong>Support</strong>!</p>");
        assertThat(savedComment.getMessage()).doesNotContain("<script>");
        assertThat(savedComment.getMessage()).doesNotContain("alert");
        assertThat(savedComment.isInternal()).isFalse(); // Users cannot create internal comments
        assertThat(savedComment.getAuthor()).isEqualTo(testUser);
        assertThat(savedComment.getIssue()).isEqualTo(testIssue);
    }

    @Test
    void addUserComment_shouldAlwaysSetInternalToFalse() {
        // Given - User tries to set internal=true
        IssueCommentRequest request = new IssueCommentRequest("Test message", true);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addUserComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        // Internal flag should be forced to false
        assertThat(savedComment.isInternal()).isFalse();
    }

    @Test
    void addUserComment_shouldThrowException_whenIssueNotOwnedByUser() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Test", false);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> commentService.addUserComment(1L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Issue not found with id: 1");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addUserComment_withFileAttachment_shouldSaveAttachment() throws IOException {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Comment with file", false);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Test content".getBytes()
        );

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addUserComment(1L, request, file);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getAttachmentFilename()).isEqualTo("test.txt");
        assertThat(savedComment.getAttachmentContentType()).isEqualTo("text/plain");
        assertThat(savedComment.getAttachmentData()).isEqualTo("Test content".getBytes());
        assertThat(savedComment.getAttachmentSizeBytes()).isEqualTo(12L);
    }

    @Test
    void addUserComment_withLargeFile_shouldThrowException() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Comment", false);
        byte[] largeData = new byte[11 * 1024 * 1024]; // 11 MB (exceeds 10 MB limit)
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.bin",
            "application/octet-stream",
            largeData
        );

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));

        // When/Then
        assertThatThrownBy(() -> commentService.addUserComment(1L, request, largeFile))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("File size exceeds maximum limit");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void addUserComment_withInvalidFileType_shouldThrowException() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Comment", false);
        MockMultipartFile executableFile = new MockMultipartFile(
            "file",
            "malware.exe",
            "application/x-msdownload",
            "fake executable".getBytes()
        );

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));

        // When/Then
        assertThatThrownBy(() -> commentService.addUserComment(1L, request, executableFile))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("File type not allowed");

        verify(commentRepository, never()).save(any());
    }

    // ========== Admin Comment Tests ==========

    @Test
    void addAdminComment_shouldCreatePublicComment_andUpdateIssueTimestamp() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Admin reply", false);

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.isInternal()).isFalse();
        assertThat(savedComment.getAuthor()).isEqualTo(adminUser);

        // Verify issue lastPublicReplyAt was updated
        verify(issueRepository).save(issueCaptor.capture());
        IssueSubmission updatedIssue = issueCaptor.getValue();
        assertThat(updatedIssue.getLastPublicReplyAt()).isNotNull();
    }

    @Test
    void addAdminComment_withInternalFlag_shouldNotUpdateIssueTimestamp() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Internal note", true);

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addAdminComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.isInternal()).isTrue();

        // Verify issue was NOT updated (internal comments don't trigger unread badge)
        verify(issueRepository, never()).save(any());
    }

    @Test
    void addAdminComment_shouldSendEmailNotification_whenUserOptedIn() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Admin response", false);
        testUser.setEmailNotificationsEnabled(true);

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(1L, request);

        // Then
        verify(emailNotificationService).sendTicketReplyEmail(
            eq(testIssue),
            eq("Admin response"),
            eq("testuser@example.com")
        );
    }

    @Test
    void addAdminComment_shouldNotSendEmail_whenUserOptedOut() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Admin response", false);
        testUser.setEmailNotificationsEnabled(false);

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);
        when(issueRepository.save(any(IssueSubmission.class))).thenReturn(testIssue);

        // When
        commentService.addAdminComment(1L, request);

        // Then
        verify(emailNotificationService, never()).sendTicketReplyEmail(any(), any(), any());
    }

    @Test
    void addAdminComment_shouldNotSendEmail_whenInternalComment() {
        // Given
        IssueCommentRequest request = new IssueCommentRequest("Internal note", true);
        testUser.setEmailNotificationsEnabled(true);

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addAdminComment(1L, request);

        // Then
        verify(emailNotificationService, never()).sendTicketReplyEmail(any(), any(), any());
    }

    // ========== HTML Sanitization Tests ==========

    @Test
    void htmlSanitization_shouldAllowSafeHtmlTags() {
        // Given
        String safeHtml = "<p>Paragraph</p><strong>Bold</strong><em>Italic</em><ul><li>Item</li></ul><code>code</code>";
        IssueCommentRequest request = new IssueCommentRequest(safeHtml, false);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addUserComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getMessage()).contains("<p>");
        assertThat(savedComment.getMessage()).contains("<strong>");
        assertThat(savedComment.getMessage()).contains("<em>");
        assertThat(savedComment.getMessage()).contains("<ul>");
        assertThat(savedComment.getMessage()).contains("<code>");
    }

    @Test
    void htmlSanitization_shouldRemoveDangerousTags() {
        // Given
        String dangerousHtml = "<script>alert('xss')</script><iframe src='evil.com'></iframe><object data='bad'></object>";
        IssueCommentRequest request = new IssueCommentRequest(dangerousHtml, false);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addUserComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getMessage()).doesNotContain("<script>");
        assertThat(savedComment.getMessage()).doesNotContain("<iframe>");
        assertThat(savedComment.getMessage()).doesNotContain("<object>");
        assertThat(savedComment.getMessage()).doesNotContain("alert");
    }

    @Test
    void htmlSanitization_shouldRemoveDangerousAttributes() {
        // Given
        String htmlWithEvents = "<a href='#' onclick='alert(1)'>Link</a><img src='x' onerror='alert(2)'>";
        IssueCommentRequest request = new IssueCommentRequest(htmlWithEvents, false);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.save(any(IssueComment.class))).thenReturn(testComment);

        // When
        commentService.addUserComment(1L, request);

        // Then
        verify(commentRepository).save(commentCaptor.capture());
        IssueComment savedComment = commentCaptor.getValue();

        assertThat(savedComment.getMessage()).doesNotContain("onclick");
        assertThat(savedComment.getMessage()).doesNotContain("onerror");
        assertThat(savedComment.getMessage()).doesNotContain("alert");
    }

    // ========== Comment Retrieval Tests ==========

    @Test
    void getUserComments_shouldReturnOnlyPublicComments() {
        // Given
        IssueComment publicComment = new IssueComment();
        publicComment.setId(1L);
        publicComment.setInternal(false);
        publicComment.setMessage("Public");
        publicComment.setAuthor(adminUser);
        publicComment.setIssue(testIssue);

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(issueRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testIssue));
        when(commentRepository.findPublicCommentsByIssue(testIssue))
            .thenReturn(Collections.singletonList(publicComment));

        // When
        List<IssueCommentDto> result = commentService.getUserComments(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).internal()).isFalse();
        verify(commentRepository).findPublicCommentsByIssue(testIssue);
    }

    @Test
    void getAdminComments_shouldReturnAllComments() {
        // Given
        IssueComment publicComment = new IssueComment();
        publicComment.setId(1L);
        publicComment.setInternal(false);
        publicComment.setAuthor(testUser);
        publicComment.setMessage("Public");
        publicComment.setIssue(testIssue);

        IssueComment internalComment = new IssueComment();
        internalComment.setId(2L);
        internalComment.setInternal(true);
        internalComment.setAuthor(adminUser);
        internalComment.setMessage("Internal");
        internalComment.setIssue(testIssue);

        when(issueRepository.findById(1L)).thenReturn(Optional.of(testIssue));
        when(commentRepository.findAllCommentsByIssue(testIssue))
            .thenReturn(Arrays.asList(publicComment, internalComment));

        // When
        List<IssueCommentDto> result = commentService.getAdminComments(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(IssueCommentDto::internal).count()).isEqualTo(1);
        verify(commentRepository).findAllCommentsByIssue(testIssue);
    }

    // ========== Attachment Authorization Tests ==========

    @Test
    void getCommentWithAttachment_shouldReturnAttachment_whenUserOwnsTicket() {
        // Given
        testComment.setAttachmentFilename("test.txt");
        testComment.setAttachmentData("data".getBytes());

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When
        IssueComment result = commentService.getCommentWithAttachment(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttachmentFilename()).isEqualTo("test.txt");
    }

    @Test
    void getCommentWithAttachment_shouldThrowException_whenUserDoesNotOwnTicket() {
        // Given
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("otheruser");

        testComment.setAttachmentFilename("test.txt");
        testComment.setAttachmentData("data".getBytes());

        when(securityUtils.getCurrentUser()).thenReturn(otherUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When/Then
        assertThatThrownBy(() -> commentService.getCommentWithAttachment(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("You do not have permission");
    }

    @Test
    void getCommentWithAttachment_shouldThrowException_whenCommentIsInternal() {
        // Given
        testComment.setInternal(true);
        testComment.setAttachmentFilename("test.txt");
        testComment.setAttachmentData("data".getBytes());

        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When/Then
        assertThatThrownBy(() -> commentService.getCommentWithAttachment(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("You do not have permission");
    }

    @Test
    void getCommentWithAttachment_shouldAllowAdmin_toAccessAllAttachments() {
        // Given
        testComment.setAttachmentFilename("test.txt");
        testComment.setAttachmentData("data".getBytes());
        testComment.setInternal(true); // Even internal comments

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When
        IssueComment result = commentService.getCommentWithAttachment(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttachmentFilename()).isEqualTo("test.txt");
    }

    @Test
    void getCommentWithAttachment_shouldThrowException_whenNoAttachment() {
        // Given - Comment without attachment
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));

        // When/Then
        assertThatThrownBy(() -> commentService.getCommentWithAttachment(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Comment has no attachment");
    }
}
