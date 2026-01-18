import React, { useMemo } from 'react';
import DOMPurify from 'dompurify';

interface SafeLLMContentProps {
  content: string;
  className?: string;
}

/**
 * Safely displays LLM-generated content after sanitizing it to prevent XSS attacks.
 * Handles markdown-like formatting from LLM responses.
 *
 * Security: Uses DOMPurify with a strict allowlist of tags to prevent:
 * - Script injection
 * - Event handler injection
 * - Malicious URLs
 * - Style-based attacks
 */
export const SafeLLMContent: React.FC<SafeLLMContentProps> = ({ content, className = '' }) => {
  const sanitizedHtml = useMemo(() => {
    if (!content) return '';

    // Convert markdown-like content to HTML
    let html = content
      // Escape any existing HTML first (defense in depth)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      // Then convert markdown-like patterns to safe HTML
      // Headers
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      // Bold and italic
      .replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      // Code blocks (simple inline)
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      // Lists (basic support)
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/^\* (.+)$/gm, '<li>$1</li>')
      .replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>')
      // Line breaks
      .replace(/\n\n/g, '</p><p>')
      .replace(/\n/g, '<br/>');

    // Wrap in paragraph if not starting with a block element
    if (!html.startsWith('<h') && !html.startsWith('<p')) {
      html = `<p>${html}</p>`;
    }

    // Wrap consecutive <li> elements in <ul>
    html = html.replace(/(<li>.*?<\/li>)(\s*<li>)/g, '$1$2');
    html = html.replace(/(<li>.*?<\/li>)+/g, '<ul>$&</ul>');

    // Sanitize with strict allowlist
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'b', 'i', 'u',
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li',
        'blockquote', 'pre', 'code',
        'table', 'thead', 'tbody', 'tr', 'th', 'td',
        'span', 'div',
      ],
      ALLOWED_ATTR: ['class'],
      // Forbid any URL-based attributes to prevent javascript: URLs
      FORBID_ATTR: ['href', 'src', 'xlink:href', 'action', 'formaction'],
      // Forbid data URIs
      ALLOW_DATA_ATTR: false,
    });
  }, [content]);

  return (
    <div
      className={`llm-content prose prose-sm max-w-none dark:prose-invert ${className}`}
      dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
    />
  );
};

export default SafeLLMContent;
