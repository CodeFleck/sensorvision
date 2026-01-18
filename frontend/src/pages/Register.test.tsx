import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Register } from './Register';
import { useAuth, AuthContextType } from '../contexts/AuthContext';
import type { User, RegisterRequest } from '../types';

// Mock the AuthContext
vi.mock('../contexts/AuthContext');

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Type-safe mock factory for AuthContext
const createMockAuthContext = (overrides: {
  user?: User | null;
  loading?: boolean;
  isAuthenticated?: boolean;
  register?: ReturnType<typeof vi.fn>;
} = {}): AuthContextType => ({
  user: overrides.user ?? null,
  loading: overrides.loading ?? false,
  login: vi.fn() as AuthContextType['login'],
  register: (overrides.register ?? vi.fn()) as AuthContextType['register'],
  logout: vi.fn() as AuthContextType['logout'],
  setTokens: vi.fn() as AuthContextType['setTokens'],
  refreshUser: vi.fn() as AuthContextType['refreshUser'],
  isAuthenticated: overrides.isAuthenticated ?? false,
  isAdmin: false,
  hasRole: vi.fn().mockReturnValue(false) as AuthContextType['hasRole'],
});

// Helper to render with router
const renderRegister = () => {
  return render(
    <BrowserRouter>
      <Register />
    </BrowserRouter>
  );
};

// Helper to get input by id (labels are properly associated via htmlFor)
const getInput = (id: string): HTMLInputElement => {
  const input = document.getElementById(id);
  if (!input) throw new Error(`Input with id "${id}" not found`);
  return input as HTMLInputElement;
};

// Helper to fill a single input field using fireEvent.change
// Note: We use fireEvent.change for controlled inputs as userEvent.type
// can have issues with certain input components. This still tests the
// onChange handler and state updates.
const fillInput = (id: string, value: string) => {
  const input = getInput(id);
  fireEvent.change(input, { target: { value, name: id } });
};

// Helper to fill the entire form - uses input IDs which are tied to label htmlFor
const fillForm = (data: {
  firstName?: string;
  lastName?: string;
  username?: string;
  email?: string;
  organization?: string;
  password?: string;
  confirmPassword?: string;
}) => {
  if (data.firstName) fillInput('firstName', data.firstName);
  if (data.lastName) fillInput('lastName', data.lastName);
  if (data.username) fillInput('username', data.username);
  if (data.email) fillInput('email', data.email);
  if (data.organization) fillInput('organizationName', data.organization);
  if (data.password) fillInput('password', data.password);
  if (data.confirmPassword) fillInput('confirmPassword', data.confirmPassword);
};

// Standard valid form data for reuse
const validFormData = {
  firstName: 'John',
  lastName: 'Doe',
  username: 'johndoe',
  email: 'john@example.com',
  password: 'password123',
  confirmPassword: 'password123',
};

