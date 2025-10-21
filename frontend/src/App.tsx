import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { OAuth2Callback } from './pages/OAuth2Callback';
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

function App() {
  return (
    <AuthProvider>
      <WebSocketProvider url="ws://localhost:8080/ws/telemetry">
        <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/oauth2/callback" element={<OAuth2Callback />} />

        {/* Protected routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <Layout>
                <Routes>
                  {/* Standard user routes */}
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/dashboards" element={<Dashboards />} />
                  <Route path="/devices" element={<Devices />} />
                  <Route path="/analytics" element={<Analytics />} />
                  <Route path="/rules" element={<Rules />} />
                  <Route path="/alerts" element={<Alerts />} />
                  <Route path="/notifications" element={<Notifications />} />

                  {/* Admin-only routes */}
                  <Route path="/device-groups" element={<ProtectedRoute adminOnly={true}><DeviceGroups /></ProtectedRoute>} />
                  <Route path="/device-tags" element={<ProtectedRoute adminOnly={true}><DeviceTags /></ProtectedRoute>} />
                  <Route path="/events" element={<ProtectedRoute adminOnly={true}><Events /></ProtectedRoute>} />
                  <Route path="/data-ingestion" element={<ProtectedRoute adminOnly={true}><DataIngestion /></ProtectedRoute>} />
                  <Route path="/data-import" element={<ProtectedRoute adminOnly={true}><DataImport /></ProtectedRoute>} />
                  <Route path="/data-export" element={<ProtectedRoute adminOnly={true}><DataExport /></ProtectedRoute>} />
                  <Route path="/variables" element={<ProtectedRoute adminOnly={true}><Variables /></ProtectedRoute>} />
                </Routes>
              </Layout>
            </ProtectedRoute>
          }
        />
      </Routes>
      </WebSocketProvider>
    </AuthProvider>
  );
}

export default App;