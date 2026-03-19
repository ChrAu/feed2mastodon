import { useState, useEffect } from 'react';
import {
  ExternalLink,
  Activity,
  ArrowUpRight,
  Sparkles,
  Server,
  ShieldCheck
} from 'lucide-react';
import { SERVICES } from '../data/services';
import StatusIndicator from '../components/StatusIndicator';
import ProxmoxDashboard from "../components/ProxmoxDashboard";
import PiHoleDashboard from "../components/PiHoleDashboard";

export default function Home() {
  const [isLoaded, setIsLoaded] = useState(false);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  useEffect(() => {
    setIsLoaded(true);
  }, []);

  return (
    <>
      {/* Hero Bereich */}
      <header className="relative z-10 pt-20 pb-16 px-6 overflow-hidden">
        {/* Animated Background Elements */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-3xl h-[400px] opacity-20 pointer-events-none">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-500 rounded-full mix-blend-screen filter blur-[100px] animate-[pulse_4s_cubic-bezier(0.4,0,0.6,1)_infinite]"></div>
          <div className="absolute top-1/2 left-1/3 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-cyan-500 rounded-full mix-blend-screen filter blur-[100px] animate-[blob_7s_infinite]" style={{ animationDelay: '2s' }}></div>
          <div className="absolute top-1/2 left-2/3 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-purple-500 rounded-full mix-blend-screen filter blur-[100px] animate-[blob_7s_infinite]" style={{ animationDelay: '4s' }}></div>
        </div>

        <div className="max-w-7xl mx-auto text-center relative z-10">
          <div className={`inline-flex items-center space-x-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 backdrop-blur-md mb-8 transition-all duration-1000 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`}>
            <Sparkles className="w-4 h-4 text-blue-400" />
            <span className="text-sm font-medium text-slate-300">Willkommen in deinem Workspace</span>
          </div>
          
          <h1 className={`text-5xl md:text-7xl font-extrabold text-white mb-8 tracking-tight transition-all duration-1000 delay-100 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`}>
            Deine Dienste, <br className="hidden md:block" />
            <span className="text-transparent bg-clip-text bg-[linear-gradient(to_right,#60a5fa,#22d3ee,#c084fc)] animate-[gradient_8s_linear_infinite]" style={{ backgroundSize: '300%' }}>
              vereint an einem Ort.
            </span>
          </h1>
          
          <p className={`text-slate-400 max-w-2xl mx-auto text-lg md:text-xl mb-12 leading-relaxed transition-all duration-1000 delay-300 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-4 opacity-0'}`}>
            Dezentral, sicher und vollständig selbstgehostet auf deiner Infrastruktur.
            Erlebe die Freiheit der Datenkontrolle.
          </p>
        </div>
      </header>

      {/* Service Grid */}
      <main className="relative z-10 max-w-7xl mx-auto px-6 pb-32">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 relative">
          
          {/* Background Grid Pattern */}
          <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none -z-10 [mask-image:radial-gradient(ellipse_60%_50%_at_50%_50%,#000_70%,transparent_100%)]"></div>

          {SERVICES.map((service, index) => {
            const isHovered = hoveredIndex === index;
            
            return (
              <div
                key={service.id}
                onMouseEnter={() => setHoveredIndex(index)}
                onMouseLeave={() => setHoveredIndex(null)}
                className={`group relative rounded-3xl transition-all duration-500 transform ${isLoaded ? 'translate-y-0 opacity-100' : 'translate-y-12 opacity-0'}`}
                style={{ transitionDelay: `${index * 100}ms` }}
              >
                {/* Neon Glow Background */}
                <div className={`absolute -inset-[1px] bg-[linear-gradient(to_right,var(--tw-gradient-stops))] ${service.color.replace('border-', 'from-').replace('-500', '-500/50')} to-transparent rounded-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 blur-sm`}></div>
                
                {/* Main Card Content */}
                <a href={service.url} target="_blank" rel="noopener noreferrer" className="block relative h-full p-6 sm:p-8 rounded-3xl bg-[#0a0f18]/90 border border-white/10 backdrop-blur-xl overflow-hidden flex flex-col group-hover:bg-[#0d1420]/90 transition-colors duration-500 shadow-2xl hover:shadow-[0_0_40px_rgba(0,0,0,0.5)]">
                  
                  {/* Decorative Elements */}
                  <div className={`absolute top-0 right-0 w-32 h-32 bg-[linear-gradient(to_bottom_right,rgba(255,255,255,0.05),transparent)] rounded-tr-3xl pointer-events-none transition-opacity duration-500 opacity-50 group-hover:opacity-100`}></div>
                  <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-[linear-gradient(to_right,transparent,currentColor,transparent)] opacity-0 group-hover:opacity-100 transition-opacity duration-500 ${service.text} pointer-events-none blur-[1px]`}></div>

                  {/* Top Area: Icon & Status */}
                  <div className="flex justify-between items-start mb-6 relative z-10">
                    <div className={`w-14 h-14 sm:w-16 sm:h-16 rounded-2xl flex items-center justify-center relative ${service.bg} border border-white/5 shadow-inner overflow-hidden`}>
                      <div className={`absolute inset-0 bg-[linear-gradient(to_bottom_right,rgba(255,255,255,0.2),transparent)] opacity-0 group-hover:opacity-100 transition-opacity duration-500`}></div>
                      <service.icon className={`w-7 h-7 sm:w-8 sm:h-8 ${service.text} relative z-10 transition-all duration-500 group-hover:scale-110 group-hover:rotate-3 drop-shadow-md`} />
                    </div>
                    
                    {service.monitorId && (
                      <div className="relative z-30 transition-transform duration-300 group-hover:-translate-y-1">
                        <StatusIndicator monitorId={service.monitorId} className="!bg-[#0a0f18]/80 backdrop-blur-md !px-2.5 !py-1 text-[10px] border-white/10 shadow-lg" />
                      </div>
                    )}
                  </div>
                  
                  {/* Content Area */}
                  <div className="relative z-10 flex-grow">
                    <h3 className={`text-xl sm:text-2xl font-bold text-white mb-2 sm:mb-3 tracking-tight group-hover:text-transparent group-hover:bg-clip-text group-hover:bg-[linear-gradient(to_right,#ffffff,#94a3b8)] transition-all duration-300`}>
                      {service.name}
                    </h3>
                    <p className="text-slate-400 text-xs sm:text-sm leading-relaxed mb-6 sm:mb-8 line-clamp-2 sm:line-clamp-3">
                      {service.description}
                    </p>
                  </div>
                  
                  {/* Bottom Area: Action Link */}
                  <div className="relative z-10 mt-auto pt-4 sm:pt-6 border-t border-white/5 flex items-center justify-between">
                    <span className={`text-[10px] sm:text-xs font-semibold uppercase tracking-widest text-slate-500 group-hover:${service.text} transition-colors duration-300 flex items-center`}>
                      Dienst Öffnen <ExternalLink className="w-3 h-3 ml-2 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300" />
                    </span>
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center bg-white/5 border border-white/5 group-hover:bg-white/10 group-hover:border-white/10 transition-all duration-300 shadow-sm ${isHovered ? 'animate-bounce' : ''}`}>
                      <ArrowUpRight className={`w-4 h-4 ${service.text} transition-transform duration-300 group-hover:rotate-12`} />
                    </div>
                  </div>
                </a>
              </div>
            );
          })}
          
          {/* Animated Status Card - Always takes up available space nicely */}
          <div className={`relative rounded-3xl p-[1px] bg-[linear-gradient(to_bottom,rgba(59,130,246,0.2),rgba(59,130,246,0.05),transparent)] transition-all duration-1000 delay-700 ${isLoaded ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-12'} md:col-span-2 lg:col-span-1 xl:col-span-1`}>
            <div className="h-full p-8 rounded-3xl bg-[#0a0f18]/90 backdrop-blur-xl flex flex-col justify-center items-center text-center group hover:bg-[#0d1420]/90 transition-colors duration-500 shadow-2xl overflow-hidden">
              
              {/* Radar Effect Background */}
              <div className="absolute inset-0 opacity-10 pointer-events-none">
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[200%] h-[200%] border-[0.5px] border-blue-500/30 rounded-full animate-[spin_10s_linear_infinite] border-t-transparent border-l-transparent"></div>
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[150%] h-[150%] border-[0.5px] border-blue-500/20 rounded-full animate-[spin_15s_linear_infinite_reverse] border-b-transparent border-r-transparent"></div>
              </div>

              <div className="w-20 h-20 rounded-full bg-blue-500/10 flex items-center justify-center mb-6 relative">
                <div className="absolute inset-0 bg-blue-500/20 rounded-full animate-ping opacity-20"></div>
                <div className="absolute inset-2 bg-blue-500/10 rounded-full animate-pulse"></div>
                <Activity className="w-10 h-10 text-blue-400 relative z-10" />
              </div>
              <h4 className="text-2xl font-bold text-white mb-3 relative z-10">System Status</h4>
              <p className="text-slate-400 text-sm max-w-[250px] mx-auto mb-8 relative z-10">Alle Kernsysteme operieren innerhalb normaler Parameter.</p>
              
              <div className="mt-auto flex items-center justify-center space-x-3 bg-emerald-500/10 px-4 py-2 rounded-full border border-emerald-500/20 relative z-10">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.8)]"></span>
                </span>
                <span className="text-xs font-bold text-emerald-400 uppercase tracking-widest">All Systems Operational</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Dashboard Section */}
      <section className="relative z-10 border-t border-white/5 bg-[linear-gradient(to_bottom,transparent,#050810)] pt-24 pb-32">
        <div className="max-w-7xl mx-auto px-6">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-16 space-y-4 md:space-y-0">
            <div className="flex items-center space-x-4">
              <div className="w-12 h-12 rounded-xl bg-slate-800 flex items-center justify-center border border-white/10 shadow-lg">
                 <Server className="w-6 h-6 text-slate-300" />
              </div>
              <div>
                <h2 className="text-3xl font-bold text-white tracking-tight">Infrastruktur Metriken</h2>
                <p className="text-slate-400 text-sm mt-1">Echtzeit-Überwachung der Kernsysteme</p>
              </div>
            </div>
            <div className="hidden md:flex space-x-2">
                <div className="h-1 w-12 bg-orange-500/50 rounded-full"></div>
                <div className="h-1 w-12 bg-red-500/50 rounded-full"></div>
            </div>
          </div>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12 items-stretch">
            {/* Proxmox Card */}
            <div className="group relative bg-[#0a0f18]/80 border border-white/5 rounded-[2rem] p-8 backdrop-blur-xl shadow-2xl hover:border-orange-500/30 transition-colors duration-500 overflow-hidden flex flex-col h-full">
               <div className="absolute top-0 right-0 w-64 h-64 bg-orange-500/5 rounded-full filter blur-[80px] -translate-y-1/2 translate-x-1/2 pointer-events-none group-hover:bg-orange-500/10 transition-colors duration-500"></div>
              
              <div className="flex items-center justify-between mb-8 relative z-10 border-b border-white/5 pb-6">
                <h3 className="text-2xl font-bold text-white flex items-center tracking-tight">
                  <div className="w-10 h-10 rounded-lg bg-orange-500/10 flex items-center justify-center mr-4 border border-orange-500/20">
                     <Server className="w-5 h-5 text-orange-400" />
                  </div>
                  Proxmox Hypervisor
                </h3>
                <div className="px-3 py-1 rounded-full bg-orange-500/10 border border-orange-500/20 text-orange-400 text-xs font-semibold uppercase tracking-wider">Node 1</div>
              </div>
              <div className="relative z-10 flex-grow flex flex-col justify-end">
                <ProxmoxDashboard />
              </div>
            </div>
            
            {/* Pi-Hole Card */}
            <div className="group relative bg-[#0a0f18]/80 border border-white/5 rounded-[2rem] p-8 backdrop-blur-xl shadow-2xl hover:border-red-500/30 transition-colors duration-500 overflow-hidden flex flex-col h-full">
               <div className="absolute top-0 right-0 w-64 h-64 bg-red-500/5 rounded-full filter blur-[80px] -translate-y-1/2 translate-x-1/2 pointer-events-none group-hover:bg-red-500/10 transition-colors duration-500"></div>

              <div className="flex items-center justify-between mb-8 relative z-10 border-b border-white/5 pb-6">
                <h3 className="text-2xl font-bold text-white flex items-center tracking-tight">
                  <div className="w-10 h-10 rounded-lg bg-red-500/10 flex items-center justify-center mr-4 border border-red-500/20">
                    <ShieldCheck className="w-5 h-5 text-red-400" />
                  </div>
                  Pi-Hole DNS
                </h3>
                 <div className="px-3 py-1 rounded-full bg-red-500/10 border border-red-500/20 text-red-400 text-xs font-semibold uppercase tracking-wider">Network</div>
              </div>
              <div className="relative z-10 flex-grow flex flex-col justify-end">
                <PiHoleDashboard />
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
