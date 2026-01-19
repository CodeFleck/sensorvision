import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MultiSelect, MultiSelectOption } from './MultiSelect';

// Helper to create mock options
const createMockOptions = (count: number = 5): MultiSelectOption[] =>
  Array.from({ length: count }, (_, i) => ({
    value: `option-${i + 1}`,
    label: `Option ${i + 1}`,
  }));

describe('MultiSelect', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render the component', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      expect(screen.getByTestId('multiselect-container')).toBeInTheDocument();
    });

    it('should display placeholder when nothing selected', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={[]}
          onChange={() => {}}
          placeholder="Select devices..."
        />
      );

      expect(screen.getByText('Select devices...')).toBeInTheDocument();
    });

    it('should display default placeholder when not specified', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      expect(screen.getByText('Select items...')).toBeInTheDocument();
    });

    it('should render trigger button with correct aria attributes', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      const trigger = screen.getByTestId('multiselect-trigger');
      expect(trigger).toHaveAttribute('aria-haspopup', 'listbox');
      expect(trigger).toHaveAttribute('aria-expanded', 'false');
    });
  });

  // ========== Selection Display Tests ==========

  describe('Selection Display', () => {
    it('should display selected items as tags', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2']}
          onChange={() => {}}
        />
      );

      expect(screen.getByTestId('selected-tag-option-1')).toBeInTheDocument();
      expect(screen.getByTestId('selected-tag-option-2')).toBeInTheDocument();
      expect(screen.getByText('Option 1')).toBeInTheDocument();
      expect(screen.getByText('Option 2')).toBeInTheDocument();
    });

    it('should show "+X more" when exceeding maxDisplayed', () => {
      const options = createMockOptions(10);
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2', 'option-3', 'option-4', 'option-5']}
          onChange={() => {}}
          maxDisplayed={3}
        />
      );

      expect(screen.getByTestId('more-count')).toHaveTextContent('+2 more');
    });

    it('should not show "+X more" when within maxDisplayed limit', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2']}
          onChange={() => {}}
          maxDisplayed={3}
        />
      );

      expect(screen.queryByTestId('more-count')).not.toBeInTheDocument();
    });

    it('should display remove button on each tag', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={['option-1']}
          onChange={() => {}}
        />
      );

      expect(screen.getByTestId('remove-tag-option-1')).toBeInTheDocument();
    });
  });

  // ========== Dropdown Interaction Tests ==========

  describe('Dropdown Interaction', () => {
    it('should open dropdown on click', async () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      const trigger = screen.getByTestId('multiselect-trigger');
      fireEvent.click(trigger);

      expect(screen.getByTestId('multiselect-dropdown')).toBeInTheDocument();
      expect(trigger).toHaveAttribute('aria-expanded', 'true');
    });

    it('should close dropdown on second click', async () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      const trigger = screen.getByTestId('multiselect-trigger');
      fireEvent.click(trigger);
      expect(screen.getByTestId('multiselect-dropdown')).toBeInTheDocument();

      fireEvent.click(trigger);
      expect(screen.queryByTestId('multiselect-dropdown')).not.toBeInTheDocument();
    });

    it('should display all options in dropdown', () => {
      const options = createMockOptions(3);
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('option-option-1')).toBeInTheDocument();
      expect(screen.getByTestId('option-option-2')).toBeInTheDocument();
      expect(screen.getByTestId('option-option-3')).toBeInTheDocument();
    });

    it('should have proper ARIA attributes on dropdown', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      const dropdown = screen.getByTestId('multiselect-dropdown');

      expect(dropdown).toHaveAttribute('role', 'listbox');
      expect(dropdown).toHaveAttribute('aria-multiselectable', 'true');
    });

    it('should have proper ARIA attributes on options', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={['option-1']} onChange={() => {}} />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      const selectedOption = screen.getByTestId('option-option-1');
      const unselectedOption = screen.getByTestId('option-option-2');

      expect(selectedOption).toHaveAttribute('role', 'option');
      expect(selectedOption).toHaveAttribute('aria-selected', 'true');
      expect(unselectedOption).toHaveAttribute('aria-selected', 'false');
    });
  });

  // ========== Selection/Deselection Tests ==========

  describe('Selection and Deselection', () => {
    it('should call onChange when selecting an option', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(<MultiSelect options={options} selected={[]} onChange={onChange} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.click(screen.getByTestId('option-option-1'));

      expect(onChange).toHaveBeenCalledWith(['option-1']);
    });

    it('should call onChange when deselecting an option', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2']}
          onChange={onChange}
        />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.click(screen.getByTestId('option-option-1'));

      expect(onChange).toHaveBeenCalledWith(['option-2']);
    });

    it('should call onChange when removing via tag X button', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2']}
          onChange={onChange}
        />
      );

      fireEvent.click(screen.getByTestId('remove-tag-option-1'));

      expect(onChange).toHaveBeenCalledWith(['option-2']);
    });
  });

  // ========== Clear All Tests ==========

  describe('Clear All', () => {
    it('should display clear all button when items are selected', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={['option-1']} onChange={() => {}} />
      );

      expect(screen.getByTestId('clear-all-button')).toBeInTheDocument();
    });

    it('should not display clear all button when nothing is selected', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      expect(screen.queryByTestId('clear-all-button')).not.toBeInTheDocument();
    });

    it('should call onChange with empty array when clearing all', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2', 'option-3']}
          onChange={onChange}
        />
      );

      fireEvent.click(screen.getByTestId('clear-all-button'));

      expect(onChange).toHaveBeenCalledWith([]);
    });

    it('should have accessible label on clear all button', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={['option-1']} onChange={() => {}} />
      );

      expect(screen.getByTestId('clear-all-button')).toHaveAttribute(
        'aria-label',
        'Clear all selections'
      );
    });
  });

  // ========== Select All Tests ==========

  describe('Select All', () => {
    it('should display select all button in dropdown', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('select-all-button')).toBeInTheDocument();
    });

    it('should call onChange with all options when selecting all', () => {
      const options = createMockOptions(3);
      const onChange = vi.fn();
      render(<MultiSelect options={options} selected={[]} onChange={onChange} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.click(screen.getByTestId('select-all-button'));

      expect(onChange).toHaveBeenCalledWith(['option-1', 'option-2', 'option-3']);
    });

    it('should display selection count in dropdown', () => {
      const options = createMockOptions(5);
      render(
        <MultiSelect
          options={options}
          selected={['option-1', 'option-2']}
          onChange={() => {}}
        />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('selection-count')).toHaveTextContent('2 of 5 selected');
    });
  });

  // ========== Search/Filter Tests ==========

  describe('Search and Filter', () => {
    it('should display search input in dropdown', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('search-input')).toBeInTheDocument();
    });

    it('should filter options based on search input', async () => {
      const options = [
        { value: 'apple', label: 'Apple' },
        { value: 'banana', label: 'Banana' },
        { value: 'cherry', label: 'Cherry' },
      ];
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      const searchInput = screen.getByTestId('search-input');

      fireEvent.change(searchInput, { target: { value: 'an' } });

      expect(screen.getByTestId('option-banana')).toBeInTheDocument();
      expect(screen.queryByTestId('option-apple')).not.toBeInTheDocument();
      expect(screen.queryByTestId('option-cherry')).not.toBeInTheDocument();
    });

    it('should show "No options found" when search has no matches', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.change(screen.getByTestId('search-input'), {
        target: { value: 'xyz' },
      });

      expect(screen.getByTestId('no-options')).toHaveTextContent('No options found');
    });

    it('should be case-insensitive in search', () => {
      const options = [
        { value: 'apple', label: 'Apple' },
        { value: 'banana', label: 'Banana' },
      ];
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.change(screen.getByTestId('search-input'), {
        target: { value: 'APPLE' },
      });

      expect(screen.getByTestId('option-apple')).toBeInTheDocument();
    });

    it('should have accessible label on search input', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('search-input')).toHaveAttribute(
        'aria-label',
        'Search options'
      );
    });

    it('should use custom search placeholder', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={[]}
          onChange={() => {}}
          searchPlaceholder="Find devices..."
        />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('search-input')).toHaveAttribute(
        'placeholder',
        'Find devices...'
      );
    });
  });

  // ========== Keyboard Navigation Tests ==========

  describe('Keyboard Navigation', () => {
    it('should close dropdown on Escape key', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      expect(screen.getByTestId('multiselect-dropdown')).toBeInTheDocument();

      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'Escape',
      });

      expect(screen.queryByTestId('multiselect-dropdown')).not.toBeInTheDocument();
    });

    it('should open dropdown on ArrowDown when closed', () => {
      const options = createMockOptions();
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });

      expect(screen.getByTestId('multiselect-dropdown')).toBeInTheDocument();
    });

    it('should navigate options with ArrowDown', () => {
      const options = createMockOptions(3);
      const { container } = render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // Navigate down
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });

      // First option should be focused (index 0)
      const firstOption = screen.getByTestId('option-option-1');
      expect(firstOption).toHaveClass('ring-2');
    });

    it('should navigate options with ArrowUp', () => {
      const options = createMockOptions(3);
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // Navigate down twice then up
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowUp',
      });

      // Should be on first option (index 0)
      const firstOption = screen.getByTestId('option-option-1');
      expect(firstOption).toHaveClass('ring-2');
    });

    it('should select focused option on Enter', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(<MultiSelect options={options} selected={[]} onChange={onChange} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // Navigate to first option
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });

      // Press Enter to select
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'Enter',
      });

      expect(onChange).toHaveBeenCalledWith(['option-1']);
    });

    it('should jump to first option on Home key', () => {
      const options = createMockOptions(5);
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // Navigate down multiple times
      for (let i = 0; i < 3; i++) {
        fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
          key: 'ArrowDown',
        });
      }

      // Press Home
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'Home',
      });

      const firstOption = screen.getByTestId('option-option-1');
      expect(firstOption).toHaveClass('ring-2');
    });

    it('should jump to last option on End key', () => {
      const options = createMockOptions(5);
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // Press End
      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'End',
      });

      const lastOption = screen.getByTestId('option-option-5');
      expect(lastOption).toHaveClass('ring-2');
    });
  });

  // ========== Disabled State Tests ==========

  describe('Disabled State', () => {
    it('should not open dropdown when disabled', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} disabled />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.queryByTestId('multiselect-dropdown')).not.toBeInTheDocument();
    });

    it('should have aria-disabled attribute on trigger', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} disabled />
      );

      expect(screen.getByTestId('multiselect-trigger')).toHaveAttribute('aria-disabled', 'true');
    });

    it('should apply disabled styling', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} disabled />
      );

      expect(screen.getByTestId('multiselect-trigger')).toHaveClass('opacity-50');
    });

    it('should not respond to keyboard events when disabled', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} disabled />
      );

      fireEvent.keyDown(screen.getByTestId('multiselect-container'), {
        key: 'ArrowDown',
      });

      expect(screen.queryByTestId('multiselect-dropdown')).not.toBeInTheDocument();
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle empty options array', () => {
      render(<MultiSelect options={[]} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      expect(screen.getByTestId('no-options')).toHaveTextContent('No options found');
    });

    it('should handle single option', () => {
      const options = [{ value: 'only', label: 'Only Option' }];
      const onChange = vi.fn();
      render(<MultiSelect options={options} selected={[]} onChange={onChange} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.click(screen.getByTestId('option-only'));

      expect(onChange).toHaveBeenCalledWith(['only']);
    });

    it('should handle selected value that does not exist in options', () => {
      const options = createMockOptions(3);
      // Trying to display a selected value that is not in options
      render(
        <MultiSelect
          options={options}
          selected={['non-existent']}
          onChange={() => {}}
        />
      );

      // Should not crash, but also should not show the non-existent value
      expect(screen.queryByText('non-existent')).not.toBeInTheDocument();
    });

    it('should handle very long option labels', () => {
      const options = [
        {
          value: 'long',
          label: 'This is a very long option label that might overflow the container',
        },
      ];
      render(<MultiSelect options={options} selected={['long']} onChange={() => {}} />);

      // Should render without crashing and use truncation
      expect(screen.getByTestId('multiselect-container')).toBeInTheDocument();
    });

    it('should handle special characters in search', () => {
      const options = [
        { value: 'special', label: 'Option (with) [special] {chars}' },
      ];
      render(<MultiSelect options={options} selected={[]} onChange={() => {}} />);

      fireEvent.click(screen.getByTestId('multiselect-trigger'));
      fireEvent.change(screen.getByTestId('search-input'), {
        target: { value: '(with)' },
      });

      expect(screen.getByTestId('option-special')).toBeInTheDocument();
    });

    it('should handle rapid clicking without breaking', () => {
      const options = createMockOptions();
      const onChange = vi.fn();
      render(<MultiSelect options={options} selected={[]} onChange={onChange} />);

      const trigger = screen.getByTestId('multiselect-trigger');

      // Rapid clicks
      for (let i = 0; i < 10; i++) {
        fireEvent.click(trigger);
      }

      // Should not crash
      expect(screen.getByTestId('multiselect-container')).toBeInTheDocument();
    });
  });

  // ========== Accessibility Tests ==========

  describe('Accessibility', () => {
    it('should support custom aria-label', () => {
      const options = createMockOptions();
      render(
        <MultiSelect
          options={options}
          selected={[]}
          onChange={() => {}}
          aria-label="Select your devices"
        />
      );

      expect(screen.getByTestId('multiselect-trigger')).toHaveAttribute(
        'aria-label',
        'Select your devices'
      );
    });

    it('should have aria-label on remove buttons', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={['option-1']} onChange={() => {}} />
      );

      expect(screen.getByTestId('remove-tag-option-1')).toHaveAttribute(
        'aria-label',
        'Remove Option 1'
      );
    });

    it('should hide decorative icons from screen readers', () => {
      const options = createMockOptions();
      render(
        <MultiSelect options={options} selected={['option-1']} onChange={() => {}} />
      );

      fireEvent.click(screen.getByTestId('multiselect-trigger'));

      // ChevronDown should have aria-hidden
      const { container } = render(
        <MultiSelect options={options} selected={[]} onChange={() => {}} />
      );
      const chevron = container.querySelector('[aria-hidden="true"]');
      expect(chevron).toBeInTheDocument();
    });
  });
});
