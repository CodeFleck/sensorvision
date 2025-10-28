package org.sensorvision.model;

import jakarta.persistence.*;

@Entity
@Table(name = "issue_submissions")
public class IssueSubmission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IssueCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IssueSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IssueStatus status = IssueStatus.SUBMITTED;

    @Column(name = "screenshot_filename", length = 255)
    private String screenshotFilename;

    @Column(name = "screenshot_data", columnDefinition = "bytea")
    private byte[] screenshotData;

    @Column(name = "screenshot_content_type", length = 100)
    private String screenshotContentType;

    @Column(name = "browser_info", columnDefinition = "TEXT")
    private String browserInfo;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "screen_resolution", length = 50)
    private String screenResolution;

    @Column(name = "last_viewed_at")
    private java.time.Instant lastViewedAt;

    // Constructors
    public IssueSubmission() {
    }

    public IssueSubmission(User user, Organization organization, String title, String description,
                          IssueCategory category, IssueSeverity severity) {
        this.user = user;
        this.organization = organization;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueCategory getCategory() {
        return category;
    }

    public void setCategory(IssueCategory category) {
        this.category = category;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public String getScreenshotFilename() {
        return screenshotFilename;
    }

    public void setScreenshotFilename(String screenshotFilename) {
        this.screenshotFilename = screenshotFilename;
    }

    public byte[] getScreenshotData() {
        return screenshotData;
    }

    public void setScreenshotData(byte[] screenshotData) {
        this.screenshotData = screenshotData;
    }

    public String getScreenshotContentType() {
        return screenshotContentType;
    }

    public void setScreenshotContentType(String screenshotContentType) {
        this.screenshotContentType = screenshotContentType;
    }

    public String getBrowserInfo() {
        return browserInfo;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getScreenResolution() {
        return screenResolution;
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public java.time.Instant getLastViewedAt() {
        return lastViewedAt;
    }

    public void setLastViewedAt(java.time.Instant lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }
}
