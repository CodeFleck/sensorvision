package org.sensorvision.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HtmlUtils - HTML content validation and processing.
 * Focus on regression testing for empty Quill editor markup validation.
 */
class HtmlUtilsTest {

    // ========== REGRESSION: Empty Quill HTML Validation ==========
    // Bug: Quill's empty editor produces <p><br></p> which passes .trim() check
    // but should be treated as empty content

    @Test
    void hasTextContent_shouldRejectQuillEmptyEditorMarkup() {
        // Quill's default empty state variations
        assertThat(HtmlUtils.hasTextContent("<p><br></p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p><br/></p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p><br /></p>")).isFalse();
    }

    @Test
    void hasTextContent_shouldRejectWhitespaceOnlyHtml() {
        assertThat(HtmlUtils.hasTextContent("<p>   </p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p>\n\t </p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<div>  <span>  </span>  </div>")).isFalse();
    }

    @Test
    void hasTextContent_shouldRejectEmptyNestedTags() {
        assertThat(HtmlUtils.hasTextContent("<p><strong></strong></p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p><em><strong></strong></em></p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<div><p></p></div>")).isFalse();
    }

    // ========== Valid Content Tests ==========

    @Test
    void hasTextContent_shouldAcceptHtmlWithActualText() {
        assertThat(HtmlUtils.hasTextContent("<p>Hello</p>")).isTrue();
        assertThat(HtmlUtils.hasTextContent("<p>Hello <strong>World</strong></p>")).isTrue();
        assertThat(HtmlUtils.hasTextContent("<div><p>Text</p></div>")).isTrue();
    }

    @Test
    void hasTextContent_shouldAcceptPlainTextWithoutHtml() {
        assertThat(HtmlUtils.hasTextContent("Plain text")).isTrue();
        assertThat(HtmlUtils.hasTextContent("Text with spaces")).isTrue();
    }

    @Test
    void hasTextContent_shouldHandleSpecialCharacters() {
        assertThat(HtmlUtils.hasTextContent("<p>!@#$%</p>")).isTrue();
        assertThat(HtmlUtils.hasTextContent("<p>123</p>")).isTrue();
    }

    // ========== Edge Cases ==========

    @Test
    void hasTextContent_shouldHandleNullAndEmpty() {
        assertThat(HtmlUtils.hasTextContent(null)).isFalse();
        assertThat(HtmlUtils.hasTextContent("")).isFalse();
    }

    @Test
    void hasTextContent_shouldHandleMalformedHtml() {
        assertThat(HtmlUtils.hasTextContent("<p>Unclosed tag")).isTrue();
        assertThat(HtmlUtils.hasTextContent("No tags at all")).isTrue();
        // Note: <><> is parsed by Jsoup as empty tags, behavior may vary
    }

    // ========== stripHtmlTags Tests ==========

    @Test
    void stripHtmlTags_shouldRemoveTagsAndReturnPlainText() {
        assertThat(HtmlUtils.stripHtmlTags("<p>Hello</p>")).isEqualTo("Hello");
        assertThat(HtmlUtils.stripHtmlTags("<p>Hello <strong>World</strong></p>")).isEqualTo("Hello World");
        assertThat(HtmlUtils.stripHtmlTags("<div><p>Nested</p></div>")).isEqualTo("Nested");
    }

    @Test
    void stripHtmlTags_shouldHandleEmptyContent() {
        assertThat(HtmlUtils.stripHtmlTags("")).isEmpty();
        assertThat(HtmlUtils.stripHtmlTags(null)).isEmpty();
        assertThat(HtmlUtils.stripHtmlTags("<p></p>")).isEmpty();
    }

    @Test
    void stripHtmlTags_shouldTrimWhitespace() {
        assertThat(HtmlUtils.stripHtmlTags("<p>  Text  </p>")).isEqualTo("Text");
        assertThat(HtmlUtils.stripHtmlTags("  <p>Text</p>  ")).isEqualTo("Text");
    }
}
