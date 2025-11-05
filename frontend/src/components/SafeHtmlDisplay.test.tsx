import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SafeHtmlDisplay } from './SafeHtmlDisplay';

describe('SafeHtmlDisplay', () => {
  // ========== XSS Prevention Tests ==========

  it('should sanitize and render safe HTML', () => {
    const safeHtml = '<p>Hello <strong>World</strong></p>';
    render(<SafeHtmlDisplay html={safeHtml} />);

    expect(screen.getByText(/Hello/)).toBeInTheDocument();
    expect(screen.getByText(/World/)).toBeInTheDocument();
  });

  it('should remove script tags to prevent XSS', () => {
    const dangerousHtml = '<p>Safe content</p><script>alert("xss")</script>';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    // Safe content should be present
    expect(screen.getByText(/Safe content/)).toBeInTheDocument();

    // Script tag should be removed
    expect(container.innerHTML).not.toContain('<script>');
    expect(container.innerHTML).not.toContain('alert');
  });

  it('should remove iframe tags', () => {
    const dangerousHtml = '<p>Content</p><iframe src="https://evil.com"></iframe>';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(screen.getByText(/Content/)).toBeInTheDocument();
    expect(container.innerHTML).not.toContain('<iframe>');
  });

  it('should remove event handlers from HTML', () => {
    const dangerousHtml = '<a href="#" onclick="alert(1)">Click me</a>';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(screen.getByText(/Click me/)).toBeInTheDocument();
    expect(container.innerHTML).not.toContain('onclick');
    expect(container.innerHTML).not.toContain('alert');
  });

  it('should remove onerror attributes from images', () => {
    const dangerousHtml = '<img src="x" onerror="alert(2)">';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(container.innerHTML).not.toContain('onerror');
    expect(container.innerHTML).not.toContain('alert');
  });

  it('should remove javascript: protocol from links', () => {
    const dangerousHtml = '<a href="javascript:alert(1)">Click</a>';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(container.innerHTML).not.toContain('javascript:');
  });

  it('should remove data: URLs from images', () => {
    const dangerousHtml = '<img src="data:text/html,<script>alert(1)</script>">';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(container.innerHTML).not.toContain('data:text/html');
    expect(container.innerHTML).not.toContain('<script>');
  });

  it('should remove object and embed tags', () => {
    const dangerousHtml = '<object data="malicious.swf"></object><embed src="bad.swf">';
    const { container } = render(<SafeHtmlDisplay html={dangerousHtml} />);

    expect(container.innerHTML).not.toContain('<object>');
    expect(container.innerHTML).not.toContain('<embed>');
  });

  // ========== Allowed HTML Tests ==========

  it('should allow paragraph tags', () => {
    const html = '<p>Paragraph text</p>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('p')).toBeInTheDocument();
  });

  it('should allow strong/bold tags', () => {
    const html = '<strong>Bold</strong><b>Also bold</b>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('strong')).toBeInTheDocument();
    expect(container.querySelector('b')).toBeInTheDocument();
  });

  it('should allow italic/emphasis tags', () => {
    const html = '<em>Emphasis</em><i>Italic</i>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('em')).toBeInTheDocument();
    expect(container.querySelector('i')).toBeInTheDocument();
  });

  it('should allow underline and strikethrough tags', () => {
    const html = '<u>Underlined</u><s>Strikethrough</s>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('u')).toBeInTheDocument();
    expect(container.querySelector('s')).toBeInTheDocument();
  });

  it('should allow heading tags', () => {
    const html = '<h1>H1</h1><h2>H2</h2><h3>H3</h3>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('h1')).toBeInTheDocument();
    expect(container.querySelector('h2')).toBeInTheDocument();
    expect(container.querySelector('h3')).toBeInTheDocument();
  });

  it('should allow list tags', () => {
    const html = '<ul><li>Item 1</li></ul><ol><li>Item 2</li></ol>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('ul')).toBeInTheDocument();
    expect(container.querySelector('ol')).toBeInTheDocument();
    expect(container.querySelectorAll('li')).toHaveLength(2);
  });

  it('should allow blockquote tags', () => {
    const html = '<blockquote>Quote text</blockquote>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('blockquote')).toBeInTheDocument();
  });

  it('should allow code and pre tags', () => {
    const html = '<pre><code>code block</code></pre>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('pre')).toBeInTheDocument();
    expect(container.querySelector('code')).toBeInTheDocument();
  });

  it('should allow safe links with href', () => {
    const html = '<a href="https://example.com">Link</a>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    const link = container.querySelector('a');
    expect(link).toBeInTheDocument();
    expect(link?.getAttribute('href')).toBe('https://example.com');
  });

  it('should allow line breaks', () => {
    const html = 'Line 1<br>Line 2';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.querySelector('br')).toBeInTheDocument();
  });

  // ========== className Tests ==========

  it('should apply default rich-text-content className', () => {
    const html = '<p>Text</p>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.firstChild).toHaveClass('rich-text-content');
  });

  it('should apply custom className along with default', () => {
    const html = '<p>Text</p>';
    const { container } = render(<SafeHtmlDisplay html={html} className="custom-class" />);

    expect(container.firstChild).toHaveClass('rich-text-content');
    expect(container.firstChild).toHaveClass('custom-class');
  });

  // ========== Edge Cases ==========

  it('should handle empty HTML string', () => {
    const { container } = render(<SafeHtmlDisplay html="" />);

    expect(container.firstChild).toBeInTheDocument();
    expect(container.firstChild?.textContent).toBe('');
  });

  it('should handle plain text without HTML', () => {
    const text = 'Just plain text';
    render(<SafeHtmlDisplay html={text} />);

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it('should handle HTML entities correctly', () => {
    const html = '<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>';
    render(<SafeHtmlDisplay html={html} />);

    // HTML entities should be decoded and displayed as text
    expect(screen.getByText(/<script>alert\(1\)<\/script>/)).toBeInTheDocument();
  });

  it('should handle malformed HTML gracefully', () => {
    const html = '<p>Unclosed paragraph<strong>Bold without closing';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    // DOMPurify should auto-fix malformed HTML
    expect(container.textContent).toContain('Unclosed paragraph');
    expect(container.textContent).toContain('Bold without closing');
  });

  it('should apply word-break style for long URLs', () => {
    const html = '<p>https://verylongurl.com/with/many/segments/that/would/overflow</p>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    const div = container.firstChild as HTMLElement;
    expect(div.style.wordBreak).toBe('break-word');
  });

  // ========== Complex XSS Attempts ==========
  // Note: Basic XSS prevention is thoroughly tested above. DOMPurify handles complex
  // nested scenarios internally, and the important thing is no XSS actually executes.

  it('should prevent XSS via SVG', () => {
    const html = '<svg><script>alert(1)</script></svg>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.innerHTML).not.toContain('<script>');
    expect(container.innerHTML).not.toContain('alert');
  });

  it('should prevent XSS via style tags', () => {
    const html = '<style>body{background:url("javascript:alert(1)")}</style>';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.innerHTML).not.toContain('<style>');
    expect(container.innerHTML).not.toContain('javascript:');
  });

  it('should prevent mutation XSS (mXSS)', () => {
    const html = '<noscript><p title="</noscript><img src=x onerror=alert(1)>">';
    const { container } = render(<SafeHtmlDisplay html={html} />);

    expect(container.innerHTML).not.toContain('onerror');
    expect(container.innerHTML).not.toContain('alert');
  });
});
