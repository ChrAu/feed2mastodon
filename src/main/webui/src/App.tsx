import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import PiHoleDashboard from "./components/PiHoleDashboard";
import ProxmoxDashboard from "./components/ProxmoxDashboard";
import Datenschutz from "./pages/Datenschutz";
import Home from './pages/Home';
import Impressum from './pages/Impressum';
import CpuDashboard from "./components/CpuDashboard";

const App = () => {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/impressum" element={<Impressum />} />
            <Route path="/datenschutz" element={<Datenschutz />} />
        </Routes>
          <div className="container mx-auto">
              <h1 className="text-xl font-bold p-4">Server Status</h1>
              <ProxmoxDashboard />
              <CpuDashboard />
          </div>
          <div className="container mx-auto">
              <h1 className="text-xl font-bold p-4">Pi Hole Status</h1>
              <PiHoleDashboard />
          </div>
      </Layout>
    </Router>
  );
};

export default App;
