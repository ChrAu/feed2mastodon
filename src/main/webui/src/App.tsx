import React, { useState, useEffect } from 'react';
import { 
  Shield, 
  Cloud, 
  Mail, 
  BookOpen, 
  Edit3, 
  FileText, 
  Lock, 
  Activity,
  ExternalLink,
  ChevronRight,
  Server
} from 'lucide-react';

/**
 * Konfiguration der Dienste basierend auf dem Bild und der Anfrage.
 * Jede Farbe entspricht dem visuellen Stil des Dienstes im Bild.
 */
const SERVICES = [
  {
    id: 'keycloak',
    name: 'Keycloak',
    description: 'Login-Dienst (IAM)',
    icon: Shield,
    color: 'border-purple-500',
    glow: 'group-hover:shadow-purple-500/50',
    bg: 'bg-purple-500/10',
    text: 'text-purple-400',
    url: 'https://sso.codeheap.dev'
  },
  {
    id: 'nextcloud',
    name: 'Nextcloud',
    description: 'Daten & Kollaboration',
    icon: Cloud,
    color: 'border-blue-500',
    glow: 'group-hover:shadow-blue-500/50',
    bg: 'bg-blue-500/10',
    text: 'text-blue-400',
    url: 'https://nextcloud.codeheap.dev'
  },
  {
    id: 'mailcow',
    name: 'Mailcow',
    description: 'Sicherer Mailserver',
    icon: Mail,
    color: 'border-cyan-500',
    glow: 'group-hover:shadow-cyan-500/50',
    bg: 'bg-cyan-500/10',
    text: 'text-cyan-400',
    url: 'https://mail.codeheap.dev'
  },
  {
    id: 'vaultwarden',
    name: 'Vaultwarden',
    description: 'Passwort-Manager',
    icon: Lock,
    color: 'border-emerald-500',
    glow: 'group-hover:shadow-emerald-500/50',
    bg: 'bg-emerald-500/10',
    text: 'text-emerald-400',
    url: 'https://vaultwarden.codeheap.dev'
  },
  {
    id: 'audiobookshelf',
    name: 'Audiobookshelf',
    description: 'Hörbücher & Podcasts',
    icon: BookOpen,
    color: 'border-red-500',
    glow: 'group-hover:shadow-red-500/50',
    bg: 'bg-red-500/10',
    text: 'text-red-400',
    url: 'https://audiobookshelf.codeheap.dev'
  },
  {
    id: 'blinko',
    name: 'Blinko',
    description: 'Schnelle Notizen',
    icon: Edit3,
    color: 'border-amber-500',
    glow: 'group-hover:shadow-amber-500/50',
    bg: 'bg-amber-500/10',
    text: 'text-amber-400',
    url: 'https://blinko.codeheap.dev'
  },
  {
    id: 'paperless',
    name: 'Paperless-ngx',
    description: 'Dokumenten-Management',
    icon: FileText,
    color: 'border-orange-500',
    glow: 'group-hover:shadow-orange-500/50',
    bg: 'bg-orange-500/10',
    text: 'text-orange-400',
    url: 'https://paperless.codeheap.dev'
  }
];

const StatusIndicator = () => {
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    // Wir nutzen den Proxy-Endpunkt, um CORS-Probleme zu vermeiden.
    fetch('/api/status')
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.json();
      })
      .then(data => {
        if (!data.heartbeatList) {
          setStatus('offline');
          return;
        }

        // Wir prüfen nur den Monitor mit der ID 7
        const monitorId = '7';
        const monitorHeartbeats = data.heartbeatList[monitorId];
        if (Array.isArray(monitorHeartbeats) && monitorHeartbeats.length > 0) {
          const latestHeartbeat = monitorHeartbeats[monitorHeartbeats.length - 1];
          debugger;
          if (latestHeartbeat.status.integral === true) {
            setStatus('online');
          } else {
            setStatus('offline');
          }
        } else {
          // Wenn keine Heartbeats für Monitor 7 gefunden wurden, gehen wir von offline aus (oder unbekannt)
          setStatus('offline');
        }
      })
      .catch(() => {
        setStatus('offline');
      });
  }, []);

  if (status === 'loading') {
    return (
      <div className="flex items-center space-x-2 px-3 py-1 bg-gray-500/10 border border-gray-500/20 rounded-full">
        <div className="w-2 h-2 bg-gray-500 rounded-full"></div>
        <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Lade Status...</span>
      </div>
    );
  }

  if (status === 'offline') {
    return (
      <a href="https://kuma.codeheap.dev/status/codeheap" target="_blank" rel="noopener noreferrer" className="flex items-center space-x-2 px-3 py-1 bg-red-500/10 border border-red-500/20 rounded-full">
        <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
        <span className="text-xs font-medium text-red-400 uppercase tracking-wider">Störung</span>
      </a>
    );
  }

  return (
    <a href="https://kuma.codeheap.dev/status/codeheap" target="_blank" rel="noopener noreferrer" className="flex items-center space-x-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 rounded-full">
      <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></div>
      <span className="text-xs font-medium text-emerald-400 uppercase tracking-wider">Systeme Online</span>
    </a>
  );
};


