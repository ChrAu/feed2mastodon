import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Datenschutz from "./pages/Datenschutz";
import Home from './pages/Home';
import Impressum from './pages/Impressum';

const App = () => {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/impressum" element={<Impressum />} />
            <Route path="/datenschutz" element={<Datenschutz />} />
        </Routes>
      </Layout>
    </Router>
  );
};

export default App;