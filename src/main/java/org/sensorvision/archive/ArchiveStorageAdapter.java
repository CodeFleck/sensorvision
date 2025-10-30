package org.sensorvision.archive;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.TelemetryRecord;

import java.io.IOException;
import java.util.List;

/**
 * Interface for storing archived telemetry data in various storage backends
 */
public interface ArchiveStorageAdapter {

    /**
     * Store archived telemetry records
     *
     * @param records List of telemetry records to archive
     * @param organizationId Organization ID for the records
     * @param archiveDate Date range identifier for the archive
     * @param storageConfig Storage-specific configuration
     * @return Path/URI where the archive was stored
     * @throws IOException if storage operation fails
     */
    String storeArchive(
            List<TelemetryRecord> records,
            Long organizationId,
            String archiveDate,
            JsonNode storageConfig
    ) throws IOException;

    /**
     * Retrieve archived telemetry records
     *
     * @param archivePath Path/URI where the archive is stored
     * @param storageConfig Storage-specific configuration
     * @return List of restored telemetry records
     * @throws IOException if retrieval operation fails
     */
    List<TelemetryRecord> retrieveArchive(
            String archivePath,
            JsonNode storageConfig
    ) throws IOException;

    /**
     * Delete an archive
     *
     * @param archivePath Path/URI where the archive is stored
     * @param storageConfig Storage-specific configuration
     * @throws IOException if deletion operation fails
     */
    void deleteArchive(
            String archivePath,
            JsonNode storageConfig
    ) throws IOException;

    /**
     * Get the size of an archive in bytes
     *
     * @param archivePath Path/URI where the archive is stored
     * @param storageConfig Storage-specific configuration
     * @return Size in bytes
     * @throws IOException if size calculation fails
     */
    long getArchiveSize(
            String archivePath,
            JsonNode storageConfig
    ) throws IOException;
}