const App = () => {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    setIsLoaded(true);
  }, []);

  return (
    <div className="min-h-screen bg-[#06090f] text-slate-200 font-sans selection:bg-blue-500/30 overflow-x-hidden">
      {/* Hintergrund-Effekte */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-600/10 blur-[120px] rounded-full"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600/10 blur-[120px] rounded-full"></div>
        <div className="absolute inset-0 opacity-[0.03]" style={{ backgroundImage: 'radial-gradient(#fff 1px, transparent 1px)', backgroundSize: '40px 40px' }}></div>
      </div>

      {/* Navigation */}
      <nav className="relative z-10 border-b border-white/5 bg-black/20 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-600/20 rounded-lg border border-blue-500/30">
              <Server className="w-6 h-6 text-blue-400" />
            </div>
            <span className="text-xl font-bold tracking-tight text-white italic">
              codeheap<span className="text-blue-500">.dev</span>
            </span>
          </div>
          
          <div className="hidden md:flex items-center space-x-6">
            <StatusIndicator />
          </div>
        </div>
      </nav>

      {/* Hero Bereich */}
      <header className="relative z-10 pt-16 pb-12 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <h1 className={`text-4xl md:text-6xl font-extrabold text-white mb-6 transition-all duration-1000 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`}>
            Deine Dienste, vereint <br className="hidden md:block" />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-purple-400">
              an einem Ort.
            </span>
          </h1>
          <p className={`text-slate-400 max-w-2xl mx-auto text-lg mb-10 transition-all duration-1000 delay-300 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`}>
            Dezentral, sicher und vollständig selbstgehostet auf deiner Infrastruktur.
            Wähle einen Dienst aus, um fortzufahren.
          </p>
        </div>
      </header>

      {/* Service Grid */}
      <main className="relative z-10 max-w-7xl mx-auto px-6 pb-24">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {SERVICES.map((service, index) => (
            <a
              key={service.id}
              href={service.url}
              className={`group relative p-8 rounded-3xl bg-slate-900/40 border border-white/5 backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-slate-800/60 shadow-xl ${service.glow} ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-8 opacity-0'}`}
              style={{ transitionDelay: `${index * 100}ms` }}
            >
              {/* Dekorative Leiterbahnen-Stil-Elemente */}
              <div className={`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-white/5 to-transparent rounded-tr-3xl pointer-events-none`}></div>
              
              <div className="relative z-10">
                <div className={`w-14 h-14 ${service.bg} border ${service.color} rounded-2xl flex items-center justify-center mb-6 transition-transform duration-500 group-hover:scale-110 group-hover:rotate-3 shadow-lg shadow-black/50`}>
                  <service.icon className={`w-7 h-7 ${service.text}`} />
                </div>
                
                <h3 className="text-xl font-bold text-white mb-2 flex items-center">
                  {service.name}
                  <ChevronRight className="w-4 h-4 ml-1 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all text-blue-400" />
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  {service.description}
                </p>
                
                <div className="flex items-center text-xs font-semibold uppercase tracking-widest text-slate-500 group-hover:text-blue-400 transition-colors">
                  Dienst Starten <ExternalLink className="w-3 h-3 ml-2" />
                </div>
              </div>

              {/* Unterer Glow-Streifen */}
              <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-gradient-to-r from-transparent via-current to-transparent opacity-0 group-hover:opacity-100 transition-opacity ${service.text}`}></div>
            </a>
          ))}
          
          {/* Status Card */}
          <div className={`p-8 rounded-3xl bg-blue-600/5 border border-blue-500/20 backdrop-blur-sm flex flex-col justify-center items-center text-center transition-all duration-1000 delay-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
            <Activity className="w-10 h-10 text-blue-500/50 mb-4" />
            <h4 className="text-white font-semibold mb-1">Server Status</h4>
            <p className="text-slate-500 text-xs">Alle Instanzen laufen stabil im Rechenzentrum.</p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 border-t border-white/5 py-12 px-6">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between space-y-4 md:space-y-0 text-sm text-slate-500">
          <div className="flex items-center space-x-2">
            <Shield className="w-4 h-4" />
            <span>&copy; {new Date().getFullYear()} codeheap.dev - Deine Daten, deine Kontrolle.</span>
          </div>
          <div className="flex items-center space-x-6">
            <a href="#" className="hover:text-white transition-colors">Impressum</a>
            <a href="#" className="hover:text-white transition-colors">Datenschutz</a>
            <div className="w-[1px] h-4 bg-white/10"></div>
            <span className="flex items-center text-blue-500">
              <div className="w-2 h-2 bg-blue-500 rounded-full mr-2 shadow-[0_0_8px_rgba(59,130,246,0.6)]"></div>
              Node: DE-FRA-01
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default App;