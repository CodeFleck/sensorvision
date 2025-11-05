package org.sensorvision.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.IssueCommentDto;
import org.sensorvision.dto.IssueCommentRequest;
import org.sensorvision.service.IssueCommentService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests for AdminIssueController - focus on admin REST API validation.
 * Regression tests for HTML content validation on admin JSON endpoints.
 */
@ExtendWith(MockitoExtension.class)
class AdminIssueControllerTest {

    @Mock
    private IssueCommentService commentService;

    @InjectMocks
    private AdminIssueController controller;

    // ========== REGRESSION: Admin JSON Endpoint HTML Validation ==========
    // Bug: Admin JSON endpoint at line 93 didn't validate HTML content
    // Admins using REST API directly could bypass UI validation and post empty comments

    @Test
    void REGRESSION_addComment_shouldRejectQuillEmptyMarkup() {
        // Given - Quill empty editor markup
        IssueCommentRequest request = new IssueCommentRequest("<p><br></p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        // Service should NOT be called for invalid content
        verify(commentService, never()).addAdminComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldRejectWhitespaceOnlyHtml() {
        // Given - HTML with only whitespace
        IssueCommentRequest request = new IssueCommentRequest("<p>   </p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addAdminComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldRejectNonBreakingSpacesOnly() {
        // CRITICAL: This is the Unicode whitespace bug
        // &nbsp; (U+00A0) would pass trim() but should be rejected
        IssueCommentRequest request = new IssueCommentRequest("<p>&nbsp;</p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addAdminComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldRejectZeroWidthSpaces() {
        // Zero-width space (U+200B) - another Unicode whitespace variant
        String zeroWidthSpace = "\u200B";
        IssueCommentRequest request = new IssueCommentRequest("<p>" + zeroWidthSpace + "</p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addAdminComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldAcceptValidHtmlContent() {
        // Given - Valid HTML with actual text
        IssueCommentRequest request = new IssueCommentRequest("<p>Valid admin comment</p>", false);
        IssueCommentDto expectedDto = new IssueCommentDto(
            1L, 1L, 1L, "admin", "<p>Valid admin comment</p>", false,
            false, null, null, null, java.time.Instant.now()
        );

        when(commentService.addAdminComment(1L, request)).thenReturn(expectedDto);

        // When - Should NOT throw
        controller.addComment(1L, request);

        // Then - Service should be called
        verify(commentService).addAdminComment(1L, request);
    }

    @Test
    void REGRESSION_addComment_shouldAcceptInternalComments() {
        // Given - Valid internal comment
        IssueCommentRequest request = new IssueCommentRequest("<p>Internal note</p>", true);
        IssueCommentDto expectedDto = new IssueCommentDto(
            1L, 1L, 1L, "admin", "<p>Internal note</p>", true,
            false, null, null, null, java.time.Instant.now()
        );

        when(commentService.addAdminComment(1L, request)).thenReturn(expectedDto);

        // When - Should NOT throw
        controller.addComment(1L, request);

        // Then - Service should be called with internal flag
        verify(commentService).addAdminComment(1L, request);
    }

    @Test
    void REGRESSION_addComment_shouldRejectEmptyInternalComments() {
        // Given - Empty internal comment (internal flag doesn't bypass validation)
        IssueCommentRequest request = new IssueCommentRequest("<p>&nbsp;</p>", true);

        // When/Then - Should throw even for internal comments
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addAdminComment(anyLong(), any());
    }
}
