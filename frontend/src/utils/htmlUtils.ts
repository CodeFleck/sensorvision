/**
 * Utility functions for HTML content validation and processing
 */

/**
 * Checks if HTML content is empty or contains only whitespace/markup.
 * This is critical for validating rich text editor content (like Quill)
 * which produces markup like <p><br></p> for empty editors.
 *
 * @param html - The HTML string to validate
 * @returns true if content is meaningful, false if empty/whitespace-only
 *
 * @example
 * hasTextContent('<p><br></p>') // false - Quill empty editor
 * hasTextContent('<p>   </p>') // false - Only whitespace
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

  // Check if there's any non-whitespace text
  return textContent.trim().length > 0;
};

/**
 * Strips HTML tags and returns plain text content.
 * Useful for validation and length checks.
 *
 * @param html - The HTML string to strip
 * @returns Plain text content without HTML tags
 */
export const stripHtmlTags = (html: string | null | undefined): string => {
  if (!html) return '';

  const tempDiv = document.createElement('div');
  tempDiv.innerHTML = html;

  return (tempDiv.textContent || tempDiv.innerText || '').trim();
};
