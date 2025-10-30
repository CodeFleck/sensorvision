import { Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LayoutV1 } from './components/LayoutV1';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { ForgotPassword } from './pages/ForgotPassword';
import { ResetPassword } from './pages/ResetPassword';
import { OAuth2Callback } from './pages/OAuth2Callback';
import HowItWorks from './pages/HowItWorks';
import { Dashboard } from './pages/Dashboard';
import { Dashboards } from './pages/Dashboards';
import { Devices } from './pages/Devices';
import { Analytics } from './pages/Analytics';
import { Rules } from './pages/Rules';
import { Alerts } from './pages/Alerts';
import { Events } from './pages/Events';
import { Notifications } from './pages/Notifications';
import DataIngestion from './pages/DataIngestion';
import DataImport from './pages/DataImport';
import DeviceGroups from './pages/DeviceGroups';
import DeviceTags from './pages/DeviceTags';
import DataExport from './pages/DataExport';
import Variables from './pages/Variables';
import IntegrationWizard from './pages/IntegrationWizard';
import WebhookTester from './pages/WebhookTester';
import ApiPlayground from './pages/ApiPlayground';
import EmailTemplateBuilder from './pages/EmailTemplateBuilder';
import { AdminSupportTickets } from './pages/AdminSupportTickets';
import { AdminCannedResponses } from './pages/AdminCannedResponses';
import { MyTickets } from './pages/MyTickets';
import DataRetention from './pages/DataRetention';
import { config } from './config';

function App() {

  return (
    <AuthProvider>
      <ThemeProvider>
        <WebSocketProvider url={config.webSocketUrl}>
          <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 4000,
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              duration: 5000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
        <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/oauth2/callback" element={<OAuth2Callback />} />
        <Route path="/how-it-works" element={<HowItWorks />} />

        {/* Protected routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <LayoutV1>
                <Routes>
                  {/* Standard user routes */}
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/integration-wizard" element={<IntegrationWizard />} />
                  <Route path="/dashboards" element={<Dashboards />} />
                  <Route path="/devices" element={<Devices />} />
                  <Route path="/analytics" element={<Analytics />} />
                  <Route path="/rules" element={<Rules />} />
                  <Route path="/alerts" element={<Alerts />} />
                  <Route path="/notifications" element={<Notifications />} />
                  <Route path="/my-tickets" element={<MyTickets />} />

                  {/* Admin-only routes */}
                  <Route path="/device-groups" element={<ProtectedRoute adminOnly={true}><DeviceGroups /></ProtectedRoute>} />
                  <Route path="/device-tags" element={<ProtectedRoute adminOnly={true}><DeviceTags /></ProtectedRoute>} />
                  <Route path="/events" element={<ProtectedRoute adminOnly={true}><Events /></ProtectedRoute>} />
                  <Route path="/data-ingestion" element={<ProtectedRoute adminOnly={true}><DataIngestion /></ProtectedRoute>} />
                  <Route path="/data-import" element={<ProtectedRoute adminOnly={true}><DataImport /></ProtectedRoute>} />
                  <Route path="/data-export" element={<ProtectedRoute adminOnly={true}><DataExport /></ProtectedRoute>} />
                  <Route path="/variables" element={<ProtectedRoute adminOnly={true}><Variables /></ProtectedRoute>} />
                  <Route path="/data-retention" element={<ProtectedRoute adminOnly={true}><DataRetention /></ProtectedRoute>} />
                  <Route path="/webhook-tester" element={<ProtectedRoute adminOnly={true}><WebhookTester /></ProtectedRoute>} />
                  <Route path="/api-playground" element={<ProtectedRoute adminOnly={true}><ApiPlayground /></ProtectedRoute>} />
                  <Route path="/email-templates" element={<ProtectedRoute adminOnly={true}><EmailTemplateBuilder /></ProtectedRoute>} />
                  <Route path="/admin/support-tickets" element={<ProtectedRoute adminOnly={true}><AdminSupportTickets /></ProtectedRoute>} />
                  <Route path="/admin/canned-responses" element={<ProtectedRoute adminOnly={true}><AdminCannedResponses /></ProtectedRoute>} />
                </Routes>
              </LayoutV1>
            </ProtectedRoute>
          }
        />
      </Routes>
        </WebSocketProvider>
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;