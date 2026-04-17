import { lazy, Suspense, useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';

const Home = lazy(() => import('./pages/Home'));
const ServerStatus = lazy(() => import('./pages/ServerStatus'));
const Impressum = lazy(() => import('./pages/Impressum'));
const Datenschutz = lazy(() => import('./pages/Datenschutz'));
const MailTest = lazy(() => import('./pages/MailTest'));
const Tanken = lazy(() => import('./pages/Tanken'));
const Strompreis = lazy(() => import('./pages/Strompreis'));
const Auto = lazy(() => import('./pages/Auto')); // New import

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        // Prüfen, ob der User angemeldet ist (JSON-Antwort parsen)
        fetch('/api/user')
            .then(res => res.json())
            .then(data => setIsAuthenticated(data.loggedIn))
            .catch(() => setIsAuthenticated(false));
    }, []);

    // Zeige einen Lade-Indikator, solange der Auth-Status noch nicht bekannt ist
    if (isAuthenticated === null) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
            </div>
        );
    }

    // Wenn nicht angemeldet, leite zur Startseite um.
    // Das verhindert auch, dass React.lazy den Code für das Modul lädt.
    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
};

const App = () => {
    return (
        <Router>
            <Layout>
                <Suspense fallback={<div className="flex justify-center items-center h-screen"><div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div></div>}>
                    <Routes>
                        <Route path="/" element={<Home />} />
                        <Route path="/server-status" element={<ServerStatus />} />
                        <Route path="/impressum" element={<Impressum />} />
                        <Route path="/datenschutz" element={<Datenschutz />} />
                        <Route path="/mail-test" element={<MailTest />} />
                        <Route path="/tanken" element={<Tanken />} />
                        <Route path="/strompreis" element={<Strompreis />} />
                        <Route path="/auto" element={<ProtectedRoute><Auto /></ProtectedRoute>} /> {/* Geschützte Route */}
                    </Routes>
                </Suspense>
            </Layout>
        </Router>
    );
};

export default App;
