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
  const [maintenances, setMaintenances] = useState<MaintenanceInfo[]>([]);
  const [dismissedIds, setDismissedIds] = useState<number[]>([]);
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    fetch('/api/status/page')
      .then(response => response.json())
      .then((data: StatusPageResponse) => {
        const activeMaintenances = data.maintenanceList.filter(m => 
          m.active && (m.status === 'under-maintenance' || m.status === 'upcoming' || m.status === 'schedule')
        );
        
        if (activeMaintenances.length > 0) {
          setMaintenances(activeMaintenances);
          
          // Load dismissed IDs
          const storedDismissed = sessionStorage.getItem('dismissedMaintenanceIds');
          const legacyDismissed = sessionStorage.getItem('dismissedMaintenanceId');
          
          let initialDismissed: number[] = [];
          if (storedDismissed) {
             try {
               initialDismissed = JSON.parse(storedDismissed);
             } catch(e) {
               console.error('Error parsing dismissedMaintenanceIds', e);
             }
          } else if (legacyDismissed) {
             const parsed = parseInt(legacyDismissed);
             if (!isNaN(parsed)) {
                initialDismissed = [parsed];
             }
          }
          setDismissedIds(initialDismissed);
        }
        setHasLoaded(true);
      })
      .catch(error => {
        console.error('Error fetching maintenance info:', error);
        setHasLoaded(true);
      });
  }, []);

  const handleDismiss = (id: number) => {
    const newDismissed = [...dismissedIds, id];
    setDismissedIds(newDismissed);
    sessionStorage.setItem('dismissedMaintenanceIds', JSON.stringify(newDismissed));
  };

  const handleDismissAll = (ids: number[]) => {
    const newDismissed = [...dismissedIds, ...ids];
    setDismissedIds(newDismissed);
    sessionStorage.setItem('dismissedMaintenanceIds', JSON.stringify(newDismissed));
  };

  const handleShowAll = () => {
    setDismissedIds([]);
    sessionStorage.removeItem('dismissedMaintenanceIds');
    sessionStorage.removeItem('dismissedMaintenanceId');
  };

  if (!hasLoaded || maintenances.length === 0) {
    return null;
  }

  const visibleMaintenances = maintenances.filter(m => !dismissedIds.includes(m.id));
  const isAllDismissed = visibleMaintenances.length === 0;

  // Determine global status for minimized button
  const hasCritical = maintenances.some(m => m.status === 'under-maintenance');
  const borderColor = hasCritical ? 'border-amber-500' : 'border-blue-500';
  const iconColor = hasCritical ? 'text-amber-400' : 'text-blue-400';
  const headerText = hasCritical ? 'text-amber-400' : 'text-blue-400';

  // Minimized State (Floating Button)
  if (isAllDismissed) {
    return (
      <button 
        onClick={handleShowAll}
        className={`fixed bottom-4 right-4 z-50 flex items-center space-x-2 px-4 py-3 bg-[#0f172a] border ${borderColor} rounded-full shadow-lg hover:bg-[#1e293b] transition-all duration-300 group animate-in fade-in slide-in-from-bottom-4`}
      >
        <Wrench className={`w-5 h-5 ${iconColor} ${hasCritical ? 'animate-pulse' : ''}`} />
        <span className={`font-medium ${headerText} text-sm hidden group-hover:inline-block transition-opacity duration-300`}>
          Wartungsinfos ({maintenances.length})
        </span>
      </button>
    );
  }

  const MAX_VISIBLE = 2;
  const displayedMaintenances = visibleMaintenances.slice(0, MAX_VISIBLE);
  const remainingCount = Math.max(0, visibleMaintenances.length - MAX_VISIBLE);

  // Maximized State (List of Popups)
  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 md:items-end md:justify-end md:p-6 md:bg-transparent md:backdrop-blur-none pointer-events-none">
      <div className="flex flex-col space-y-4 w-full max-w-md max-h-[90vh] overflow-y-auto pointer-events-auto scrollbar-hide">
        {displayedMaintenances.map(maintenance => (
           <MaintenanceCard key={maintenance.id} maintenance={maintenance} onDismiss={() => handleDismiss(maintenance.id)} />
        ))}
        {remainingCount > 0 && (
            <div className="w-full bg-[#0f172a] border border-slate-700 rounded-xl shadow-lg overflow-hidden animate-in slide-in-from-bottom-10 fade-in duration-300">
                <div className="bg-slate-800/50 px-5 py-4 flex items-center justify-between">
                    <div className="flex items-center space-x-3 text-slate-300">
                        <AlertTriangle className="w-6 h-6" />
                        <span className="font-bold text-base">
                            +{remainingCount} weitere Wartung{remainingCount > 1 ? 'en' : ''}
                        </span>
                    </div>
                    <button 
                        onClick={() => {
                           const remainingIds = visibleMaintenances.slice(MAX_VISIBLE).map(m => m.id);
                           handleDismissAll(remainingIds);
                        }}
                        className="text-slate-400 hover:text-white transition-colors p-1.5 rounded-full hover:bg-white/10"
                        aria-label="Alle weiteren ausblenden"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>
                <div className="px-5 pb-4 text-sm text-slate-400">
                   Es liegen noch weitere Wartungsarbeiten vor. Bitte prüfen Sie die Statusseite für eine vollständige Übersicht.
                </div>
            </div>
        )}
      </div>
    </div>
  );
};

const MaintenanceCard: React.FC<{ maintenance: MaintenanceInfo; onDismiss: () => void }> = ({ maintenance, onDismiss }) => {
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

  return (
      <div className={`w-full bg-[#0f172a] border ${borderColor} rounded-xl ${glow} overflow-hidden animate-in slide-in-from-bottom-10 fade-in duration-300`}>
        {/* Header */}
        <div className={`${headerBg} px-5 py-4 border-b ${isCritical ? 'border-amber-500/30' : 'border-blue-500/30'} flex items-center justify-between`}>
          <div className={`flex items-center space-x-3 ${headerText}`}>
            <AlertTriangle className="w-6 h-6" />
            <span className="font-bold text-base uppercase tracking-wider">
              {isCritical ? 'Laufende Wartung' : 'Geplante Wartung'}
            </span>
          </div>
          <button 
            onClick={onDismiss}
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
  );
};

export default MaintenancePopup;