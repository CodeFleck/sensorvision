package org.sensorvision.archive;

import lombok.RequiredArgsConstructor;
import org.sensorvision.model.ArchiveStorageType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveStorageAdapterFactory {

    private final LocalFileStorageAdapter localFileStorageAdapter;

    public ArchiveStorageAdapter getAdapter(ArchiveStorageType storageType) {
        return switch (storageType) {
            case LOCAL_FILE -> localFileStorageAdapter;
            case S3, AZURE_BLOB, GCS -> throw new UnsupportedOperationException(
                    "Cloud storage not yet implemented: " + storageType
            );
        };
    }
}
