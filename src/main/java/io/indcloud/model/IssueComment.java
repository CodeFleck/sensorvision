package io.indcloud.model;

import jakarta.persistence.*;

@Entity
@Table(name = "issue_comments")
public class IssueComment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private IssueSubmission issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_internal", nullable = false)
    private boolean internal = false;

    // File attachment fields
    @Column(name = "attachment_filename", length = 255)
    private String attachmentFilename;

    // Lazy-loaded to avoid fetching large blobs when loading comment lists
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "attachment_data", columnDefinition = "bytea")
    private byte[] attachmentData;

    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;

    @Column(name = "attachment_size_bytes")
    private Long attachmentSizeBytes;

    // Constructors
    public IssueComment() {
    }

    public IssueComment(IssueSubmission issue, User author, String message, boolean internal) {
        this.issue = issue;
        this.author = author;
        this.message = message;
        this.internal = internal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IssueSubmission getIssue() {
        return issue;
    }

    public void setIssue(IssueSubmission issue) {
        this.issue = issue;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getAttachmentFilename() {
        return attachmentFilename;
    }

    public void setAttachmentFilename(String attachmentFilename) {
        this.attachmentFilename = attachmentFilename;
    }

    public byte[] getAttachmentData() {
        return attachmentData;
    }

    public void setAttachmentData(byte[] attachmentData) {
        this.attachmentData = attachmentData;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }

    public Long getAttachmentSizeBytes() {
        return attachmentSizeBytes;
    }

    public void setAttachmentSizeBytes(Long attachmentSizeBytes) {
        this.attachmentSizeBytes = attachmentSizeBytes;
    }

    /**
     * Check if this comment has an attachment
     */
    public boolean hasAttachment() {
        return attachmentFilename != null && attachmentData != null;
    }
}
