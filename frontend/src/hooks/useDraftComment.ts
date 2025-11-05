import { useState, useEffect, useCallback, useRef } from 'react';

interface UseDraftCommentOptions {
  ticketId: number | null;
  onSave?: () => void;
  autoSaveDelay?: number; // milliseconds
}

interface DraftStatus {
  isSaving: boolean;
  lastSaved: Date | null;
  hasDraft: boolean;
}

/**
 * Hook for managing auto-saved comment drafts in localStorage.
 * Prevents data loss by automatically saving drafts as the user types.
 *
 * Usage:
 * ```
 * const { draft, setDraft, clearDraft, status } = useDraftComment({
 *   ticketId: ticket.id,
 *   autoSaveDelay: 1000
 * });
 * ```
 */
export const useDraftComment = ({
  ticketId,
  onSave,
  autoSaveDelay = 1000,
}: UseDraftCommentOptions) => {
  const [draft, setDraftState] = useState<string>('');
  const [status, setStatus] = useState<DraftStatus>({
    isSaving: false,
    lastSaved: null,
    hasDraft: false,
  });

  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const storageKey = ticketId ? `ticket-draft-${ticketId}` : null;

  // Load draft from localStorage when ticketId changes
  // CRITICAL: Always reset state to prevent draft inheritance between tickets
  useEffect(() => {
    if (!storageKey) {
      // No storage key means we should reset to empty state
      setDraftState('');
      setStatus({
        isSaving: false,
        lastSaved: null,
        hasDraft: false,
      });
      return;
    }

    // CRITICAL: Always determine final state, then update once
    // This ensures tickets don't inherit each other's drafts
    let finalDraft = '';
    let finalStatus: DraftStatus = {
      isSaving: false,
      lastSaved: null,
      hasDraft: false,
    };

    try {
      const savedDraft = localStorage.getItem(storageKey);
      if (savedDraft) {
        const parsed = JSON.parse(savedDraft);
        finalDraft = parsed.content;
        finalStatus = {
          isSaving: false,
          lastSaved: new Date(parsed.timestamp),
          hasDraft: true,
        };
      }
    } catch (error) {
      console.error('Failed to load draft from localStorage:', error);
      // Keep empty state (already initialized above)
    }

    // Single state update to avoid batching issues
    setDraftState(finalDraft);
    setStatus(finalStatus);
  }, [storageKey]);

  // Auto-save draft to localStorage (debounced)
  useEffect(() => {
    if (!storageKey) return;

    // Clear existing timeout
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }

    // Handle empty draft: remove from localStorage to prevent resurrection
    if (!draft.trim()) {
      try {
        localStorage.removeItem(storageKey);
        setStatus({
          isSaving: false,
          lastSaved: null,
          hasDraft: false,
        });
      } catch (error) {
        console.error('Failed to remove draft from localStorage:', error);
      }
      return;
    }

    // Set saving indicator
    setStatus(prev => ({ ...prev, isSaving: true }));

    // Schedule save
    saveTimeoutRef.current = setTimeout(() => {
      try {
        const draftData = {
          content: draft,
          timestamp: new Date().toISOString(),
        };
        localStorage.setItem(storageKey, JSON.stringify(draftData));

        setStatus({
          isSaving: false,
          lastSaved: new Date(),
          hasDraft: true,
        });

        if (onSave) {
          onSave();
        }
      } catch (error) {
        console.error('Failed to save draft to localStorage:', error);
        setStatus(prev => ({ ...prev, isSaving: false }));
      }
    }, autoSaveDelay);

    // Cleanup timeout on unmount
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, [draft, storageKey, autoSaveDelay, onSave]);

  // Set draft value
  const setDraft = useCallback((value: string) => {
    setDraftState(value);
  }, []);

  // Clear draft from state and localStorage
  const clearDraft = useCallback(() => {
    setDraftState('');
    if (storageKey) {
      try {
        localStorage.removeItem(storageKey);
        setStatus({
          isSaving: false,
          lastSaved: null,
          hasDraft: false,
        });
      } catch (error) {
        console.error('Failed to clear draft from localStorage:', error);
      }
    }
  }, [storageKey]);

  return {
    draft,
    setDraft,
    clearDraft,
    status,
  };
};
