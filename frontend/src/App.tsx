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
import { DashboardTemplates } from './pages/DashboardTemplates';
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
import ServerlessFunctions from './pages/ServerlessFunctions';
import DataPlugins from './pages/DataPlugins';
import WebhookTester from './pages/WebhookTester';
import ApiPlayground from './pages/ApiPlayground';
import EmailTemplateBuilder from './pages/EmailTemplateBuilder';
import { AdminSupportTickets } from './pages/AdminSupportTickets';
import { AdminCannedResponses } from './pages/AdminCannedResponses';
import { MyTickets } from './pages/MyTickets';
import DataRetention from './pages/DataRetention';
import Profile from './pages/Profile';
import { Playlists } from './pages/Playlists';
import { PlaylistPlayer } from './pages/PlaylistPlayer';
import SmsSettings from './pages/SmsSettings';
import PluginMarketplace from './pages/PluginMarketplace';

function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <WebSocketProvider>
          <Toaster position="top-right" />
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/oauth2/callback" element={<OAuth2Callback />} />
            <Route path="/how-it-works" element={<HowItWorks />} />

            {/* Protected Routes */}
            <Route
              path="/*"
              element={
                <ProtectedRoute>
                  <LayoutV1>
                    <Routes>
                      <Route path="/" element={<Dashboard />} />
                      <Route path="/dashboard" element={<Dashboard />} />
                      <Route path="/dashboards" element={<Dashboards />} />
                      <Route path="/dashboards/templates" element={<DashboardTemplates />} />
                      <Route path="/devices" element={<Devices />} />
                      <Route path="/device-groups" element={<DeviceGroups />} />
                      <Route path="/device-tags" element={<DeviceTags />} />
                      <Route path="/analytics" element={<Analytics />} />
                      <Route path="/rules" element={<Rules />} />
                      <Route path="/alerts" element={<Alerts />} />
                      <Route path="/notifications" element={<Notifications />} />
                      <Route path="/profile" element={<Profile />} />
                      <Route path="/playlists" element={<Playlists />} />
                      <Route path="/playlists/:id/play" element={<PlaylistPlayer />} />
                      <Route path="/my-tickets" element={<MyTickets />} />
                      <Route path="/integration/wizard" element={<IntegrationWizard />} />
                      <Route path="/serverless-functions" element={<ServerlessFunctions />} />
                      <Route path="/webhook-tester" element={<WebhookTester />} />
                      <Route path="/api-playground" element={<ApiPlayground />} />
                      <Route path="/variables" element={<Variables />} />
                      <Route path="/plugins" element={<PluginMarketplace />} />

                      {/* Admin Routes */}
                      <Route path="/admin/support" element={<ProtectedRoute adminOnly={true}><AdminSupportTickets /></ProtectedRoute>} />
                      <Route path="/admin/canned-responses" element={<ProtectedRoute adminOnly={true}><AdminCannedResponses /></ProtectedRoute>} />
                      <Route path="/events" element={<ProtectedRoute adminOnly={true}><Events /></ProtectedRoute>} />
                      <Route path="/data-ingestion" element={<ProtectedRoute adminOnly={true}><DataIngestion /></ProtectedRoute>} />
                      <Route path="/data-import" element={<ProtectedRoute adminOnly={true}><DataImport /></ProtectedRoute>} />
                      <Route path="/data-plugins" element={<ProtectedRoute adminOnly={true}><DataPlugins /></ProtectedRoute>} />
                      <Route path="/email-templates" element={<ProtectedRoute adminOnly={true}><EmailTemplateBuilder /></ProtectedRoute>} />
                      <Route path="/sms-settings" element={<ProtectedRoute adminOnly={true}><SmsSettings /></ProtectedRoute>} />
                      <Route path="/data-retention" element={<ProtectedRoute adminOnly={true}><DataRetention /></ProtectedRoute>} />
                      <Route path="/data-export" element={<ProtectedRoute adminOnly={true}><DataExport /></ProtectedRoute>} />
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