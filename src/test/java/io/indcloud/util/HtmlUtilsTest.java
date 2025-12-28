package io.indcloud.util;

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

    // ========== REGRESSION: Unicode Whitespace Tests ==========
    // Bug: trim() only strips ASCII whitespace ≤0x20, not Unicode whitespace
    // Payloads like <p>&nbsp;</p> would pass validation and persist

    @Test
    void REGRESSION_hasTextContent_shouldRejectNonBreakingSpaces() {
        // &nbsp; entity (U+00A0) - common in Quill/rich text editors
        assertThat(HtmlUtils.hasTextContent("<p>&nbsp;</p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p>&nbsp;&nbsp;&nbsp;</p>")).isFalse();

        // Mixed with ASCII whitespace
        assertThat(HtmlUtils.hasTextContent("<p> &nbsp; </p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<div>&nbsp;<span>&nbsp;</span>&nbsp;</div>")).isFalse();
    }

    @Test
    void REGRESSION_hasTextContent_shouldRejectZeroWidthSpaces() {
        // Zero-width space (U+200B)
        String zeroWidthSpace = "\u200B";
        assertThat(HtmlUtils.hasTextContent("<p>" + zeroWidthSpace + "</p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p>" + zeroWidthSpace + zeroWidthSpace + "</p>")).isFalse();

        // Mixed with other whitespace
        assertThat(HtmlUtils.hasTextContent("<p> " + zeroWidthSpace + " </p>")).isFalse();
    }

    @Test
    void REGRESSION_hasTextContent_shouldRejectOtherUnicodeWhitespace() {
        // Various Unicode whitespace characters
        String thinSpace = "\u2009";      // Thin space (U+2009)
        String hairSpace = "\u200A";      // Hair space (U+200A)
        String ideographicSpace = "\u3000"; // Ideographic space (U+3000)

        assertThat(HtmlUtils.hasTextContent("<p>" + thinSpace + "</p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p>" + hairSpace + "</p>")).isFalse();
        assertThat(HtmlUtils.hasTextContent("<p>" + ideographicSpace + "</p>")).isFalse();

        // Mixed Unicode whitespace
        assertThat(HtmlUtils.hasTextContent("<p>&nbsp;" + thinSpace + hairSpace + "</p>")).isFalse();
    }

    @Test
    void REGRESSION_hasTextContent_shouldAcceptTextWithUnicodeWhitespace() {
        // Valid text that happens to contain Unicode whitespace
        assertThat(HtmlUtils.hasTextContent("<p>Hello&nbsp;World</p>")).isTrue();
        assertThat(HtmlUtils.hasTextContent("<p>Valid\u200BText</p>")).isTrue();
        assertThat(HtmlUtils.hasTextContent("<p>Japanese\u3000Space</p>")).isTrue();
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
