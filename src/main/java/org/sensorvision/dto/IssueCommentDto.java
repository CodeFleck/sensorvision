package org.sensorvision.dto;

import org.sensorvision.model.IssueComment;

import java.time.Instant;

public record IssueCommentDto(
    Long id,
    Long issueId,
    Long authorId,
    String authorName,
    String message,
    boolean internal,
    boolean hasAttachment,
    String attachmentFilename,
    String attachmentContentType,
    Long attachmentSizeBytes,
    Instant createdAt
) {
    public static IssueCommentDto fromEntity(IssueComment comment) {
        return new IssueCommentDto(
            comment.getId(),
            comment.getIssue().getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName(),
            comment.getMessage(),
            comment.isInternal(),
            comment.hasAttachment(),
            comment.getAttachmentFilename(),
            comment.getAttachmentContentType(),
            comment.getAttachmentSizeBytes(),
            comment.getCreatedAt()
        );
    }
}
