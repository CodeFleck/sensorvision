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
 * Tests for IssueSubmissionController - focus on request validation.
 * Regression tests for HTML content validation on JSON endpoints.
 */
@ExtendWith(MockitoExtension.class)
class IssueSubmissionControllerTest {

    @Mock
    private IssueCommentService commentService;

    @InjectMocks
    private IssueSubmissionController controller;

    // ========== REGRESSION: JSON Endpoint HTML Validation ==========

    @Test
    void REGRESSION_addComment_shouldRejectQuillEmptyMarkup() {
        // Bug: JSON endpoint at line 123 didn't validate HTML content
        // Quill's <p><br></p> would pass @NotBlank but should be rejected

        // Given - Quill empty editor markup
        IssueCommentRequest request = new IssueCommentRequest("<p><br></p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        // Service should NOT be called for invalid content
        verify(commentService, never()).addUserComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldRejectWhitespaceOnlyHtml() {
        // Given - HTML with only whitespace
        IssueCommentRequest request = new IssueCommentRequest("<p>   </p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addUserComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldRejectEmptyNestedTags() {
        // Given - Nested empty tags
        IssueCommentRequest request = new IssueCommentRequest("<p><strong></strong></p>", false);

        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() -> controller.addComment(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addUserComment(anyLong(), any());
    }

    @Test
    void REGRESSION_addComment_shouldAcceptValidHtmlContent() {
        // Given - Valid HTML with actual text
        IssueCommentRequest request = new IssueCommentRequest("<p>Valid comment</p>", false);
        IssueCommentDto expectedDto = new IssueCommentDto(
            1L, 1L, 1L, "user", "<p>Valid comment</p>", false,
            false, null, null, null, java.time.Instant.now()
        );

        when(commentService.addUserComment(1L, request)).thenReturn(expectedDto);

        // When - Should NOT throw
        controller.addComment(1L, request);

        // Then - Service should be called
        verify(commentService).addUserComment(1L, request);
    }

    @Test
    void REGRESSION_addCommentWithAttachment_shouldAlsoValidateHtml() {
        // Bug: Multipart endpoint had validation but JSON endpoint didn't
        // Verify both paths have consistent validation

        // Given - Quill empty markup on multipart endpoint
        // When/Then - Should throw IllegalArgumentException
        assertThatThrownBy(() ->
            controller.addCommentWithAttachment(1L, "<p><br></p>", false, null)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message is required");

        verify(commentService, never()).addUserComment(anyLong(), any(), any());
    }
}
