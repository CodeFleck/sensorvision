import React, { useState } from 'react';
import { X, Play, CheckCircle, AlertCircle, Info, Loader } from 'lucide-react';
import toast from 'react-hot-toast';
import Editor from '@monaco-editor/react';
import serverlessFunctionsService, {
  ServerlessFunction,
  InvokeFunctionRequest,
  InvokeFunctionResponse
} from '../../services/serverlessFunctionsService';

interface FunctionInvokeModalProps {
  open: boolean;
  function: ServerlessFunction;
  onClose: () => void;
}

const DEFAULT_INPUT = JSON.stringify({
  message: "Test invocation",
  timestamp: new Date().toISOString()
}, null, 2);

const FunctionInvokeModal: React.FC<FunctionInvokeModalProps> = ({
  open,
  function: func,
  onClose
}) => {
  const [inputJson, setInputJson] = useState(DEFAULT_INPUT);
  const [sync, setSync] = useState(true);
  const [invoking, setInvoking] = useState(false);
  const [result, setResult] = useState<InvokeFunctionResponse | null>(null);
  const [inputError, setInputError] = useState<string | null>(null);

  const handleInputChange = (value: string) => {
    setInputJson(value);
    setInputError(null);

    if (value.trim()) {
      try {
        JSON.parse(value);
      } catch (e) {
        setInputError('Invalid JSON');
      }
    }
  };

  const handleInvoke = async () => {
    if (inputError) {
      toast.error('Please fix JSON errors before invoking');
      return;
    }

    let input;
    try {
      input = JSON.parse(inputJson);
    } catch (e) {
      toast.error('Invalid JSON input');
      return;
    }

    try {
      setInvoking(true);
      setResult(null);

      const request: InvokeFunctionRequest = {
        input,
        sync
      };

      const response = await serverlessFunctionsService.invokeFunction(func.id, request);
      setResult(response);

      if (response.status === 'SUCCESS') {
        toast.success('Function executed successfully');
      } else if (response.status === 'ACCEPTED') {
        toast('Function invoked asynchronously', { icon: 'ℹ️' });
      } else {
        toast.error('Function execution failed');
      }
    } catch (error: any) {
      console.error('Failed to invoke function:', error);
      const errorMessage = error.response?.data?.message || error.message || 'Failed to invoke function';
      toast.error(errorMessage);
    } finally {
      setInvoking(false);
    }
  };

  const formatJson = (obj: any): string => {
    try {
      return JSON.stringify(obj, null, 2);
    } catch (e) {
      return String(obj);
    }
  };

  const handleClose = () => {
    setResult(null);
    setInputJson(DEFAULT_INPUT);
    setInputError(null);
    onClose();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-900">
            Invoke Function: {func.name}
          </h2>
          <button
            onClick={handleClose}
            className="p-1 hover:bg-gray-100 rounded"
          >
            <X className="w-6 h-6 text-gray-600" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div>
            <label className="flex items-center">
              <input
                type="checkbox"
                checked={sync}
                onChange={(e) => setSync(e.target.checked)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="ml-2 text-sm text-gray-700">
                Synchronous execution (wait for result)
              </span>
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Input Data (JSON)
            </label>
            <div className={`border rounded-md overflow-hidden ${
              inputError ? 'border-red-300' : 'border-gray-300'
            }`}>
              <Editor
                height="300px"
                language="json"
                value={inputJson}
                onChange={(value) => handleInputChange(value || '')}
                options={{
                  minimap: { enabled: false },
                  fontSize: 13,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  automaticLayout: true,
                  tabSize: 2,
                  insertSpaces: true,
                  wordWrap: 'on',
                  formatOnPaste: true,
                  formatOnType: true,
                }}
                theme="vs"
              />
            </div>
            {inputError && (
              <p className="mt-1 text-sm text-red-600">{inputError}</p>
            )}
          </div>

          {result && (
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-medium text-gray-700">Execution Result</h3>
                {result.status === 'SUCCESS' && (
                  <span className="flex items-center gap-1 px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded">
                    <CheckCircle className="w-3 h-3" />
                    Success
                  </span>
                )}
                {result.status === 'FAILED' && (
                  <span className="flex items-center gap-1 px-2 py-1 text-xs font-medium bg-red-100 text-red-800 rounded">
                    <AlertCircle className="w-3 h-3" />
                    Failed
                  </span>
                )}
                {result.status === 'ACCEPTED' && (
                  <span className="flex items-center gap-1 px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded">
                    <Info className="w-3 h-3" />
                    Async Invocation
                  </span>
                )}
              </div>

              {result.status === 'ACCEPTED' && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
                  Function invoked asynchronously. Execution ID: {result.executionId}
                  <br />
                  View execution history for results.
                </div>
              )}

              {result.output && (
                <div className="border border-gray-200 rounded-lg p-3">
                  <h4 className="text-xs font-medium text-gray-700 mb-2">Output</h4>
                  <pre className="text-xs font-mono bg-gray-50 p-2 rounded overflow-x-auto">
                    {formatJson(result.output)}
                  </pre>
                </div>
              )}

              {result.errorMessage && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                  <h4 className="text-xs font-medium text-red-800 mb-2">Error Message</h4>
                  <pre className="text-xs text-red-700 whitespace-pre-wrap">
                    {result.errorMessage}
                  </pre>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="sticky bottom-0 bg-gray-50 border-t border-gray-200 px-6 py-4 flex justify-end gap-3">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Close
          </button>
          <button
            onClick={handleInvoke}
            disabled={invoking || !!inputError}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {invoking ? (
              <>
                <Loader className="w-4 h-4 animate-spin" />
                Invoking...
              </>
            ) : (
              <>
                <Play className="w-4 h-4" />
                Invoke Function
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FunctionInvokeModal;
