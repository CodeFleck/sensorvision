import { useState, useEffect } from 'react';
import { Layers, X } from 'lucide-react';

export type LayoutVersion = 'v1' | 'v2' | 'v3';

interface LayoutVersionSwitcherProps {
  currentVersion: LayoutVersion;
  onVersionChange: (version: LayoutVersion) => void;
}

const versions = [
  {
    id: 'v1' as LayoutVersion,
    name: 'V1: Grouped Sections',
    description: 'Collapsible sections with color-coded groups',
    features: ['Organized sections', 'Collapsible groups', 'Classic sidebar']
  },
  {
    id: 'v2' as LayoutVersion,
    name: 'V2: Mega Menu',
    description: 'Horizontal navigation with dropdown menus',
    features: ['Horizontal nav', 'Mega dropdowns', 'More screen space']
  },
  {
    id: 'v3' as LayoutVersion,
    name: 'V3: Icon Sidebar',
    description: 'Modern dark collapsible sidebar with icons',
    features: ['Dark theme', 'Collapsible icons', 'Keyboard shortcuts']
  }
];

export const LayoutVersionSwitcher: React.FC<LayoutVersionSwitcherProps> = ({
  currentVersion,
  onVersionChange
}) => {
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ctrl+Shift+L to open switcher
      if (e.ctrlKey && e.shiftKey && e.key === 'L') {
        e.preventDefault();
        setIsOpen(prev => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <>
      {/* Floating Button */}
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-50 flex items-center gap-2 px-4 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all hover:scale-105"
        title="Switch Layout Version (Ctrl+Shift+L)"
      >
        <Layers className="h-5 w-5" />
        <span className="font-medium">Layout: V{currentVersion.slice(1)}</span>
      </button>

      {/* Modal */}
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 backdrop-blur-sm">
          <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            {/* Header */}
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between rounded-t-xl">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                  <Layers className="h-6 w-6 text-blue-600" />
                  Choose Layout Version
                </h2>
                <p className="text-sm text-gray-600 mt-1">
                  Experiment with different menu designs • Current: <span className="font-semibold">V{currentVersion.slice(1)}</span>
                </p>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <X className="h-6 w-6 text-gray-500" />
              </button>
            </div>

            {/* Version Cards */}
            <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-4">
              {versions.map((version) => {
                const isActive = currentVersion === version.id;
                return (
                  <button
                    key={version.id}
                    onClick={() => {
                      onVersionChange(version.id);
                      setIsOpen(false);
                    }}
                    className={`
                      relative p-6 rounded-xl border-2 text-left transition-all
                      ${isActive
                        ? 'border-blue-600 bg-blue-50 shadow-lg ring-2 ring-blue-600 ring-opacity-50'
                        : 'border-gray-200 hover:border-blue-400 hover:shadow-md'
                      }
                    `}
                  >
                    {/* Active Badge */}
                    {isActive && (
                      <div className="absolute top-3 right-3 px-2 py-1 bg-blue-600 text-white text-xs font-bold rounded-full">
                        ACTIVE
                      </div>
                    )}

                    {/* Version Number */}
                    <div className={`
                      inline-flex items-center justify-center w-12 h-12 rounded-full mb-4 font-bold text-lg
                      ${isActive
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700'
                      }
                    `}>
                      V{version.id.slice(1)}
                    </div>

                    {/* Title & Description */}
                    <h3 className={`text-lg font-bold mb-2 ${isActive ? 'text-blue-900' : 'text-gray-900'}`}>
                      {version.name}
                    </h3>
                    <p className="text-sm text-gray-600 mb-4">
                      {version.description}
                    </p>

                    {/* Features */}
                    <ul className="space-y-2">
                      {version.features.map((feature, idx) => (
                        <li key={idx} className="flex items-center text-sm">
                          <span className={`mr-2 ${isActive ? 'text-blue-600' : 'text-gray-400'}`}>✓</span>
                          <span className="text-gray-700">{feature}</span>
                        </li>
                      ))}
                    </ul>

                    {/* Select Button */}
                    {!isActive && (
                      <div className="mt-4 pt-4 border-t border-gray-200">
                        <span className="text-sm font-medium text-blue-600 hover:text-blue-700">
                          Switch to this version →
                        </span>
                      </div>
                    )}
                  </button>
                );
              })}
            </div>

            {/* Footer Info */}
            <div className="border-t border-gray-200 px-6 py-4 bg-gray-50 rounded-b-xl">
              <div className="flex items-center justify-between text-sm">
                <div className="text-gray-600">
                  <kbd className="px-2 py-1 bg-white border border-gray-300 rounded text-xs font-mono">Ctrl</kbd>
                  {' + '}
                  <kbd className="px-2 py-1 bg-white border border-gray-300 rounded text-xs font-mono">Shift</kbd>
                  {' + '}
                  <kbd className="px-2 py-1 bg-white border border-gray-300 rounded text-xs font-mono">L</kbd>
                  <span className="ml-2 text-gray-500">to open this menu</span>
                </div>
                <div className="text-gray-500">
                  Experiment mode • Branch: feature/menu-redesign
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
