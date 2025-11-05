/**
 * Utility functions for HTML content validation and processing
 */

/**
 * Checks if a string contains only whitespace (Unicode-aware).
 * Matches Java's String.isBlank() behavior.
 *
 * Handles:
 * - ASCII whitespace (space, tab, newline, etc.)
 * - Non-breaking space (&nbsp; / U+00A0)
 * - Zero-width space (U+200B)
 * - Other Unicode whitespace
 *
 * @param str - The string to check
 * @returns true if string is null, empty, or contains only whitespace
 */
const isBlank = (str: string | null | undefined): boolean => {
  if (!str) return true;

  // Remove all Unicode whitespace characters
  // \s matches most whitespace, but we explicitly add common Unicode variants
  // U+00A0 (nbsp), U+200B (zero-width space), U+2009 (thin space), etc.
  const stripped = str.replace(/[\s\u00A0\u200B\u2009\u200A\u3000]/g, '');

  return stripped.length === 0;
};

/**
 * Checks if HTML content is empty or contains only whitespace/markup.
 * This is critical for validating rich text editor content (like Quill)
 * which produces markup like <p><br></p> for empty editors.
 *
 * IMPORTANT: Uses Unicode-aware whitespace checking to match backend validation.
 * This prevents confusing validation errors where frontend passes but backend rejects.
 *
 * @param html - The HTML string to validate
 * @returns true if content is meaningful, false if empty/whitespace-only
 *
 * @example
 * hasTextContent('<p><br></p>') // false - Quill empty editor
 * hasTextContent('<p>   </p>') // false - Only ASCII whitespace
 * hasTextContent('<p>&nbsp;</p>') // false - Only non-breaking spaces
 * hasTextContent('<p>Hello</p>') // true - Has text
 * hasTextContent('<p><strong></strong></p>') // false - Only empty tags
 */
export const hasTextContent = (html: string | null | undefined): boolean => {
  if (!html) return false;

  // Create a temporary DOM element to parse HTML
  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = html;

  // Get text content (strips all HTML tags)
  const textContent = tempDiv.textContent || tempDiv.innerText || '';

  // CRITICAL: Use Unicode-aware whitespace check (isBlank) instead of trim()
  // trim() only handles ASCII whitespace, missing nbsp, zero-width spaces, etc.
  // This matches the backend's String.isBlank() behavior
  return !isBlank(textContent);
};

/**
 * Strips HTML tags and returns plain text content.
 * Useful for validation and length checks.
 *
 * @param html - The HTML string to strip
 * @returns Plain text content without HTML tags, with Unicode whitespace stripped
 */
export const stripHtmlTags = (html: string | null | undefined): string => {
  if (!html) return '';

  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = html;

  const text = tempDiv.textContent || tempDiv.innerText || '';

  // Use Unicode-aware whitespace removal
  return text.replace(/^[\s\u00A0\u200B\u2009\u200A\u3000]+|[\s\u00A0\u200B\u2009\u200A\u3000]+$/g, '');
};
