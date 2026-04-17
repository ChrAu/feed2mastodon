import React, { useEffect, useState, useCallback } from 'react';
import { CarDataHistoryChart, VisibleCarLinesState } from '../components/CarDataHistoryChart';
import { RefreshCw } from 'lucide-react';

interface CarData {
  odometer: number | null;
  electricRange: number | null;
  batteryLevel: number | null;
  externalTemperature: number | null;
  lastUpdate: string | null;
}

const HISTORY_OPTIONS: { label: string; value: number }[] = [
    { label: '24h', value: 24 },
    { label: '3 Tage', value: 72 },
    { label: '7 Tage', value: 168 },
    { label: '30 Tage', value: 720 }
];

const Auto = () => {
  const updateIntervalSeconds = 5 * 60; // 5 Minuten
  const [carData, setCarData] = useState<CarData>({
    odometer: null,
    electricRange: null,
    batteryLevel: null,
    externalTemperature: null,
    lastUpdate: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds);

  const [chartDurationHours, setChartDurationHours] = useState<number>(24);
  const [visibleLines, setVisibleLines] = useState<VisibleCarLinesState>({
    electricRange: true,
    batteryLevel: true,
    externalTemperature: true,
  });
  const [isCheckMode, setIsCheckMode] = useState<boolean>(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setIsCheckMode(params.get('check') === 'true');
  }, []);

  useEffect(() => {
    const fetchCarData = async () => {
      try {
        const response = await fetch('/api/homeassistant/car-data');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setCarData({
          odometer: data.odometer,
          electricRange: data.electricRange,
          batteryLevel: data.batteryLevel,
          externalTemperature: data.externalTemperature,
          lastUpdate: data.lastUpdate,
        });
        setNextUpdate(updateIntervalSeconds);
        setError(null);
      } catch (e: any) {
        console.error("Failed to fetch car data:", e);
        setError("Fehler beim Laden der Fahrzeugdaten.");
      } finally {
        setLoading(false);
      }
    };

    fetchCarData();
    const interval = setInterval(fetchCarData, updateIntervalSeconds * 1000);
    const countdownInterval = setInterval(() => setNextUpdate(prev => (prev > 0 ? prev - 1 : updateIntervalSeconds)), 1000);

    return () => {
        clearInterval(interval);
        clearInterval(countdownInterval);
    };
  }, []);

  const toggleLineVisibility = useCallback((lineKey: keyof VisibleCarLinesState) => {
    setVisibleLines(prev => ({
      ...prev,
      [lineKey]: !prev[lineKey],
    }));
  }, []);

  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins > 0) return `${mins}m ${secs.toString().padStart(2, '0')}s`;
    return `${secs}s`;
  };

  if (loading && !carData.odometer) { // Check for a key piece of data to indicate initial load
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error && !carData.odometer) {
    return <div className="text-center text-red-500 p-4">Fehler beim Laden der Daten: {error}</div>;
  }

  return (
    <div className="container mx-auto px-6 max-w-7xl pb-24">
      <div className="mt-16 text-center">
        <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
          ID.4 <br className="hidden md:block" />
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-teal-400">
            Übersicht & Statistik
          </span>
        </h2>
      </div>

      <div className="space-y-8 relative">
        <div className="flex justify-end mb-4 mt-8">
            <div className="flex items-center text-sm text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-full border border-slate-700 shadow-sm backdrop-blur-sm z-10">
                <RefreshCw className={`w-4 h-4 mr-2 ${nextUpdate < 5 ? 'animate-spin text-blue-400' : ''}`} />
                <span>Nächstes Update in {formatCountdown(nextUpdate)}</span>
            </div>
        </div>

        <div className="bg-slate-800 p-4 sm:p-6 rounded-xl border border-slate-700 shadow-lg w-full relative flex flex-col">
          <div className="flex flex-col mb-4 sm:mb-6">
            <h3 className="text-xl sm:text-2xl font-bold text-white mb-4">Aktuelle Werte</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-slate-300">
              <div className="bg-slate-700/50 p-3 rounded-lg">
                <h4 className="text-sm font-semibold text-slate-400">Kilometerstand</h4>
                <p className="text-lg font-bold">{carData.odometer !== null ? `${carData.odometer} km` : 'N/A'}</p>
              </div>
              <div className="bg-slate-700/50 p-3 rounded-lg">
                <h4 className="text-sm font-semibold text-slate-400">Reichweite</h4>
                <p className="text-lg font-bold">{carData.electricRange !== null ? `${carData.electricRange} km` : 'N/A'}</p>
              </div>
              <div className="bg-slate-700/50 p-3 rounded-lg">
                <h4 className="text-sm font-semibold text-slate-400">Batteriestand</h4>
                <p className="text-lg font-bold">{carData.batteryLevel !== null ? `${carData.batteryLevel} %` : 'N/A'}</p>
              </div>
              <div className="bg-slate-700/50 p-3 rounded-lg">
                <h4 className="text-sm font-semibold text-slate-400">Außentemperatur</h4>
                <p className="text-lg font-bold">{carData.externalTemperature !== null ? `${carData.externalTemperature} °C` : 'N/A'}</p>
              </div>
            </div>
            {carData.lastUpdate && (
                <p className="text-sm text-slate-500 mt-4">Zuletzt aktualisiert: {new Date(carData.lastUpdate).toLocaleString('de-DE')}</p>
            )}
          </div>

          <div className="flex flex-col gap-4 w-full mt-8">
            <h3 className="text-xl sm:text-2xl font-bold text-white mb-2">Verlauf</h3>
            {/* Data Range Options Row */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2">
                <span className="text-slate-400 text-sm font-medium min-w-30">Historie:</span>
                <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto">
                    {HISTORY_OPTIONS.map((option) => (
                        <button
                            key={option.value}
                            onClick={() => setChartDurationHours(option.value)}
                            className={`flex-1 sm:flex-none px-3 py-1.5 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                                chartDurationHours === option.value
                                    ? 'bg-blue-600 text-white'
                                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                            }`}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Line Visibility Toggles */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2 mt-4">
                <span className="text-slate-400 text-sm font-medium min-w-30">Sichtbare Linien:</span>
                <div className="flex flex-wrap gap-4">
                    <label className="flex items-center text-slate-300 cursor-pointer">
                        <input
                            type="checkbox"
                            className="form-checkbox h-4 w-4 text-blue-600 rounded"
                            checked={visibleLines.electricRange}
                            onChange={() => toggleLineVisibility('electricRange')}
                        />
                        <span className="ml-2">Reichweite</span>
                    </label>
                    <label className="flex items-center text-slate-300 cursor-pointer">
                        <input
                            type="checkbox"
                            className="form-checkbox h-4 w-4 text-green-600 rounded"
                            checked={visibleLines.batteryLevel}
                            onChange={() => toggleLineVisibility('batteryLevel')}
                        />
                        <span className="ml-2">Batteriestand</span>
                    </label>
                    <label className="flex items-center text-slate-300 cursor-pointer">
                        <input
                            type="checkbox"
                            className="form-checkbox h-4 w-4 text-yellow-600 rounded"
                            checked={visibleLines.externalTemperature}
                            onChange={() => toggleLineVisibility('externalTemperature')}
                        />
                        <span className="ml-2">Außentemperatur</span>
                    </label>
                </div>
            </div>

            <div className="shrink-0 w-full relative mt-4">
                <CarDataHistoryChart
                    height={400}
                    durationHours={chartDurationHours}
                    isCheckMode={isCheckMode}
                    visibleLines={visibleLines}
                />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Auto;
