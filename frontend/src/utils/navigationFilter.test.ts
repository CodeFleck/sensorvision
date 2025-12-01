import { describe, it, expect } from 'vitest';
import { filterNavigationSections, NavigationSection, NavigationItem } from './navigationFilter';

// Mock icon component
const MockIcon = () => null;

// Helper to create a navigation item
function createItem(overrides: Partial<NavigationItem> = {}): NavigationItem {
  return {
    name: 'Test Item',
    href: '/test',
    icon: MockIcon,
    adminOnly: false,
    ...overrides,
  };
}

// Helper to create a navigation section
function createSection(overrides: Partial<NavigationSection> = {}): NavigationSection {
  return {
    name: 'Test Section',
    icon: MockIcon,
    iconColor: 'text-blue-600',
    items: [createItem()],
    adminOnly: false,
    ...overrides,
  };
}

describe('filterNavigationSections', () => {
  describe('section-level filtering', () => {
    it('shows non-admin section to regular users', () => {
      const sections = [createSection({ adminOnly: false })];
      const result = filterNavigationSections(sections, false);
      expect(result).toHaveLength(1);
    });

    it('shows non-admin section to admin users', () => {
      const sections = [createSection({ adminOnly: false })];
      const result = filterNavigationSections(sections, true);
      expect(result).toHaveLength(1);
    });

    it('hides admin-only section from regular users', () => {
      const sections = [createSection({ adminOnly: true })];
      const result = filterNavigationSections(sections, false);
      expect(result).toHaveLength(0);
    });

    it('shows admin-only section to admin users', () => {
      const sections = [createSection({ adminOnly: true })];
      const result = filterNavigationSections(sections, true);
      expect(result).toHaveLength(1);
    });

    it('hides excludeForAdmin section from admin users', () => {
      const sections = [createSection({ excludeForAdmin: true })];
      const result = filterNavigationSections(sections, true);
      expect(result).toHaveLength(0);
    });

    it('shows excludeForAdmin section to regular users', () => {
      const sections = [createSection({ excludeForAdmin: true })];
      const result = filterNavigationSections(sections, false);
      expect(result).toHaveLength(1);
    });
  });

  describe('item-level filtering', () => {
    it('shows non-admin items to regular users', () => {
      const sections = [
        createSection({
          items: [createItem({ adminOnly: false })],
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result[0].items).toHaveLength(1);
    });

    it('hides admin-only items from regular users', () => {
      const sections = [
        createSection({
          items: [
            createItem({ name: 'Regular Item', adminOnly: false }),
            createItem({ name: 'Admin Item', adminOnly: true }),
          ],
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result[0].items).toHaveLength(1);
      expect(result[0].items[0].name).toBe('Regular Item');
    });

    it('shows admin-only items to admin users', () => {
      const sections = [
        createSection({
          items: [createItem({ adminOnly: true })],
        }),
      ];
      const result = filterNavigationSections(sections, true);
      expect(result[0].items).toHaveLength(1);
    });

    it('hides excludeForAdmin items from admin users', () => {
      const sections = [
        createSection({
          items: [
            createItem({ name: 'Admin Visible', excludeForAdmin: false }),
            createItem({ name: 'User Only', excludeForAdmin: true }),
          ],
        }),
      ];
      const result = filterNavigationSections(sections, true);
      expect(result[0].items).toHaveLength(1);
      expect(result[0].items[0].name).toBe('Admin Visible');
    });

    it('shows excludeForAdmin items to regular users', () => {
      const sections = [
        createSection({
          items: [createItem({ excludeForAdmin: true })],
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result[0].items).toHaveLength(1);
    });
  });

  describe('section hiding when no items visible', () => {
    it('hides section when all items are admin-only and user is not admin', () => {
      const sections = [
        createSection({
          adminOnly: false,
          items: [
            createItem({ adminOnly: true }),
            createItem({ adminOnly: true }),
          ],
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result).toHaveLength(0);
    });

    it('hides section when all items are excludeForAdmin and user is admin', () => {
      const sections = [
        createSection({
          adminOnly: false,
          items: [
            createItem({ excludeForAdmin: true }),
            createItem({ excludeForAdmin: true }),
          ],
        }),
      ];
      const result = filterNavigationSections(sections, true);
      expect(result).toHaveLength(0);
    });
  });

  describe('complex scenarios', () => {
    it('handles MONITORING section correctly for regular users', () => {
      const monitoringSection = createSection({
        name: 'MONITORING',
        excludeForAdmin: true,
        items: [
          createItem({ name: 'Rules', excludeForAdmin: true }),
          createItem({ name: 'Alerts', excludeForAdmin: true }),
          createItem({ name: 'Events', excludeForAdmin: true }),
        ],
      });
      const result = filterNavigationSections([monitoringSection], false);
      expect(result).toHaveLength(1);
      expect(result[0].items).toHaveLength(3);
    });

    it('handles MONITORING section correctly for admin users', () => {
      const monitoringSection = createSection({
        name: 'MONITORING',
        excludeForAdmin: true,
        items: [
          createItem({ name: 'Rules', excludeForAdmin: true }),
          createItem({ name: 'Alerts', excludeForAdmin: true }),
          createItem({ name: 'Events', excludeForAdmin: true }),
        ],
      });
      const result = filterNavigationSections([monitoringSection], true);
      expect(result).toHaveLength(0);
    });

    it('handles DATA MANAGEMENT section correctly for regular users', () => {
      const dataSection = createSection({
        name: 'DATA MANAGEMENT',
        excludeForAdmin: true,
        items: [
          createItem({ name: 'Data Import', excludeForAdmin: true }),
          createItem({ name: 'Data Export', excludeForAdmin: true }),
          createItem({ name: 'Variables', excludeForAdmin: true }),
        ],
      });
      const result = filterNavigationSections([dataSection], false);
      expect(result).toHaveLength(1);
      expect(result[0].items).toHaveLength(3);
    });

    it('handles DATA MANAGEMENT section correctly for admin users', () => {
      const dataSection = createSection({
        name: 'DATA MANAGEMENT',
        excludeForAdmin: true,
        items: [
          createItem({ name: 'Data Import', excludeForAdmin: true }),
          createItem({ name: 'Data Export', excludeForAdmin: true }),
          createItem({ name: 'Variables', excludeForAdmin: true }),
        ],
      });
      const result = filterNavigationSections([dataSection], true);
      expect(result).toHaveLength(0);
    });

    it('handles ADMINISTRATION section correctly for admin users', () => {
      const adminSection = createSection({
        name: 'ADMINISTRATION',
        adminOnly: true,
        items: [
          createItem({ name: 'User Management', adminOnly: true }),
          createItem({ name: 'Organizations', adminOnly: true }),
        ],
      });
      const result = filterNavigationSections([adminSection], true);
      expect(result).toHaveLength(1);
      expect(result[0].items).toHaveLength(2);
    });

    it('handles ADMINISTRATION section correctly for regular users', () => {
      const adminSection = createSection({
        name: 'ADMINISTRATION',
        adminOnly: true,
        items: [
          createItem({ name: 'User Management', adminOnly: true }),
          createItem({ name: 'Organizations', adminOnly: true }),
        ],
      });
      const result = filterNavigationSections([adminSection], false);
      expect(result).toHaveLength(0);
    });
  });

  describe('edge cases', () => {
    it('handles empty sections array', () => {
      const result = filterNavigationSections([], false);
      expect(result).toHaveLength(0);
    });

    it('handles section with empty items array', () => {
      const sections = [createSection({ items: [] })];
      const result = filterNavigationSections(sections, false);
      expect(result).toHaveLength(0);
    });

    it('preserves section properties in returned result', () => {
      const sections = [
        createSection({
          name: 'Custom Section',
          iconColor: 'text-red-600',
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result[0].name).toBe('Custom Section');
      expect(result[0].iconColor).toBe('text-red-600');
    });

    it('preserves item properties in returned result', () => {
      const sections = [
        createSection({
          items: [createItem({ name: 'Custom Item', href: '/custom' })],
        }),
      ];
      const result = filterNavigationSections(sections, false);
      expect(result[0].items[0].name).toBe('Custom Item');
      expect(result[0].items[0].href).toBe('/custom');
    });
  });
});
