import { Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { ErrorBoundary } from './components/ErrorBoundary';
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
import { DashboardTemplates } from './pages/DashboardTemplates';
import { Devices } from './pages/Devices';
import DeviceGroups from './pages/DeviceGroups';
import DeviceTags from './pages/DeviceTags';
import DeviceTypes from './pages/DeviceTypes';
import { Analytics } from './pages/Analytics';
import { Rules } from './pages/Rules';
import { Alerts } from './pages/Alerts';
import { Events } from './pages/Events';
import { Notifications } from './pages/Notifications';
import DataIngestion from './pages/DataIngestion';
import DataImport from './pages/DataImport';
import { IntegrationWizard } from './pages/IntegrationWizard';
import ServerlessFunctions from './pages/ServerlessFunctions';
import DataPlugins from './pages/DataPlugins';
import PluginMarketplace from './pages/PluginMarketplace';
import EmailTemplateBuilder from './pages/EmailTemplateBuilder';
import { AdminSupportTickets } from './pages/AdminSupportTickets';
import { AdminCannedResponses } from './pages/AdminCannedResponses';
import { MyTickets } from './pages/MyTickets';
import DataRetention from './pages/DataRetention';
import Variables from './pages/Variables';
import DataExport from './pages/DataExport';
import WebhookTester from './pages/WebhookTester';
import ApiPlayground from './pages/ApiPlayground';
import Profile from './pages/Profile';

import { Playlists } from './pages/Playlists';
import { PlaylistPlayer } from './pages/PlaylistPlayer';
import PhoneNumbers from './pages/PhoneNumbers';
import SmsSettings from './pages/SmsSettings';
import AdminUsers from './pages/AdminUsers';
import AdminOrganizations from './pages/AdminOrganizations';
import AdminDevices from './pages/AdminDevices';
import { AdminTrash } from './pages/AdminTrash';
import { GlobalRules } from './pages/GlobalRules';
import { GlobalAlerts } from './pages/GlobalAlerts';
import LogViewer from './pages/LogViewer';
import Settings from './pages/Settings';
import { MLModels } from './pages/MLModels';
import { MLAnomalies } from './pages/MLAnomalies';
import { AIAssistant } from './pages/AIAssistant';

import AdminDashboard from './pages/AdminDashboard';
import { config } from './config';

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ThemeProvider>
          <WebSocketProvider url={config.webSocketUrl}>
            <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: { background: '#363636', color: '#fff' },
              success: { duration: 4000, iconTheme: { primary: '#10b981', secondary: '#fff' } },
              error: { duration: 5000, iconTheme: { primary: '#ef4444', secondary: '#fff' } },
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

            {/* Fullscreen Playlist Player (outside layout) */}
            <Route
              path="/playlist-player/:playlistId"
              element={
                <ProtectedRoute>
                  <PlaylistPlayer />
                </ProtectedRoute>
              }
            />

            {/* Protected routes */}
            <Route
              path="/*"
              element={
                <ProtectedRoute>
                  <LayoutV1>
                    <Routes>
                      {/* Core user routes */}
                      <Route path="/" element={<Dashboard />} />
                      <Route path="/dashboards" element={<Dashboards />} />
                      <Route path="/dashboard-templates" element={<DashboardTemplates />} />
                      <Route path="/analytics" element={<Analytics />} />
                      <Route path="/profile" element={<Profile />} />
                      <Route path="/settings" element={<Settings />} />

                      {/* Devices & Data routes (user-accessible) */}
                      <Route path="/devices" element={<Devices />} />
                      <Route path="/device-groups" element={<DeviceGroups />} />
                      <Route path="/device-tags" element={<DeviceTags />} />
                      <Route path="/device-types" element={<DeviceTypes />} />
                      <Route path="/integration-wizard" element={<IntegrationWizard />} />
                      <Route path="/serverless-functions" element={<ServerlessFunctions />} />
                      <Route path="/data-plugins" element={<DataPlugins />} />
                      <Route path="/plugin-marketplace" element={<PluginMarketplace />} />
                      <Route path="/playlists" element={<Playlists />} />

                      {/* Monitoring routes (user-accessible) */}
                      <Route path="/rules" element={<Rules />} />
                      <Route path="/global-rules" element={<GlobalRules />} />
                      <Route path="/global-alerts" element={<GlobalAlerts />} />
                      <Route path="/alerts" element={<Alerts />} />
                      <Route path="/notifications" element={<Notifications />} />
                      <Route path="/phone-numbers" element={<PhoneNumbers />} />
                      <Route path="/sms-settings" element={<SmsSettings />} />
                      <Route path="/events" element={<Events />} />

                      {/* ML Pipeline routes (user-accessible) */}
                      <Route path="/ml-models" element={<MLModels />} />
                      <Route path="/ml-anomalies" element={<MLAnomalies />} />
                      <Route path="/ai-assistant" element={<AIAssistant />} />

                      {/* Data Management routes (user-accessible) */}
                      <Route path="/data-ingestion" element={<DataIngestion />} />
                      <Route path="/data-import" element={<DataImport />} />
                      <Route path="/data-export" element={<DataExport />} />
                      <Route path="/variables" element={<Variables />} />
                      <Route path="/data-retention" element={<DataRetention />} />
                      <Route path="/webhook-tester" element={<WebhookTester />} />
                      <Route path="/api-playground" element={<ApiPlayground />} />

                      {/* Support routes */}
                      <Route path="/my-tickets" element={<MyTickets />} />

                      {/* Developer tools (requires ROLE_DEVELOPER) */}
                      <Route path="/logs" element={<ProtectedRoute requiredRole="ROLE_DEVELOPER"><LogViewer /></ProtectedRoute>} />

                      {/* Admin-only routes */}
                      <Route path="/admin-dashboard" element={<ProtectedRoute adminOnly={true}><AdminDashboard /></ProtectedRoute>} />
                      <Route path="/admin/users" element={<ProtectedRoute adminOnly={true}><AdminUsers /></ProtectedRoute>} />
                      <Route path="/admin/devices" element={<ProtectedRoute adminOnly={true}><AdminDevices /></ProtectedRoute>} />
                      <Route path="/admin/organizations" element={<ProtectedRoute adminOnly={true}><AdminOrganizations /></ProtectedRoute>} />
                      <Route path="/admin/support-tickets" element={<ProtectedRoute adminOnly={true}><AdminSupportTickets /></ProtectedRoute>} />
                      <Route path="/admin/canned-responses" element={<ProtectedRoute adminOnly={true}><AdminCannedResponses /></ProtectedRoute>} />
                      <Route path="/admin/trash" element={<ProtectedRoute adminOnly={true}><AdminTrash /></ProtectedRoute>} />
                      <Route path="/email-templates" element={<ProtectedRoute adminOnly={true}><EmailTemplateBuilder /></ProtectedRoute>} />
                    </Routes>
                  </LayoutV1>
                </ProtectedRoute>
              }
            />
          </Routes>
          </WebSocketProvider>
        </ThemeProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;