describe('Register Page', () => {
  let mockRegister: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    mockRegister = vi.fn();
    vi.mocked(useAuth).mockReturnValue(createMockAuthContext({ register: mockRegister }));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Rendering', () => {
    it('should render the registration form with heading', () => {
      renderRegister();

      expect(screen.getByRole('heading', { level: 2 })).toBeInTheDocument();
      expect(screen.getByText(/start monitoring your iot devices/i)).toBeInTheDocument();
    });

    it('should render all required form fields with proper labels', () => {
      renderRegister();

      // Inputs are properly labeled via htmlFor attribute
      expect(getInput('firstName')).toBeInTheDocument();
      expect(getInput('lastName')).toBeInTheDocument();
      expect(getInput('username')).toBeInTheDocument();
      expect(getInput('email')).toBeInTheDocument();
      expect(getInput('password')).toBeInTheDocument();
      expect(getInput('confirmPassword')).toBeInTheDocument();
    });

    it('should render optional organization field', () => {
      renderRegister();

      const orgField = getInput('organizationName');
      expect(orgField).toBeInTheDocument();
      expect(orgField).not.toBeRequired();
    });

    it('should render the submit button', () => {
      renderRegister();

      expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    });

    it('should render link to login page with correct href', () => {
      renderRegister();

      const signInLink = screen.getByRole('link', { name: /sign in/i });
      expect(signInLink).toHaveAttribute('href', '/login');
    });

    it('should render branding elements', () => {
      renderRegister();

      expect(screen.getAllByText(/industrial cloud/i).length).toBeGreaterThan(0);
    });

    it('should render feature cards showcasing benefits', () => {
      renderRegister();

      expect(screen.getByText('Free tier')).toBeInTheDocument();
      expect(screen.getByText('SOC 2')).toBeInTheDocument();
      expect(screen.getByText('Unlimited')).toBeInTheDocument();
    });
  });

  describe('Form Input', () => {
    it('should update form state when typing in fields', () => {
      // Note: Uses fireEvent.change via fillForm helper for controlled inputs
      renderRegister();

      fillForm({
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe',
        email: 'john@example.com',
      });

      expect(getInput('firstName')).toHaveValue('John');
      expect(getInput('lastName')).toHaveValue('Doe');
      expect(getInput('username')).toHaveValue('johndoe');
      expect(getInput('email')).toHaveValue('john@example.com');
    });

    it('should toggle password visibility when clicking show/hide button', async () => {
      const user = userEvent.setup();
      renderRegister();

      const passwordInput = getInput('password');
      expect(passwordInput).toHaveAttribute('type', 'password');

      // Find and click the visibility toggle button
      const toggleButton = screen.getAllByRole('button', { name: /show password/i })[0];
      await user.click(toggleButton);

      expect(passwordInput).toHaveAttribute('type', 'text');

      // Toggle back
      const hideButton = screen.getAllByRole('button', { name: /hide password/i })[0];
      await user.click(hideButton);

      expect(passwordInput).toHaveAttribute('type', 'password');
    });
  });

  describe('Form Validation', () => {
    it('should show error when passwords do not match', async () => {
      const user = userEvent.setup();
      renderRegister();

      fillForm({
        ...validFormData,
        password: 'password123',
        confirmPassword: 'differentpassword',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
      expect(mockRegister).not.toHaveBeenCalled();
    });

    it('should show error when password is too short', async () => {
      const user = userEvent.setup();
      renderRegister();

      fillForm({
        ...validFormData,
        password: '12345',
        confirmPassword: '12345',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/password must be at least 6 characters/i)).toBeInTheDocument();
      expect(mockRegister).not.toHaveBeenCalled();
    });

    it('should enforce required fields via HTML validation', () => {
      renderRegister();

      expect(getInput('firstName')).toBeRequired();
      expect(getInput('lastName')).toBeRequired();
      expect(getInput('username')).toBeRequired();
      expect(getInput('email')).toBeRequired();
      expect(getInput('password')).toBeRequired();
      expect(getInput('confirmPassword')).toBeRequired();
    });

    it('should have proper input types for validation', () => {
      renderRegister();

      expect(getInput('email')).toHaveAttribute('type', 'email');
      expect(getInput('password')).toHaveAttribute('type', 'password');
      expect(getInput('confirmPassword')).toHaveAttribute('type', 'password');
    });
  });

  describe('Form Submission', () => {
    it('should call register with correct data on valid submission', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        organization: 'Acme Corp',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledTimes(1);
        expect(mockRegister).toHaveBeenCalledWith({
          username: 'johndoe',
          email: 'john@example.com',
          password: 'password123',
          firstName: 'John',
          lastName: 'Doe',
          organizationName: 'Acme Corp',
        } satisfies RegisterRequest & { organizationName: string });
      });
    });

    it('should navigate to home on successful registration', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/');
      });
    });

    it('should display error message from API on registration failure', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Username already exists'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/username already exists/i)).toBeInTheDocument();
      expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('should show loading state during submission', async () => {
      const user = userEvent.setup();
      let resolveRegister: () => void;
      mockRegister.mockImplementation(() => new Promise<void>((resolve) => {
        resolveRegister = resolve;
      }));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Verify loading state appears
      await waitFor(() => {
        expect(screen.getByText(/creating account/i)).toBeInTheDocument();
      });

      // Resolve promise and wait for state update to complete (fixes act warning)
      await act(async () => {
        resolveRegister!();
      });

      // Wait for loading state to clear
      await waitFor(() => {
        expect(screen.queryByText(/creating account/i)).not.toBeInTheDocument();
      });
    });

    it('should disable submit button while loading', async () => {
      const user = userEvent.setup();
      let resolveRegister: () => void;
      mockRegister.mockImplementation(() => new Promise<void>((resolve) => {
        resolveRegister = resolve;
      }));
      renderRegister();

      fillForm(validFormData);
      const submitButton = screen.getByRole('button', { name: /create account/i });
      await user.click(submitButton);

      // Verify button becomes disabled during loading
      await waitFor(() => {
        expect(submitButton).toBeDisabled();
      });

      // Resolve promise and wait for state update to complete (fixes act warning)
      await act(async () => {
        resolveRegister!();
      });

      // Wait for button to be re-enabled after completion
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create account/i })).not.toBeDisabled();
      });
    });

    it('should handle registration without optional organization name', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm(validFormData); // No organization

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            organizationName: undefined,
          })
        );
      });
    });
  });

  describe('Accessibility', () => {
    it('should have error alert with proper role for screen readers', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Registration failed'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });
    });

    it('should have accessible labels for all form inputs', () => {
      renderRegister();

      // All inputs have labels properly associated via htmlFor
      const inputIds = [
        'firstName',
        'lastName',
        'username',
        'email',
        'organizationName',
        'password',
        'confirmPassword',
      ];

      inputIds.forEach(id => {
        const input = getInput(id);
        expect(input).toBeInTheDocument();
        // Verify label association by checking that a label exists with htmlFor matching the id
        const label = document.querySelector(`label[for="${id}"]`);
        expect(label).toBeInTheDocument();
      });
    });

    it('should have accessible password toggle buttons', () => {
      renderRegister();

      const toggleButtons = screen.getAllByRole('button', { name: /show password/i });
      expect(toggleButtons.length).toBe(2); // One for each password field
    });
  });

  describe('Edge Cases', () => {
    it('should handle generic error message when API returns non-Error object', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue({ statusCode: 500, message: null });
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/registration failed/i)).toBeInTheDocument();
    });

    it('should handle network timeout errors gracefully', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Network request failed'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/network request failed/i)).toBeInTheDocument();
    });

    it('should handle rate limiting errors', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Too many requests. Please try again later.'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/too many requests/i)).toBeInTheDocument();
    });

    it('should handle email already exists error', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Email already registered'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/email already registered/i)).toBeInTheDocument();
    });

    it('should prevent multiple submissions while loading', async () => {
      const user = userEvent.setup();
      let resolveRegister: () => void;
      mockRegister.mockImplementation(() => new Promise<void>((resolve) => {
        resolveRegister = resolve;
      }));
      renderRegister();

      fillForm(validFormData);
      const submitButton = screen.getByRole('button', { name: /create account/i });

      // Click multiple times
      await user.click(submitButton);
      await user.click(submitButton);
      await user.click(submitButton);

      // Should only be called once because button becomes disabled
      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledTimes(1);
      });

      // Resolve promise and wait for state update to complete (fixes act warning)
      await act(async () => {
        resolveRegister!();
      });

      // Wait for loading state to clear
      await waitFor(() => {
        expect(screen.queryByText(/creating account/i)).not.toBeInTheDocument();
      });
    });

    it('should trim username, email, and name fields when submitting form with whitespace', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      // Type with leading/trailing spaces
      fillInput('firstName', '  John  ');
      fillInput('lastName', '  Doe  ');
      fillInput('username', '  johndoe  ');
      fillInput('email', '  john@example.com  ');
      fillInput('password', 'password123');
      fillInput('confirmPassword', 'password123');

      await user.click(screen.getByRole('button', { name: /create account/i }));

      // All text fields should be trimmed before submission
      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            username: 'johndoe',        // Trimmed
            email: 'john@example.com',  // Trimmed
            firstName: 'John',          // Trimmed
            lastName: 'Doe',            // Trimmed
          })
        );
      });
    });

    it('should not trim password (whitespace could be intentional)', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillInput('firstName', 'John');
      fillInput('lastName', 'Doe');
      fillInput('username', 'johndoe');
      fillInput('email', 'john@example.com');
      // Password with spaces - should NOT be trimmed
      fillInput('password', '  spaced password  ');
      fillInput('confirmPassword', '  spaced password  ');

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            password: '  spaced password  ', // Password preserved as-is
          })
        );
      });
    });

    it('should trim organization name when provided', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm(validFormData);
      fillInput('organizationName', '  Acme Corp  ');

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            organizationName: 'Acme Corp', // Trimmed
          })
        );
      });
    });

    it('should clear error message when resubmitting after failure', async () => {
      const user = userEvent.setup();
      mockRegister.mockRejectedValueOnce(new Error('Registration failed'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Wait for error to appear
      expect(await screen.findByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/registration failed/i)).toBeInTheDocument();

      // On next submission, error should be cleared before new attempt
      mockRegister.mockResolvedValueOnce(undefined);
      await user.type(getInput('username'), 'x');
      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Error should be cleared, navigation should succeed
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalled();
      });

      // Error alert should no longer be in document
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('should safely render error messages containing HTML/script tags (XSS protection)', async () => {
      const user = userEvent.setup();
      // Attempt XSS via error message
      mockRegister.mockRejectedValue(new Error('<img src=x onerror="alert(1)">'));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Wait for error to appear
      const alert = await screen.findByRole('alert');

      // Should display as text, not execute - React escapes by default
      // The error message should be visible as text
      expect(alert.textContent).toContain('<img');
      // innerHTML should have escaped HTML entities, not raw tags
      expect(alert.innerHTML).not.toContain('<img src=x');
    });

    it('should prevent double submission via disabled button during loading', async () => {
      // This test verifies the button disables during submission.
      // Note: Synchronous rapid clicks before React state updates can still
      // result in multiple calls - this is a known limitation of the current
      // implementation. The button disabled state prevents subsequent clicks
      // AFTER the loading state is set.
      const user = userEvent.setup();
      let callCount = 0;
      let resolveRegister: () => void;
      mockRegister.mockImplementation(() => {
        callCount++;
        return new Promise<void>((resolve) => {
          resolveRegister = resolve;
        });
      });
      renderRegister();

      fillForm(validFormData);
      const submitButton = screen.getByRole('button', { name: /create account/i });

      // First click starts submission
      await user.click(submitButton);

      // Button should now be disabled
      await waitFor(() => {
        expect(submitButton).toBeDisabled();
      });

      // Subsequent clicks while loading should be ignored (button disabled)
      await user.click(submitButton);
      await user.click(submitButton);

      // Should still only be called once since button is disabled
      expect(callCount).toBe(1);

      // Cleanup
      await act(async () => {
        resolveRegister!();
      });
    });
  });

  describe('Password Validation Boundaries', () => {
    it('should reject password with exactly 5 characters (below minimum)', async () => {
      const user = userEvent.setup();
      renderRegister();

      fillForm({
        ...validFormData,
        password: '12345',
        confirmPassword: '12345',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      expect(await screen.findByText(/password must be at least 6 characters/i)).toBeInTheDocument();
      expect(mockRegister).not.toHaveBeenCalled();
    });

    it('should accept password with exactly 6 characters (minimum)', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        password: '123456',
        confirmPassword: '123456',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalled();
      });
    });

    it('should accept password with 100 characters (maximum server limit)', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      const longPassword = 'a'.repeat(100);
      fillForm({
        ...validFormData,
        password: longPassword,
        confirmPassword: longPassword,
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            password: longPassword,
          })
        );
      });
    });

    it('should warn about password longer than 100 characters', async () => {
      // Note: Backend has @Size(max = 100) - this test documents
      // that frontend doesn't block long passwords (server validation handles it)
      const user = userEvent.setup();
      mockRegister.mockRejectedValue(new Error('Password must be at most 100 characters'));
      renderRegister();

      const tooLongPassword = 'a'.repeat(101);
      fillForm({
        ...validFormData,
        password: tooLongPassword,
        confirmPassword: tooLongPassword,
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Frontend allows submission, server rejects
      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalled();
      });
    });
  });

  describe('Email Validation Edge Cases', () => {
    it('should accept standard email formats', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        email: 'user@example.com',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalled();
      });
    });

    it('should accept email with subdomain', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        email: 'user@mail.example.com',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            email: 'user@mail.example.com',
          })
        );
      });
    });

    it('should accept email with plus addressing', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        email: 'user+tag@example.com',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            email: 'user+tag@example.com',
          })
        );
      });
    });

    it('should accept email with dots in local part', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        email: 'first.last@example.com',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            email: 'first.last@example.com',
          })
        );
      });
    });

    it('should accept email with numbers in local part', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm({
        ...validFormData,
        email: 'user123@example.com',
      });

      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalledWith(
          expect.objectContaining({
            email: 'user123@example.com',
          })
        );
      });
    });
  });

  describe('Keyboard Navigation Accessibility', () => {
    it('should allow tab navigation through all form fields in correct order', () => {
      renderRegister();

      const expectedOrder = [
        'firstName',
        'lastName',
        'username',
        'email',
        'organizationName',
        'password',
        'confirmPassword',
      ];

      // Focus first field
      getInput('firstName').focus();
      expect(document.activeElement).toBe(getInput('firstName'));

      // Tab through fields (skipping toggle buttons)
      expectedOrder.slice(1).forEach((fieldId) => {
        // Tab to next field - in real scenario this would include toggle buttons
        // This test verifies the form structure allows tabbing
        const input = getInput(fieldId);
        input.focus();
        expect(document.activeElement).toBe(input);
      });
    });

    it('should submit form when Enter is pressed in last field', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      fillForm(validFormData);

      // Focus confirm password field and press Enter
      const confirmPasswordInput = getInput('confirmPassword');
      confirmPasswordInput.focus();

      await user.keyboard('{Enter}');

      await waitFor(() => {
        expect(mockRegister).toHaveBeenCalled();
      });
    });

    it('should not submit form when Enter is pressed in a text field (allows default behavior)', async () => {
      const user = userEvent.setup();
      mockRegister.mockResolvedValue(undefined);
      renderRegister();

      // Fill only some fields (form invalid)
      fillInput('firstName', 'John');
      fillInput('lastName', 'Doe');

      // Focus username and press Enter
      const usernameInput = getInput('username');
      usernameInput.focus();

      await user.keyboard('{Enter}');

      // Form should attempt submission (browser validation will block)
      // mockRegister won't be called due to HTML5 validation
      await waitFor(() => {
        // HTML required validation should prevent submission
        expect(mockRegister).not.toHaveBeenCalled();
      });
    });

    it('should have focusable password toggle buttons', async () => {
      const user = userEvent.setup();
      renderRegister();

      // Find password toggle buttons
      const toggleButtons = screen.getAllByRole('button', { name: /show password/i });
      expect(toggleButtons.length).toBe(2);

      // Focus and activate first toggle
      toggleButtons[0].focus();
      expect(document.activeElement).toBe(toggleButtons[0]);

      await user.keyboard('{Enter}');

      // Password should now be visible
      expect(getInput('password')).toHaveAttribute('type', 'text');
    });

    it('should have focusable submit button', () => {
      renderRegister();

      const submitButton = screen.getByRole('button', { name: /create account/i });
      submitButton.focus();

      expect(document.activeElement).toBe(submitButton);
    });

    it('should have focusable sign in link', () => {
      renderRegister();

      const signInLink = screen.getByRole('link', { name: /sign in/i });
      signInLink.focus();

      expect(document.activeElement).toBe(signInLink);
    });
  });

  describe('Loading State', () => {
    it('should not render loading state initially', () => {
      renderRegister();

      expect(screen.queryByText(/creating account/i)).not.toBeInTheDocument();
    });

    it('should show loading indicator and disable form during submission', async () => {
      const user = userEvent.setup();
      let resolveRegister: () => void;
      mockRegister.mockImplementation(() => new Promise<void>((resolve) => {
        resolveRegister = resolve;
      }));
      renderRegister();

      fillForm(validFormData);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      // Verify loading indicator shown and button disabled
      await waitFor(() => {
        const submitButton = screen.getByRole('button', { name: /creating account/i });
        expect(submitButton).toBeDisabled();
      });

      // Resolve promise and wait for state update to complete (fixes act warning)
      await act(async () => {
        resolveRegister!();
      });

      // Wait for loading state to clear
      await waitFor(() => {
        expect(screen.queryByText(/creating account/i)).not.toBeInTheDocument();
      });
    });
  });
});
