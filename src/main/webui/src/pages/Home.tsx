import { useState, useEffect } from 'react';
import {
  ChevronRight,
  ExternalLink,
  Activity
} from 'lucide-react';
import { SERVICES } from '../data/services';
import StatusIndicator from '../components/StatusIndicator';
import ProxmoxDashboard from "../components/ProxmoxDashboard";
import PiHoleDashboard from "../components/PiHoleDashboard";

const Home = () => {
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    setIsLoaded(true);
  }, []);

  return (
    <>
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
            <div
              key={service.id}
              className={`group relative p-8 rounded-3xl bg-slate-900/40 border border-white/5 backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-slate-800/60 shadow-xl ${service.glow} ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-8 opacity-0'}`}
              style={{ transitionDelay: `${index * 100}ms` }}
            >
              {/* Link Wrapper */}
              <a href={service.url} target="_blank" rel="noopener noreferrer" className="absolute inset-0 z-0" aria-label={`Öffne ${service.name}`}></a>

              {/* Dekorative Leiterbahnen-Stil-Elemente */}
              <div className={`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-white/5 to-transparent rounded-tr-3xl pointer-events-none`}></div>
              
              {/* Status Indicator (wenn monitorId vorhanden) */}
              {service.monitorId && (
                <div className="absolute top-4 right-4 z-20">
                  <StatusIndicator monitorId={service.monitorId} className="!bg-slate-900/80 backdrop-blur-md !px-2 !py-0.5 text-[10px]" />
                </div>
              )}

              <div className="relative z-10 pointer-events-none">
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
              <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-gradient-to-r from-transparent via-current to-transparent opacity-0 group-hover:opacity-100 transition-opacity ${service.text} pointer-events-none`}></div>
            </div>
          ))}
          
          {/* Status Card */}
          <div className={`p-8 rounded-3xl bg-blue-600/5 border border-blue-500/20 backdrop-blur-sm flex flex-col justify-center items-center text-center transition-all duration-1000 delay-700 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}>
            <Activity className="w-10 h-10 text-blue-500/50 mb-4" />
            <h4 className="text-white font-semibold mb-1">Server Status</h4>
            <p className="text-slate-500 text-xs">Alle Instanzen laufen stabil im Rechenzentrum.</p>
          </div>
        </div>
      </main>
        <div className="container mx-auto">
            <h1 className="text-xl font-bold p-4">Server Status</h1>
            <ProxmoxDashboard />
        </div>
        <div className="container mx-auto">
            <h1 className="text-xl font-bold p-4">Pi Hole Status</h1>
            <PiHoleDashboard />
        </div>
    </>
  );
};

export default Home;
