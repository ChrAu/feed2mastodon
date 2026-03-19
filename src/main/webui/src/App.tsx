import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Datenschutz from "./pages/Datenschutz";
import Home from './pages/Home';
import Impressum from './pages/Impressum';
import ServerStatus from "./pages/ServerStatus";

const App = () => {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/server-status" element={<ServerStatus />} />
          <Route path="/impressum" element={<Impressum />} />
          <Route path="/datenschutz" element={<Datenschutz />} />
        </Routes>
      </Layout>
    </Router>
  );
};

export default App;
