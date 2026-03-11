import React, { useState, useEffect } from 'react';
import { X, AlertTriangle, Calendar, Clock, Wrench } from 'lucide-react';

interface MaintenanceInfo {
  id: number;
  title: string;
  description: string;
  strategy: string;
  active: boolean;
  dateRange: string[];
  status: string;
}

interface StatusPageResponse {
  maintenanceList: MaintenanceInfo[];
}

const MaintenancePopup: React.FC = () => {
  const [maintenance, setMaintenance] = useState<MaintenanceInfo | null>(null);
  const [isDismissed, setIsDismissed] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    fetch('/api/status/page')
      .then(response => response.json())
      .then((data: StatusPageResponse) => {
        const activeMaintenance = data.maintenanceList.find(m => 
          m.active && (m.status === 'under-maintenance' || m.status === 'upcoming' || m.status === 'schedule')
        );
        
        if (activeMaintenance) {
          setMaintenance(activeMaintenance);
          // Wir nutzen sessionStorage statt localStorage, damit das Popup bei jedem neuen Tab/Session wieder erscheint
          // Das entspricht dem Wunsch "Beim initialen Laden der Seite darf das Fenster gerne offen sein"
          const dismissedId = sessionStorage.getItem('dismissedMaintenanceId');
          if (dismissedId === String(activeMaintenance.id)) {
            setIsDismissed(true);
          }
        }
        setHasLoaded(true);
      })
      .catch(error => {
        console.error('Error fetching maintenance info:', error);
        setHasLoaded(true);
      });
  }, []);

  const handleDismiss = () => {
    if (maintenance) {
      sessionStorage.setItem('dismissedMaintenanceId', String(maintenance.id));
      setIsDismissed(true);
    }
  };

  const handleShow = () => {
    setIsDismissed(false);
  };

  if (!hasLoaded || !maintenance) {
    return null;
  }

  const isCritical = maintenance.status === 'under-maintenance';
  const borderColor = isCritical ? 'border-amber-500' : 'border-blue-500';
  const headerBg = isCritical ? 'bg-amber-500/20' : 'bg-blue-500/20';
  const headerText = isCritical ? 'text-amber-400' : 'text-blue-400';
  const iconColor = isCritical ? 'text-amber-400' : 'text-blue-400';
  const glow = isCritical ? 'shadow-[0_0_30px_rgba(245,158,11,0.2)]' : 'shadow-[0_0_30px_rgba(59,130,246,0.2)]';

  // Helper for date formatting
  const formatDateRange = (isoStart: string, isoEnd: string) => {
    const startDate = new Date(isoStart);
    const endDate = new Date(isoEnd);
    const dateOptions: Intl.DateTimeFormatOptions = { day: '2-digit', month: '2-digit', year: 'numeric' };
    const timeOptions: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit' };
    
    return {
      date: startDate.toLocaleDateString('de-DE', dateOptions),
      startTime: startDate.toLocaleTimeString('de-DE', timeOptions),
      endTime: endDate.toLocaleTimeString('de-DE', timeOptions)
    };
  };

  const { date, startTime, endTime } = formatDateRange(maintenance.dateRange[0], maintenance.dateRange[1]);

  // Minimized State (Floating Button)
  if (isDismissed) {
    return (
      <button 
        onClick={handleShow}
        className={`fixed bottom-4 right-4 z-50 flex items-center space-x-2 px-4 py-3 bg-[#0f172a] border ${borderColor} rounded-full shadow-lg hover:bg-[#1e293b] transition-all duration-300 group animate-in fade-in slide-in-from-bottom-4`}
      >
        <Wrench className={`w-5 h-5 ${iconColor} ${isCritical ? 'animate-pulse' : ''}`} />
        <span className={`font-medium ${headerText} text-sm hidden group-hover:inline-block transition-opacity duration-300`}>
          Wartungsinfos
        </span>
      </button>
    );
  }

  // Maximized State (Popup)
  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 md:items-end md:justify-end md:p-6 md:bg-transparent md:backdrop-blur-none pointer-events-none">
      <div className={`pointer-events-auto w-full max-w-md bg-[#0f172a] border ${borderColor} rounded-xl ${glow} overflow-hidden animate-in slide-in-from-bottom-10 fade-in duration-300`}>
        {/* Header */}
        <div className={`${headerBg} px-5 py-4 border-b ${isCritical ? 'border-amber-500/30' : 'border-blue-500/30'} flex items-center justify-between`}>
          <div className={`flex items-center space-x-3 ${headerText}`}>
            <AlertTriangle className="w-6 h-6" />
            <span className="font-bold text-base uppercase tracking-wider">
              {isCritical ? 'Laufende Wartung' : 'Geplante Wartung'}
            </span>
          </div>
          <button 
            onClick={handleDismiss}
            className="text-slate-400 hover:text-white transition-colors p-1.5 rounded-full hover:bg-white/10"
            aria-label="Schließen"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        
        {/* Content */}
        <div className="p-6 space-y-5">
          <h3 className="text-xl font-bold text-slate-100">{maintenance.title}</h3>
          
          <div className={`flex flex-col space-y-2 text-sm text-slate-200 ${isCritical ? 'bg-amber-900/20 border-amber-500/30' : 'bg-slate-800/50 border-slate-700/50'} p-4 rounded-lg border`}>
            <div className="flex items-center space-x-3">
              <Calendar className={`w-5 h-5 ${iconColor}`} />
              <span className="font-medium">{date}</span>
            </div>
            <div className="flex items-center space-x-3">
              <Clock className={`w-5 h-5 ${iconColor}`} />
              <span className="font-medium">{startTime} - {endTime}</span>
            </div>
          </div>
          
          <div className="text-sm text-slate-300 leading-relaxed max-h-60 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-transparent">
             {maintenance.description.split('\n').map((line, i) => (
               <p key={i} className={`min-h-[1em] ${line.trim() === '' ? 'h-3' : 'mb-2'}`}>{line}</p>
             ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MaintenancePopup;