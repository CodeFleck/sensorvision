package io.indcloud.model;

public enum ArchiveStorageType {
    LOCAL_FILE("Local File System"),
    S3("Amazon S3"),
    AZURE_BLOB("Azure Blob Storage"),
    GCS("Google Cloud Storage");

    private final String displayName;

    ArchiveStorageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
