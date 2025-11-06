import React, { useState, useEffect, useRef } from 'react';
import { X, Plus, Trash2, ChevronDown, ChevronRight, Maximize2, Minimize2 } from 'lucide-react';
import toast from 'react-hot-toast';
import Editor, { Monaco } from '@monaco-editor/react';
import serverlessFunctionsService, {
  ServerlessFunction,
  FunctionRuntime,
  CreateFunctionRequest,
  UpdateFunctionRequest
} from '../../services/serverlessFunctionsService';
import SecretsManager from './SecretsManager';

interface FunctionEditorProps {
  open: boolean;
  function: ServerlessFunction | null;
  onClose: () => void;
  onSave: () => void;
}

const PYTHON_TEMPLATE = `def main(event):
    """
    Main handler function.

    Args:
        event: Input data (JSON object)

    Returns:
        JSON-serializable object
    """
    # Your code here
    return {
        "message": "Hello from Python",
        "input": event
    }
`;

const NODEJS_TEMPLATE = `exports.handler = async (event) => {
    // Your code here
    return {
        message: "Hello from Node.js",
        input: event
    };
};
`;

const FunctionEditor: React.FC<FunctionEditorProps> = ({
  open,
  function: editFunction,
  onClose,
  onSave
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [runtime, setRuntime] = useState<FunctionRuntime>(FunctionRuntime.PYTHON_3_11);
  const [code, setCode] = useState('');
  const [handler, setHandler] = useState('main');
  const [enabled, setEnabled] = useState(true);
  const [timeoutSeconds, setTimeoutSeconds] = useState(30);
  const [memoryLimitMb, setMemoryLimitMb] = useState(512);
  const [envVars, setEnvVars] = useState<Array<{ key: string; value: string }>>([]);
  const [envVarsExpanded, setEnvVarsExpanded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editorFullscreen, setEditorFullscreen] = useState(false);
  const editorRef = useRef<any>(null);

  useEffect(() => {
    if (editFunction) {
      setName(editFunction.name);
      setDescription(editFunction.description || '');
      setRuntime(editFunction.runtime);
      setCode(editFunction.code);
      setHandler(editFunction.handler);
      setEnabled(editFunction.enabled);
      setTimeoutSeconds(editFunction.timeoutSeconds);
      setMemoryLimitMb(editFunction.memoryLimitMb);

      if (editFunction.environmentVariables) {
        const vars = Object.entries(editFunction.environmentVariables).map(([key, value]) => ({
          key,
          value
        }));
        setEnvVars(vars);
      } else {
        setEnvVars([]);
      }
    } else {
      // Reset for new function
      setName('');
      setDescription('');
      setRuntime(FunctionRuntime.PYTHON_3_11);
      setCode(PYTHON_TEMPLATE);
      setHandler('main');
      setEnabled(true);
      setTimeoutSeconds(30);
      setMemoryLimitMb(512);
      setEnvVars([]);
    }
  }, [editFunction, open]);

  const handleRuntimeChange = (newRuntime: FunctionRuntime) => {
    setRuntime(newRuntime);
    if (!editFunction) {
      // Set template and default handler for new functions
      if (newRuntime === FunctionRuntime.PYTHON_3_11) {
        setCode(PYTHON_TEMPLATE);
        setHandler('main');
      } else if (newRuntime === FunctionRuntime.NODEJS_18) {
        setCode(NODEJS_TEMPLATE);
        setHandler('handler');
      }
    }
  };

  const handleAddEnvVar = () => {
    setEnvVars([...envVars, { key: '', value: '' }]);
  };

  const handleRemoveEnvVar = (index: number) => {
    setEnvVars(envVars.filter((_, i) => i !== index));
  };

  const handleEnvVarChange = (index: number, field: 'key' | 'value', value: string) => {
    const updated = [...envVars];
    updated[index][field] = value;
    setEnvVars(updated);
  };

  const handleEditorMount = (editor: any, monaco: Monaco) => {
    editorRef.current = editor;

    // Configure Monaco editor theme and settings
    monaco.editor.defineTheme('sensorvision', {
      base: 'vs',
      inherit: true,
      rules: [],
      colors: {
        'editor.background': '#ffffff',
      }
    });
    monaco.editor.setTheme('sensorvision');
  };

  const getEditorLanguage = (): string => {
    switch (runtime) {
      case FunctionRuntime.PYTHON_3_11:
        return 'python';
      case FunctionRuntime.NODEJS_18:
        return 'javascript';
      default:
        return 'plaintext';
    }
  };

  const handleSave = async () => {
    if (!name.trim()) {
      toast.error('Function name is required');
      return;
    }

    if (!code.trim()) {
      toast.error('Function code is required');
      return;
    }

    try {
      setSaving(true);

      const environmentVariables: Record<string, string> = {};
      envVars.forEach(({ key, value }) => {
        if (key.trim()) {
          environmentVariables[key] = value;
        }
      });

      if (editFunction) {
        // Update existing function
        const request: UpdateFunctionRequest = {
          name,
          description: description || undefined,
          code,
          handler,
          enabled,
          timeoutSeconds,
          memoryLimitMb,
          environmentVariables: Object.keys(environmentVariables).length > 0 ? environmentVariables : undefined
        };
        await serverlessFunctionsService.updateFunction(editFunction.id, request);
        toast.success('Function updated successfully');
      } else {
        // Create new function
        const request: CreateFunctionRequest = {
          name,
          description: description || undefined,
          runtime,
          code,
          handler,
          enabled,
          timeoutSeconds,
          memoryLimitMb,
          environmentVariables: Object.keys(environmentVariables).length > 0 ? environmentVariables : undefined
        };
        await serverlessFunctionsService.createFunction(request);
        toast.success('Function created successfully');
      }

      onSave();
    } catch (error: any) {
      console.error('Failed to save function:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to save function';
      toast.error(errorMessage);
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-900">
            {editFunction ? 'Edit Function' : 'Create New Function'}
          </h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded"
          >
            <X className="w-6 h-6 text-gray-600" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Function Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={!!editFunction}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                placeholder="my-function"
              />
              {editFunction && (
                <p className="mt-1 text-xs text-gray-500">Function name cannot be changed</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Runtime
              </label>
              <select
                value={runtime}
                onChange={(e) => handleRuntimeChange(e.target.value as FunctionRuntime)}
                disabled={!!editFunction}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
              >
                <option value={FunctionRuntime.PYTHON_3_11}>Python 3.11</option>
                <option value={FunctionRuntime.NODEJS_18}>Node.js 18</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
              placeholder="Describe what this function does..."
            />
          </div>

          <div className="relative">
            <div className="flex items-center justify-between mb-1">
              <label className="block text-sm font-medium text-gray-700">
                Function Code <span className="text-red-500">*</span>
              </label>
              <button
                type="button"
                onClick={() => setEditorFullscreen(!editorFullscreen)}
                className="p-1 hover:bg-gray-100 rounded"
                title={editorFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {editorFullscreen ? (
                  <Minimize2 className="w-4 h-4 text-gray-600" />
                ) : (
                  <Maximize2 className="w-4 h-4 text-gray-600" />
                )}
              </button>
            </div>
            <div className={`border border-gray-300 rounded-md overflow-hidden ${editorFullscreen ? 'fixed inset-4 z-50 bg-white shadow-2xl flex flex-col' : ''}`}>
              {editorFullscreen && (
                <div className="flex items-center justify-between px-4 py-2 bg-gray-50 border-b">
                  <span className="text-sm font-medium text-gray-700">Code Editor - {name || 'New Function'}</span>
                  <button
                    onClick={() => setEditorFullscreen(false)}
                    className="p-1 hover:bg-gray-200 rounded"
                  >
                    <X className="w-4 h-4 text-gray-600" />
                  </button>
                </div>
              )}
              <Editor
                height={editorFullscreen ? "calc(100vh - 120px)" : "500px"}
                language={getEditorLanguage()}
                value={code}
                onChange={(value) => setCode(value || '')}
                onMount={handleEditorMount}
                options={{
                  minimap: { enabled: editorFullscreen },
                  fontSize: 14,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  automaticLayout: true,
                  tabSize: runtime === FunctionRuntime.PYTHON_3_11 ? 4 : 2,
                  insertSpaces: true,
                  wordWrap: 'on',
                  formatOnPaste: true,
                  formatOnType: true,
                  quickSuggestions: true,
                  suggestOnTriggerCharacters: true,
                  acceptSuggestionOnEnter: 'on',
                  snippetSuggestions: 'top',
                }}
                theme="vs"
                loading={<div className="flex items-center justify-center h-full"><div className="text-gray-500">Loading editor...</div></div>}
              />
            </div>
            <p className="mt-1 text-xs text-gray-500">
              {runtime === FunctionRuntime.PYTHON_3_11
                ? 'Define a main(event) function that takes input and returns a JSON-serializable result'
                : 'Define an exports.handler async function that takes event and returns a result'}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Handler <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={handler}
                onChange={(e) => setHandler(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                placeholder="main"
              />
              <p className="mt-1 text-xs text-gray-500">Entry point function name</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Timeout (seconds)
              </label>
              <input
                type="number"
                value={timeoutSeconds}
                onChange={(e) => setTimeoutSeconds(parseInt(e.target.value) || 30)}
                min={1}
                max={900}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Memory Limit (MB)
              </label>
              <input
                type="number"
                value={memoryLimitMb}
                onChange={(e) => setMemoryLimitMb(parseInt(e.target.value) || 512)}
                min={128}
                max={3008}
                step={64}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={enabled}
                onChange={(e) => setEnabled(e.target.checked)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="ml-2 text-sm text-gray-700">Enabled</span>
            </label>
          </div>

          <div className="border border-gray-200 rounded-md">
            <button
              type="button"
              onClick={() => setEnvVarsExpanded(!envVarsExpanded)}
              className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50"
            >
              <span className="text-sm font-medium text-gray-700">
                Environment Variables ({envVars.length})
              </span>
              {envVarsExpanded ? (
                <ChevronDown className="w-5 h-5 text-gray-500" />
              ) : (
                <ChevronRight className="w-5 h-5 text-gray-500" />
              )}
            </button>

            {envVarsExpanded && (
              <div className="px-4 py-3 border-t border-gray-200 space-y-2">
                {envVars.map((envVar, index) => (
                  <div key={index} className="flex gap-2">
                    <input
                      type="text"
                      value={envVar.key}
                      onChange={(e) => handleEnvVarChange(index, 'key', e.target.value)}
                      placeholder="Key"
                      className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                    />
                    <input
                      type="text"
                      value={envVar.value}
                      onChange={(e) => handleEnvVarChange(index, 'value', e.target.value)}
                      placeholder="Value"
                      className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                    />
                    <button
                      onClick={() => handleRemoveEnvVar(index)}
                      className="p-2 text-red-600 hover:bg-red-50 rounded"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
                <button
                  onClick={handleAddEnvVar}
                  className="flex items-center gap-2 px-3 py-2 text-sm text-blue-600 hover:bg-blue-50 rounded"
                >
                  <Plus className="w-4 h-4" />
                  Add Variable
                </button>
              </div>
            )}
          </div>

          {/* Encrypted Secrets Section - Only shown for existing functions */}
          {editFunction && (
            <SecretsManager functionId={editFunction.id} />
          )}
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4 flex justify-end gap-3">
          <button
            onClick={onClose}
            disabled={saving}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? 'Saving...' : editFunction ? 'Update' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FunctionEditor;
