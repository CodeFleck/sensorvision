/**
 * Navigation filtering utility for role-based access control.
 * Extracts the filtering logic from LayoutV1 for testability.
 */

export interface NavigationItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly: boolean;
  excludeForAdmin?: boolean;
}

export interface NavigationSection {
  name: string;
  icon: React.ComponentType<{ className?: string }>;
  iconColor: string;
  items: NavigationItem[];
  adminOnly: boolean;
  excludeForAdmin?: boolean;
}

/**
 * Filters navigation sections based on user role.
 *
 * Rules:
 * - Sections with adminOnly: true are hidden from non-admin users
 * - Sections with excludeForAdmin: true are hidden from admin users
 * - Items within sections follow the same rules
 * - Sections with no visible items are hidden
 */
export function filterNavigationSections(
  sections: NavigationSection[],
  isAdmin: boolean
): NavigationSection[] {
  return sections
    .map(section => {
      // If section is admin-only and user is not admin, hide entire section
      if (section.adminOnly && !isAdmin) {
        return null;
      }

      // If section is marked excludeForAdmin and user is admin, hide entire section
      if (section.excludeForAdmin && isAdmin) {
        return null;
      }

      // Filter items within section
      const visibleItems = section.items.filter(item => {
        // Hide admin-only items from non-admins
        if (item.adminOnly && !isAdmin) {
          return false;
        }
        // Hide items marked as excludeForAdmin from admins
        if (item.excludeForAdmin && isAdmin) {
          return false;
        }
        return true;
      });

      // If no items are visible, hide section
      if (visibleItems.length === 0) {
        return null;
      }

      return { ...section, items: visibleItems };
    })
    .filter((section): section is NavigationSection => section !== null);
}
