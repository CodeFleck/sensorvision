import { describe, it, expect } from 'vitest';
import { hasTextContent, stripHtmlTags } from './htmlUtils';

describe('htmlUtils', () => {
  describe('hasTextContent', () => {
    // ========== REGRESSION: Empty Quill HTML Validation ==========
    // Bug: Quill's empty editor produces <p><br></p> which passes .trim() check
    // but should be treated as empty content

    it('REGRESSION: should reject Quill empty editor markup', () => {
      // Quill's default empty state
      expect(hasTextContent('<p><br></p>')).toBe(false);
      expect(hasTextContent('<p><br/></p>')).toBe(false);
      expect(hasTextContent('<p><br /></p>')).toBe(false);
    });

    it('REGRESSION: should reject whitespace-only HTML', () => {
      expect(hasTextContent('<p>   </p>')).toBe(false);
      expect(hasTextContent('<p>\n\t </p>')).toBe(false);
      expect(hasTextContent('<div>  <span>  </span>  </div>')).toBe(false);
    });

    it('REGRESSION: should reject empty nested tags', () => {
      expect(hasTextContent('<p><strong></strong></p>')).toBe(false);
      expect(hasTextContent('<p><em><strong></strong></em></p>')).toBe(false);
      expect(hasTextContent('<div><p></p></div>')).toBe(false);
    });

    // ========== Valid Content Tests ==========

    it('should accept HTML with actual text content', () => {
      expect(hasTextContent('<p>Hello</p>')).toBe(true);
      expect(hasTextContent('<p>Hello <strong>World</strong></p>')).toBe(true);
      expect(hasTextContent('<div><p>Text</p></div>')).toBe(true);
    });

    it('should accept plain text without HTML', () => {
      expect(hasTextContent('Plain text')).toBe(true);
      expect(hasTextContent('Text with spaces')).toBe(true);
    });

    it('should handle special characters', () => {
      expect(hasTextContent('<p>!@#$%</p>')).toBe(true);
      expect(hasTextContent('<p>123</p>')).toBe(true);
      expect(hasTextContent('<p>&nbsp;</p>')).toBe(false); // Non-breaking space is still whitespace
    });

    // ========== Edge Cases ==========

    it('should handle null and undefined', () => {
      expect(hasTextContent(null)).toBe(false);
      expect(hasTextContent(undefined)).toBe(false);
      expect(hasTextContent('')).toBe(false);
    });

    it('should handle malformed HTML', () => {
      expect(hasTextContent('<p>Unclosed tag')).toBe(true);
      expect(hasTextContent('No tags at all')).toBe(true);
      // Note: <><> behavior with DOM parsing may vary
    });

    // ========== REGRESSION: Frontend/Backend Consistency ==========
    // Bug: Frontend used trim() while backend used isBlank()
    // This caused confusing validation errors where frontend passed but backend rejected

    it('REGRESSION: should reject non-breaking spaces only (nbsp)', () => {
      // Multiple nbsp characters - common in rich text editors
      expect(hasTextContent('<p>&nbsp;</p>')).toBe(false);
      expect(hasTextContent('<p>&nbsp;&nbsp;&nbsp;</p>')).toBe(false);
      expect(hasTextContent('<div>&nbsp;<span>&nbsp;</span></div>')).toBe(false);
    });

    it('REGRESSION: should reject zero-width spaces', () => {
      // Zero-width space (U+200B) - invisible character
      const zws = '\u200B';
      expect(hasTextContent(`<p>${zws}</p>`)).toBe(false);
      expect(hasTextContent(`<p>${zws}${zws}${zws}</p>`)).toBe(false);
      expect(hasTextContent(`<p> ${zws} </p>`)).toBe(false);
    });

    it('REGRESSION: should reject other Unicode whitespace', () => {
      // Thin space (U+2009), hair space (U+200A), ideographic space (U+3000)
      expect(hasTextContent('<p>\u2009</p>')).toBe(false);
      expect(hasTextContent('<p>\u200A</p>')).toBe(false);
      expect(hasTextContent('<p>\u3000</p>')).toBe(false);

      // Mixed Unicode whitespace
      expect(hasTextContent('<p>&nbsp;\u2009\u200A</p>')).toBe(false);
    });

    it('REGRESSION: should accept text with Unicode whitespace inside', () => {
      // Valid text that contains Unicode whitespace is still valid
      expect(hasTextContent('<p>Hello&nbsp;World</p>')).toBe(true);
      expect(hasTextContent('<p>Valid\u200BText</p>')).toBe(true);
      expect(hasTextContent('<p>Japanese\u3000Space</p>')).toBe(true);
    });
  });

  describe('stripHtmlTags', () => {
    it('should remove HTML tags and return plain text', () => {
      expect(stripHtmlTags('<p>Hello</p>')).toBe('Hello');
      expect(stripHtmlTags('<p>Hello <strong>World</strong></p>')).toBe('Hello World');
      expect(stripHtmlTags('<div><p>Nested</p></div>')).toBe('Nested');
    });

    it('should handle empty content', () => {
      expect(stripHtmlTags('')).toBe('');
      expect(stripHtmlTags(null)).toBe('');
      expect(stripHtmlTags(undefined)).toBe('');
      expect(stripHtmlTags('<p></p>')).toBe('');
    });

    it('should trim whitespace', () => {
      expect(stripHtmlTags('<p>  Text  </p>')).toBe('Text');
      expect(stripHtmlTags('  <p>Text</p>  ')).toBe('Text');
    });
  });
});
