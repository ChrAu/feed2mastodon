import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Server,
  Shield
} from 'lucide-react';
import StatusIndicator from './StatusIndicator';
import MaintenancePopup from './MaintenancePopup'; // Import the new component

interface LayoutProps {
  children: React.ReactNode;
}

interface VersionInfo {
  version: string;
  buildTimestamp: string;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [versionInfo, setVersionInfo] = useState<VersionInfo | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  useEffect(() => {
    fetch('/api/version')
      .then(response => response.json())
      .then(data => setVersionInfo(data))
      .catch(error => console.error('Error fetching version info:', error));
  }, []);

  return (
    <div className="min-h-screen text-slate-200 font-sans selection:bg-blue-500/30 overflow-x-hidden">
      {/* Hintergrund-Effekte */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">



      </div>

      {/* Navigation */}
      <nav className="relative z-50 border-b border-white/5 bg-black/20 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <div
            className="relative"
            onMouseEnter={() => setIsMenuOpen(true)}
            onMouseLeave={() => setIsMenuOpen(false)}
          >
            <div className="flex items-center space-x-3 cursor-pointer">
              <div className="p-2 bg-blue-600/20 rounded-lg border border-blue-500/30">
                <Server className="w-6 h-6 text-blue-400" />
              </div>
              <span className="text-xl font-bold tracking-tight text-white italic">
                codeheap<span className="text-blue-500">.dev</span>
              </span>
            </div>
            {isMenuOpen && (
              <div className="absolute top-full left-0 pt-2 w-48">
                <div className="bg-slate-800/90 backdrop-blur-md border border-slate-700 rounded-md shadow-lg">
                  <Link to="/" className="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-700">Home</Link>
                  <Link to="/tanken" className="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-700">Tanken</Link>
                  <Link to="/server-status" className="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-700">Server Status</Link>
                  <Link to="/mail-test" className="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-700">Mail Test</Link>
                </div>
              </div>
            )}
          </div>

          <div className="hidden md:flex items-center space-x-6">
            <StatusIndicator />
          </div>
        </div>
      </nav>

      {/* Content */}
      <div className="relative z-11">
        {children}
      </div>

      {/* Footer */}
      <footer className="relative z-10 border-t border-white/5 py-12 px-6">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between space-y-4 md:space-y-0 text-sm text-slate-500">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4" />
            <span>&copy; {new Date().getFullYear()} codeheap.dev - Deine Daten, deine Kontrolle.</span>
            {versionInfo && (
              <span className="hidden sm:inline-block ml-2 opacity-50 text-xs border-l border-slate-700 pl-2">
                v{versionInfo.version} ({versionInfo.buildTimestamp})
              </span>
            )}
          </div>
          <div className="flex items-center space-x-6">
            <Link to="/impressum" className="hover:text-white transition-colors">Impressum</Link>
            <a href="/datenschutz" className="hover:text-white transition-colors">Datenschutz</a>
            <div className="w-[1px] h-4 bg-white/10"></div>
            <span className="flex items-center text-blue-500">
              <div className="w-2 h-2 bg-blue-500 rounded-full mr-2 shadow-[0_0_8px_rgba(59,130,246,0.6)]"></div>
              Node: Hetzner
            </span>
          </div>
        </div>
      </footer>

      {/* Maintenance Popup */}
      <MaintenancePopup />
    </div>
  );
};

export default Layout;
