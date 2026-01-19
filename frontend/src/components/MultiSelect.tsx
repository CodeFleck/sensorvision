import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { Check, ChevronDown, X, Search } from 'lucide-react';
import { clsx } from 'clsx';

export interface MultiSelectOption {
  value: string;
  label: string;
}

export interface MultiSelectProps {
  options: MultiSelectOption[];
  selected: string[];
  onChange: (selected: string[]) => void;
  placeholder?: string;
  searchPlaceholder?: string;
  maxDisplayed?: number;
  className?: string;
  disabled?: boolean;
  'aria-label'?: string;
}

export const MultiSelect = ({
  options,
  selected,
  onChange,
  placeholder = 'Select items...',
  searchPlaceholder = 'Search...',
  maxDisplayed = 3,
  className,
  disabled = false,
  'aria-label': ariaLabel = 'Select options',
}: MultiSelectProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const listboxId = useRef(`multiselect-listbox-${Math.random().toString(36).slice(2, 9)}`).current;

  // Memoize filtered options
  const filteredOptions = useMemo(
    () => options.filter((opt) => opt.label.toLowerCase().includes(search.toLowerCase())),
    [options, search]
  );

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearch('');
        setFocusedIndex(-1);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Focus search input when dropdown opens
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
      setFocusedIndex(-1);
    }
  }, [isOpen]);

  // Scroll focused option into view
  useEffect(() => {
    if (focusedIndex >= 0 && listRef.current) {
      const focusedElement = listRef.current.querySelector(`[data-index="${focusedIndex}"]`);
      focusedElement?.scrollIntoView({ block: 'nearest' });
    }
  }, [focusedIndex]);

  const toggleOption = useCallback((value: string) => {
    if (selected.includes(value)) {
      onChange(selected.filter((v) => v !== value));
    } else {
      onChange([...selected, value]);
    }
  }, [selected, onChange]);

  const removeOption = (value: string, e: React.MouseEvent) => {
    e.stopPropagation();
    onChange(selected.filter((v) => v !== value));
  };

  const clearAll = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange([]);
  };

  const selectAll = () => {
    onChange(options.map((opt) => opt.value));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (disabled) return;

    switch (e.key) {
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        setSearch('');
        setFocusedIndex(-1);
        break;
      case 'ArrowDown':
        e.preventDefault();
        if (!isOpen) {
          setIsOpen(true);
        } else {
          setFocusedIndex((prev) =>
            prev < filteredOptions.length - 1 ? prev + 1 : prev
          );
        }
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (isOpen) {
          setFocusedIndex((prev) => (prev > 0 ? prev - 1 : 0));
        }
        break;
      case 'Enter':
      case ' ':
        if (isOpen && focusedIndex >= 0 && focusedIndex < filteredOptions.length) {
          e.preventDefault();
          toggleOption(filteredOptions[focusedIndex].value);
        } else if (!isOpen && e.key === 'Enter') {
          e.preventDefault();
          setIsOpen(true);
        }
        break;
      case 'Home':
        if (isOpen) {
          e.preventDefault();
          setFocusedIndex(0);
        }
        break;
      case 'End':
        if (isOpen) {
          e.preventDefault();
          setFocusedIndex(filteredOptions.length - 1);
        }
        break;
    }
  };

  const getSelectedLabels = () => {
    return selected
      .map((value) => options.find((opt) => opt.value === value)?.label)
      .filter(Boolean) as string[];
  };

  const selectedLabels = getSelectedLabels();
  const displayedLabels = selectedLabels.slice(0, maxDisplayed);
  const remainingCount = selectedLabels.length - maxDisplayed;
  const focusedOptionId = focusedIndex >= 0 ? `${listboxId}-option-${focusedIndex}` : undefined;

  return (
    <div
      ref={containerRef}
      className={clsx('relative', className)}
      onKeyDown={handleKeyDown}
      data-testid="multiselect-container"
    >
      {/* Trigger */}
      <div
        role="combobox"
        tabIndex={disabled ? -1 : 0}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        aria-disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={ariaLabel}
        aria-controls={isOpen ? listboxId : undefined}
        aria-activedescendant={focusedOptionId}
        data-testid="multiselect-trigger"
        className={clsx(
          'w-full min-h-[42px] px-3 py-2 text-left cursor-pointer',
          'border border-default rounded-lg bg-primary',
          'flex items-center gap-2 flex-wrap',
          'hover:border-gray-400 dark:hover:border-gray-500',
          'focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-violet-500',
          'transition-colors',
          disabled && 'opacity-50 cursor-not-allowed pointer-events-none'
        )}
      >
        <div className="flex-1 flex items-center gap-2 flex-wrap min-h-[24px]">
          {selected.length === 0 ? (
            <span className="text-gray-400 dark:text-gray-500 text-sm">{placeholder}</span>
          ) : (
            <>
              {displayedLabels.map((label, idx) => {
                const value = selected[idx];
                return (
                  <span
                    key={value}
                    className={clsx(
                      'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-sm',
                      'bg-violet-100 dark:bg-violet-900/40 text-violet-700 dark:text-violet-300'
                    )}
                    data-testid={`selected-tag-${value}`}
                  >
                    {label}
                    <button
                      type="button"
                      onClick={(e) => removeOption(value, e)}
                      disabled={disabled}
                      aria-label={`Remove ${label}`}
                      className="hover:bg-violet-200 dark:hover:bg-violet-800 rounded p-0.5 transition-colors"
                      data-testid={`remove-tag-${value}`}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                );
              })}
              {remainingCount > 0 && (
                <span className="text-sm text-secondary" data-testid="more-count">
                  +{remainingCount} more
                </span>
              )}
            </>
          )}
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          {selected.length > 0 && (
            <button
              type="button"
              onClick={clearAll}
              disabled={disabled}
              className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
              title="Clear all"
              aria-label="Clear all selections"
              data-testid="clear-all-button"
            >
              <X className="h-4 w-4 text-secondary" />
            </button>
          )}
          <ChevronDown
            className={clsx(
              'h-4 w-4 text-secondary transition-transform',
              isOpen && 'rotate-180'
            )}
            aria-hidden="true"
          />
        </div>
      </div>

      {/* Dropdown */}
      {isOpen && (
        <div
          id={listboxId}
          role="listbox"
          aria-multiselectable="true"
          aria-label={ariaLabel}
          data-testid="multiselect-dropdown"
          className={clsx(
            'absolute z-50 mt-1 w-full',
            'bg-primary border border-default rounded-lg shadow-lg',
            'max-h-64 overflow-hidden flex flex-col'
          )}
        >
          {/* Search Input */}
          <div className="p-2 border-b border-default">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-secondary" aria-hidden="true" />
              <input
                ref={inputRef}
                type="text"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setFocusedIndex(-1);
                }}
                placeholder={searchPlaceholder}
                aria-label="Search options"
                data-testid="search-input"
                className={clsx(
                  'w-full pl-8 pr-3 py-1.5 text-sm',
                  'border border-default rounded-md bg-primary text-primary',
                  'focus:outline-none focus:ring-1 focus:ring-violet-500 focus:border-violet-500'
                )}
              />
            </div>
          </div>

          {/* Actions */}
          <div className="px-2 py-1.5 border-b border-default flex items-center justify-between text-xs">
            <button
              type="button"
              onClick={selectAll}
              className="text-violet-600 hover:text-violet-700 dark:text-violet-400 dark:hover:text-violet-300 font-medium"
              data-testid="select-all-button"
            >
              Select all
            </button>
            <span className="text-secondary" data-testid="selection-count">
              {selected.length} of {options.length} selected
            </span>
          </div>

          {/* Options List */}
          <div ref={listRef} className="overflow-y-auto flex-1" data-testid="options-list">
            {filteredOptions.length === 0 ? (
              <div className="p-4 text-center text-sm text-secondary" data-testid="no-options">
                No options found
              </div>
            ) : (
              filteredOptions.map((option, index) => {
                const isSelected = selected.includes(option.value);
                const isFocused = index === focusedIndex;
                return (
                  <button
                    key={option.value}
                    id={`${listboxId}-option-${index}`}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    data-index={index}
                    onClick={() => toggleOption(option.value)}
                    data-testid={`option-${option.value}`}
                    className={clsx(
                      'w-full px-3 py-2 text-left text-sm',
                      'flex items-center gap-3',
                      'hover:bg-violet-50 dark:hover:bg-violet-900/20',
                      'transition-colors',
                      isSelected && 'bg-violet-50/50 dark:bg-violet-900/10',
                      isFocused && 'ring-2 ring-inset ring-violet-500'
                    )}
                  >
                    <div
                      className={clsx(
                        'w-4 h-4 rounded border flex items-center justify-center flex-shrink-0',
                        'transition-colors',
                        isSelected
                          ? 'bg-violet-600 border-violet-600'
                          : 'border-gray-300 dark:border-gray-600'
                      )}
                      aria-hidden="true"
                    >
                      {isSelected && <Check className="h-3 w-3 text-white" />}
                    </div>
                    <span className="text-primary truncate">{option.label}</span>
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default MultiSelect;
