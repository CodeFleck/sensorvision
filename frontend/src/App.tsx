import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Devices } from './pages/Devices';
import { Analytics } from './pages/Analytics';
import { Rules } from './pages/Rules';
import { Alerts } from './pages/Alerts';

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/devices" element={<Devices />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/rules" element={<Rules />} />
        <Route path="/alerts" element={<Alerts />} />
      </Routes>
    </Layout>
  );
}

export default App;