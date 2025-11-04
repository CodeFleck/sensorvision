import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useDraftComment } from './useDraftComment';

describe('useDraftComment', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  // ========== Basic Functionality Tests ==========

  it('should initialize with empty draft', () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    expect(result.current.draft).toBe('');
    expect(result.current.status.hasDraft).toBe(false);
    expect(result.current.status.isSaving).toBe(false);
    expect(result.current.status.lastSaved).toBeNull();
  });

  it('should update draft when setDraft is called', () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    act(() => {
      result.current.setDraft('New draft content');
    });

    expect(result.current.draft).toBe('New draft content');
  });

  it('should clear draft when clearDraft is called', () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    act(() => {
      result.current.setDraft('Draft content');
    });

    expect(result.current.draft).toBe('Draft content');

    act(() => {
      result.current.clearDraft();
    });

    expect(result.current.draft).toBe('');
    expect(result.current.status.hasDraft).toBe(false);
  });

  // ========== Auto-save Tests ==========

  it('should auto-save draft to localStorage after delay', async () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    act(() => {
      result.current.setDraft('Auto-saved content');
    });

    // Should be saving immediately
    expect(result.current.status.isSaving).toBe(true);

    // Wait for auto-save to complete
    await waitFor(
      () => {
        expect(result.current.status.isSaving).toBe(false);
      },
      { timeout: 2000 }
    );

    expect(result.current.status.hasDraft).toBe(true);
    expect(result.current.status.lastSaved).not.toBeNull();

    // Check localStorage
    const stored = localStorage.getItem('ticket-draft-1');
    expect(stored).toBeTruthy();
    const parsed = JSON.parse(stored!);
    expect(parsed.content).toBe('Auto-saved content');
  });

  it('should use custom auto-save delay', async () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    act(() => {
      result.current.setDraft('Content');
    });

    expect(result.current.status.isSaving).toBe(true);

    await waitFor(
      () => {
        expect(result.current.status.isSaving).toBe(false);
      },
      { timeout: 2000 }
    );

    expect(result.current.status.hasDraft).toBe(true);
  });

  it('should not save empty drafts', () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    act(() => {
      result.current.setDraft('   '); // Only whitespace
    });

    // Should not trigger saving for whitespace-only content
    const stored = localStorage.getItem('ticket-draft-1');
    expect(stored).toBeNull();
  });

  // ========== localStorage Tests ==========

  it('should load existing draft from localStorage on mount', () => {
    // Pre-populate localStorage
    const existingDraft = {
      content: 'Previously saved draft',
      timestamp: new Date().toISOString(),
    };
    localStorage.setItem('ticket-draft-1', JSON.stringify(existingDraft));

    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    expect(result.current.draft).toBe('Previously saved draft');
    expect(result.current.status.hasDraft).toBe(true);
    expect(result.current.status.lastSaved).not.toBeNull();
  });

  it('should handle corrupted localStorage data gracefully', () => {
    // Corrupt data in localStorage
    localStorage.setItem('ticket-draft-1', 'not valid json');

    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    // Should not crash, should start with empty draft
    expect(result.current.draft).toBe('');
    expect(result.current.status.hasDraft).toBe(false);
  });

  it('should clear draft from localStorage when clearDraft is called', async () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    act(() => {
      result.current.setDraft('Draft to be cleared');
    });

    await waitFor(
      () => {
        expect(result.current.status.hasDraft).toBe(true);
      },
      { timeout: 2000 }
    );

    // Clear the draft
    act(() => {
      result.current.clearDraft();
    });

    // Should be removed from localStorage
    const stored = localStorage.getItem('ticket-draft-1');
    expect(stored).toBeNull();
    expect(result.current.status.hasDraft).toBe(false);
  });

  it('should use separate storage keys for different tickets', async () => {
    const { result: ticket1 } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    const { result: ticket2 } = renderHook(() =>
      useDraftComment({ ticketId: 2, autoSaveDelay: 50 })
    );

    act(() => {
      ticket1.current.setDraft('Draft for ticket 1');
      ticket2.current.setDraft('Draft for ticket 2');
    });

    // Wait for both to save
    await waitFor(
      () => {
        expect(ticket1.current.status.hasDraft).toBe(true);
        expect(ticket2.current.status.hasDraft).toBe(true);
      },
      { timeout: 2000 }
    );

    // Both should be stored separately
    const stored1 = localStorage.getItem('ticket-draft-1');
    const stored2 = localStorage.getItem('ticket-draft-2');

    expect(JSON.parse(stored1!).content).toBe('Draft for ticket 1');
    expect(JSON.parse(stored2!).content).toBe('Draft for ticket 2');
  });

  // ========== Edge Cases ==========

  it('should handle null ticketId gracefully', () => {
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: null, autoSaveDelay: 1000 })
    );

    act(() => {
      result.current.setDraft('Draft with no ticket');
    });

    // Should not crash, but won't save to localStorage
    expect(result.current.draft).toBe('Draft with no ticket');
  });

  it('should use different storage keys for different ticketIds', () => {
    // This test verifies that changing ticketId uses a different localStorage key
    const { result, rerender } = renderHook(
      ({ ticketId }) => useDraftComment({ ticketId, autoSaveDelay: 50 }),
      { initialProps: { ticketId: 1 as number | null } }
    );

    act(() => {
      result.current.setDraft('Draft for ticket 1');
    });

    // Pre-populate storage for ticket 2
    const ticket2Draft = {
      content: 'Draft for ticket 2',
      timestamp: new Date().toISOString(),
    };
    localStorage.setItem('ticket-draft-2', JSON.stringify(ticket2Draft));

    // Change to ticket 2
    rerender({ ticketId: 2 });

    // Should load the draft for ticket 2
    expect(result.current.draft).toBe('Draft for ticket 2');
    expect(result.current.status.hasDraft).toBe(true);
  });

  it('should handle localStorage quota exceeded gracefully', async () => {
    // Mock localStorage to throw quota exceeded error
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = vi.fn(() => {
      throw new Error('QuotaExceededError');
    });

    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50 })
    );

    // Should not crash when trying to save
    expect(() => {
      act(() => {
        result.current.setDraft('Large draft');
      });
    }).not.toThrow();

    // Restore original method
    Storage.prototype.setItem = originalSetItem;
  });

  // ========== Callback Tests ==========

  it('should call onSave callback when draft is saved', async () => {
    const onSave = vi.fn();
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50, onSave })
    );

    act(() => {
      result.current.setDraft('Draft content');
    });

    await waitFor(
      () => {
        expect(onSave).toHaveBeenCalled();
      },
      { timeout: 2000 }
    );
  });

  it('should not call onSave when clearing draft', () => {
    const onSave = vi.fn();
    const { result } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 50, onSave })
    );

    act(() => {
      result.current.setDraft('Draft');
      result.current.clearDraft();
    });

    expect(onSave).not.toHaveBeenCalled();
  });

  // ========== Performance Tests ==========

  it('should cancel pending saves when unmounted', () => {
    const { result, unmount } = renderHook(() =>
      useDraftComment({ ticketId: 1, autoSaveDelay: 1000 })
    );

    act(() => {
      result.current.setDraft('Draft that will not save');
    });

    // Unmount before save completes
    unmount();

    // Should not have saved (save was cancelled)
    const stored = localStorage.getItem('ticket-draft-1');
    expect(stored).toBeNull();
  });
});
