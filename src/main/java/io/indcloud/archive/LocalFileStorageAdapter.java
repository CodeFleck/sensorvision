package io.indcloud.archive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.TelemetryRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorageAdapter implements ArchiveStorageAdapter {

    private final ObjectMapper objectMapper;

    @Value("${app.archive.local.base-path:./archives}")
    private String basePath;

    @Override
    public String storeArchive(
            List<TelemetryRecord> records,
            Long organizationId,
            String archiveDate,
            JsonNode storageConfig
    ) throws IOException {
        // Create directory structure: {basePath}/{organizationId}/{year}/{month}/
        LocalDate date = LocalDate.parse(archiveDate, DateTimeFormatter.ISO_DATE);
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());

        Path archiveDir = Paths.get(basePath, organizationId.toString(), year, month);
        Files.createDirectories(archiveDir);

        // Create archive file: telemetry_{date}.json.gz
        String fileName = String.format("telemetry_%s.json.gz", archiveDate);
        Path archiveFile = archiveDir.resolve(fileName);

        log.info("Storing {} records to archive: {}", records.size(), archiveFile);

        // Write records as JSON array, compressed with GZIP
        try (OutputStream fileOut = Files.newOutputStream(archiveFile);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzipOut))) {

            writer.write("[\n");
            for (int i = 0; i < records.size(); i++) {
                TelemetryRecord record = records.get(i);
                String json = objectMapper.writeValueAsString(record);
                writer.write(json);
                if (i < records.size() - 1) {
                    writer.write(",\n");
                }
            }
            writer.write("\n]");
        }

        log.info("Archive created successfully: {} (size: {} bytes)",
                archiveFile, Files.size(archiveFile));

        return archiveFile.toString();
    }

    @Override
    public List<TelemetryRecord> retrieveArchive(
            String archivePath,
            JsonNode storageConfig
    ) throws IOException {
        Path file = Paths.get(archivePath);

        if (!Files.exists(file)) {
            throw new FileNotFoundException("Archive not found: " + archivePath);
        }

        log.info("Retrieving archive from: {}", archivePath);

        try (InputStream fileIn = Files.newInputStream(file);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIn))) {

            // Read entire JSON array
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            // Parse JSON array into list of TelemetryRecord
            TelemetryRecord[] records = objectMapper.readValue(
                    jsonBuilder.toString(),
                    TelemetryRecord[].class
            );

            log.info("Retrieved {} records from archive", records.length);
            return List.of(records);
        }
    }

    @Override
    public void deleteArchive(String archivePath, JsonNode storageConfig) throws IOException {
        Path file = Paths.get(archivePath);

        if (Files.exists(file)) {
            Files.delete(file);
            log.info("Deleted archive: {}", archivePath);
        } else {
            log.warn("Archive not found for deletion: {}", archivePath);
        }
    }

    @Override
    public long getArchiveSize(String archivePath, JsonNode storageConfig) throws IOException {
        Path file = Paths.get(archivePath);

        if (!Files.exists(file)) {
            throw new FileNotFoundException("Archive not found: " + archivePath);
        }

        return Files.size(file);
    }
}
