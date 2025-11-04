package org.sensorvision.util;

import org.jsoup.Jsoup;

/**
 * Utility class for HTML content validation and processing.
 * Provides methods to validate rich text editor content and prevent empty submissions.
 */
public class HtmlUtils {

    /**
     * Checks if HTML content contains actual text (not just markup/whitespace).
     * This is critical for validating rich text editor content (like Quill)
     * which produces markup like {@code <p><br></p>} for empty editors.
     *
     * @param html The HTML string to validate
     * @return true if content has meaningful text, false if empty/whitespace-only
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code hasTextContent("<p><br></p>")} returns {@code false} - Quill empty editor</li>
     *   <li>{@code hasTextContent("<p>   </p>")} returns {@code false} - Only whitespace</li>
     *   <li>{@code hasTextContent("<p>Hello</p>")} returns {@code true} - Has text</li>
     *   <li>{@code hasTextContent("<p><strong></strong></p>")} returns {@code false} - Only empty tags</li>
     * </ul>
     */
    public static boolean hasTextContent(String html) {
        if (html == null || html.isEmpty()) {
            return false;
        }

        // Use Jsoup to parse HTML and extract text content
        String textContent = Jsoup.parse(html).text();

        // Check if there's any non-whitespace text
        return textContent != null && !textContent.trim().isEmpty();
    }

    /**
     * Strips HTML tags and returns plain text content.
     * Useful for validation and length checks.
     *
     * @param html The HTML string to strip
     * @return Plain text content without HTML tags
     */
    public static String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        return Jsoup.parse(html).text().trim();
    }
}
