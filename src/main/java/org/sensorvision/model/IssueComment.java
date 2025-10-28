package org.sensorvision.model;

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
}
