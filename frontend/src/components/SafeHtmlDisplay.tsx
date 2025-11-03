import React from 'react';
import DOMPurify from 'dompurify';

interface SafeHtmlDisplayProps {
  html: string;
  className?: string;
}

/**
 * Safely displays HTML content after sanitizing it to prevent XSS attacks.
 * Used for displaying rich text comments in support tickets.
 */
export const SafeHtmlDisplay: React.FC<SafeHtmlDisplayProps> = ({ html, className = '' }) => {
  // Sanitize HTML on the client side as well (defense in depth)
  const sanitizedHtml = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 's', 'b', 'i',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li',
      'blockquote', 'pre', 'code',
      'a',
    ],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
  });

  return (
    <div
      className={`rich-text-content ${className}`}
      dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
      style={{
        // Basic styling for rich text content
        wordBreak: 'break-word',
      }}
    />
  );
};
