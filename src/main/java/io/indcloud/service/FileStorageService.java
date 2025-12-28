package io.indcloud.service;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling file storage operations, particularly avatar images.
 * Provides validation, processing, and storage of uploaded files.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int AVATAR_SIZE = 256; // 256x256 pixels

    private final Path avatarStorageLocation;

    public FileStorageService(@Value("${app.upload.avatar-dir:uploads/avatars}") String avatarDir) {
        this.avatarStorageLocation = Paths.get(avatarDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.avatarStorageLocation);
            log.info("Avatar storage directory created/verified at: {}", this.avatarStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create avatar storage directory", ex);
        }
    }

    /**
     * Store an avatar image for a user
     *
     * @param userId User ID
     * @param file   Uploaded file
     * @return Relative path to the stored file
     */
    public String storeAvatar(Long userId, MultipartFile file) {
        validateFile(file);

        try {
            // Read and process the image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new FileStorageException("Failed to read image file. File may be corrupted.");
            }

            // Resize image to standard size
            BufferedImage resizedImage = resizeImage(originalImage, AVATAR_SIZE, AVATAR_SIZE);

            // Generate filename
            String filename = userId + ".png";
            Path targetLocation = this.avatarStorageLocation.resolve(filename);

            // Save as PNG (lossless, good quality)
            ImageIO.write(resizedImage, "png", targetLocation.toFile());

            log.info("Avatar stored successfully for user {}: {}", userId, filename);
            return filename;

        } catch (IOException ex) {
            log.error("Failed to store avatar for user {}", userId, ex);
            throw new FileStorageException("Failed to store avatar file", ex);
        }
    }

    /**
     * Load avatar file for a user
     *
     * @param filename Avatar filename
     * @return File contents as byte array
     */
    public byte[] loadAvatar(String filename) {
        try {
            Path filePath = this.avatarStorageLocation.resolve(filename).normalize();

            // Security check: ensure the file is within the avatar directory
            if (!filePath.startsWith(this.avatarStorageLocation)) {
                throw new FileStorageException("Invalid file path: potential path traversal attack");
            }

            if (!Files.exists(filePath)) {
                throw new FileStorageException("File not found: " + filename);
            }

            return Files.readAllBytes(filePath);

        } catch (IOException ex) {
            log.error("Failed to load avatar: {}", filename, ex);
            throw new FileStorageException("Failed to load avatar file", ex);
        }
    }

    /**
     * Delete avatar file for a user
     *
     * @param filename Avatar filename
     */
    public void deleteAvatar(String filename) {
        try {
            Path filePath = this.avatarStorageLocation.resolve(filename).normalize();

            // Security check
            if (!filePath.startsWith(this.avatarStorageLocation)) {
                throw new FileStorageException("Invalid file path: potential path traversal attack");
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Avatar deleted successfully: {}", filename);
            } else {
                log.warn("Attempted to delete non-existent avatar: {}", filename);
            }

        } catch (IOException ex) {
            log.error("Failed to delete avatar: {}", filename, ex);
            throw new FileStorageException("Failed to delete avatar file", ex);
        }
    }

    /**
     * Check if avatar exists for a user
     *
     * @param filename Avatar filename
     * @return true if avatar exists
     */
    public boolean avatarExists(String filename) {
        Path filePath = this.avatarStorageLocation.resolve(filename).normalize();
        return Files.exists(filePath) && filePath.startsWith(this.avatarStorageLocation);
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException(
                    String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }

        // Validate filename
        String filename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (filename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + filename);
        }
    }

    /**
     * Resize image to specified dimensions while maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Calculate scaling to cover the target area (similar to CSS object-fit: cover)
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scaleX = (double) targetWidth / originalWidth;
        double scaleY = (double) targetHeight / originalHeight;
        double scale = Math.max(scaleX, scaleY); // Use max to cover the area

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        // Create intermediate scaled image
        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

        // Create final image with exact target dimensions
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Center the image
        int x = (targetWidth - scaledWidth) / 2;
        int y = (targetHeight - scaledHeight) / 2;

        g2d.drawImage(scaledImage, x, y, null);
        g2d.dispose();

        return resizedImage;
    }
}
