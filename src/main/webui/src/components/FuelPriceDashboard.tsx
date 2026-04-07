import {Clock, Fuel, X, RefreshCw, TrendingUp, TrendingDown} from 'lucide-react'; // X-Icon für den Schließen-Button, RefreshCw für Update-Info
import React, {useEffect, useState} from 'react';
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import FuelPriceCardSkeleton from './FuelPriceCardSkeleton'; // Import Skeleton

// Globale Konfiguration für die Prognoseberechnung
const FORECAST_DAYS_HISTORY = 5; // Anzahl der historischen Tage
const FORECAST_WEIGHT_BASE = 2;  // Basis für die exponentielle Gewichtung (z.B. 2 für normal stark, 3 für sehr stark)

// Angepasste Interfaces, um den Backend-DTOs zu entsprechen
interface FuelPrice {
  value: number;
  unit: string;
  lastChanged: string; // ZonedDateTime wird als String empfangen
  entityId: string; // Neu hinzugefügt
  previousValue?: number | null; // Optionaler vorheriger Wert
}

interface FuelStation {
  name: string;
  fuelPrices: { [key: string]: FuelPrice }; // Map<String, FuelPriceDto>
  status: boolean;
}

interface FuelPriceHistory {
  timestamp: string; // ZonedDateTime als String
  value: number;
  timestampMs: number; // Neu hinzugefügt für Recharts XAxis Skalierung
}

interface ChartDataPoint {
  timestampMs: number;
  value?: number;
  forecastValue?: number;
}

interface FuelPriceChartProps {
  entityId: string;
  fuelType: string;
  height?: number; // Optional height prop for chart
  durationHours?: number; // New prop for duration
  showForecast?: boolean; // New prop for forecast
}

// Helper function to get display name for fuel type
const getFuelTypeName = (fuelType: string) => {
  switch (fuelType) {
    case 'diesel': return 'Diesel';
    case 'super': return 'Super';
    case 'superE10': return 'Super E10';
    default: return fuelType;
  }
};

// Custom Tooltip component
interface CustomTooltipProps {
  active?: boolean;
  payload?: any[];
  label?: number; // Label ist jetzt ein numerischer Zeitstempel (ms)
  fuelTypeName: string;
}

const CustomTooltip: React.FC<CustomTooltipProps> = ({ active, payload, label, fuelTypeName }) => {
  if (active && payload && payload.length) {
    const date = new Date(label!); // Parse the numeric timestamp
    const formattedDate = date.toLocaleDateString('de-DE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });

    return (
      <div className="bg-slate-800 p-3 rounded-md border border-slate-700 text-white text-sm z-50 shadow-xl">
        <p className="font-bold mb-1">{formattedDate}</p>
        {payload.map((entry, index) => (
          <p key={index} style={{ color: entry.color }}>
            {entry.dataKey === 'value' ? `${fuelTypeName}: ` : `Prognose: `}
            {entry.value.toFixed(3)} €
          </p>
        ))}
      </div>
    );
  }
  return null;
};


const FuelPriceChart: React.FC<FuelPriceChartProps> = ({ entityId, fuelType, height = 200, durationHours = 24, showForecast = false }) => {
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [xTicks, setXTicks] = useState<number[]>([]); // All interval ticks for grid

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        // Für die Prognose benötigen wir die entsprechende Historie in Stunden
        const requiredHistoryHours = FORECAST_DAYS_HISTORY * 24;
        const fetchDuration = showForecast ? Math.max(durationHours, requiredHistoryHours) : durationHours;
        const response = await fetch(`/api/homeassistant/fuel-prices/history?entityId=${entityId}&durationHours=${fetchDuration}`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data: FuelPriceHistory[] = await response.json();
        const fullHistory = data.map(item => ({
          ...item,
          timestampMs: new Date(item.timestamp).getTime()
        }));

        let forecastData: ChartDataPoint[] = [];
        if (showForecast && fullHistory.length > 0) {
          const lastPoint = fullHistory[fullHistory.length - 1];
          const lastTimestampMs = lastPoint.timestampMs;

          // Generiere 12 Stunden Prognose im 5-Minuten-Takt
          const intervals = (12 * 60) / 5; // 144 Punkte
          for (let i = 1; i <= intervals; i++) {
            const futureMs = lastTimestampMs + i * 5 * 60 * 1000;

            let weightedSum = 0;
            let totalWeight = 0;

            // Durchschnitt der letzten X Tage (konfigurierbar) zur exakt gleichen Uhrzeit bilden
            for (let daysAgo = 1; daysAgo <= FORECAST_DAYS_HISTORY; daysAgo++) {
              const targetMs = futureMs - daysAgo * 24 * 60 * 60 * 1000;

              let lastKnownPoint = null;
              // Rückwärts suchen, um den neuesten Punkt VOR oder exakt ZUM targetMs zu finden
              for (let j = fullHistory.length - 1; j >= 0; j--) {
                if (fullHistory[j].timestampMs <= targetMs) {
                  lastKnownPoint = fullHistory[j];
                  break;
                }
              }

              // Falls der gesuchte Zeitpunkt kurz vor dem Anfang der Historie liegt,
              // nehmen wir den ältesten verfügbaren Punkt als beste Annäherung
              if (!lastKnownPoint && fullHistory.length > 0) {
                lastKnownPoint = fullHistory[0];
              }

              if (lastKnownPoint) {
                // Exponentielle Gewichtung: Jüngere Tage werden signifikant stärker gewichtet.
                // Basis FORECAST_WEIGHT_BASE sorgt für eine sehr starke Ausrichtung am aktuellen Trend.
                // Beispiel bei 5 Tagen Historie und Basis 3:
                // Tag 1 (gestern): 3^4 = 81
                // Tag 2: 3^3 = 27
                // Tag 3: 3^2 = 9
                // Tag 4: 3^1 = 3
                // Tag 5: 3^0 = 1
                const weight = Math.pow(FORECAST_WEIGHT_BASE, FORECAST_DAYS_HISTORY - daysAgo);

                weightedSum += lastKnownPoint.value * weight;
                totalWeight += weight;
              }
            }

            if (totalWeight > 0) {
              forecastData.push({
                timestampMs: futureMs,
                forecastValue: weightedSum / totalWeight
              });
            }
          }
        }

        const now = new Date().getTime();
        const minDisplayMs = now - durationHours * 60 * 60 * 1000;

        // Filter die Daten auf die vom Nutzer gewünschte Anzeigedauer
        let displayHistory: ChartDataPoint[] = fullHistory
          .filter(pt => pt.timestampMs >= minDisplayMs)
          .map(pt => ({ timestampMs: pt.timestampMs, value: pt.value }));

        // Verbinde die aktuelle Linie mit der Prognoselinie, indem der letzte echte Wert
        // auch als Startwert für die Prognose gesetzt wird
        if (displayHistory.length > 0 && forecastData.length > 0) {
          displayHistory[displayHistory.length - 1].forecastValue = displayHistory[displayHistory.length - 1].value;
        }

        const finalChartData = [...displayHistory, ...forecastData];
        setChartData(finalChartData);

        if (finalChartData.length > 0) {
          // Calculate tick interval based on duration
          let tickIntervalMs;
          const totalHours = durationHours + (showForecast ? 12 : 0);
          if (totalHours <= 24) {
             tickIntervalMs = 4 * 60 * 60 * 1000; // 4 hours
          } else if (totalHours <= 48) {
             tickIntervalMs = 8 * 60 * 60 * 1000; // 8 hours
          } else if (totalHours <= 96) {
             tickIntervalMs = 12 * 60 * 60 * 1000; // 12 hours
          } else {
             tickIntervalMs = 24 * 60 * 60 * 1000; // 24 hours
          }

          const generatedGridTicks: number[] = [];
          const minTimestamp = finalChartData[0].timestampMs;
          const maxTimestamp = finalChartData[finalChartData.length - 1].timestampMs;

          const startDate = new Date(minTimestamp);
          startDate.setHours(0, 0, 0, 0);

          let currentTickTime = startDate.getTime();

          while (currentTickTime < minTimestamp) {
            currentTickTime += tickIntervalMs;
          }

          // Ensure ticks do not go beyond the XAxis domain's upper bound
          const xAxisDomainUpperBound = maxTimestamp + 1000000;
          while (currentTickTime <= xAxisDomainUpperBound) {
            generatedGridTicks.push(currentTickTime);
            currentTickTime += tickIntervalMs;
          }
          setXTicks(generatedGridTicks);
        } else {
          setXTicks([]);
        }

      } catch (err) {
        console.error(`Failed to fetch history for ${entityId}:`, err);
        setError("Fehler beim Laden der Verlaufsdaten.");
      } finally {
        setLoading(false);
      }
    };

    fetchHistory().catch(console.error);
  }, [entityId, durationHours, showForecast]);

  if (loading) {
    return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-[100px] animate-pulse bg-slate-800/30 rounded-lg">Lade Verlauf...</div>;
  }

  if (error) {
    return <div className="text-red-400 text-center text-sm py-2 h-full flex items-center justify-center min-h-[100px]">Fehler: {error}</div>;
  }

  if (chartData.length === 0) {
    return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-[100px]">Keine Verlaufsdaten verfügbar.</div>;
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData} margin={{ top: 5, right: 40, left: 10, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
        <XAxis
          dataKey="timestampMs"
          type="number"
          scale="time"
          domain={['dataMin', 'dataMax + 1000000']}
          ticks={xTicks}
          tickFormatter={(timestampMs) => {
            const date = new Date(timestampMs);
            const timeString = date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
            const totalHours = durationHours + (showForecast ? 12 : 0);
            if (totalHours > 24 || timeString === '00:00') {
              return `${date.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })} ${totalHours <= 48 ? timeString : ''}`.trim();
            }
            return timeString;
          }}
          stroke="#94a3b8"
          minTickGap={20}
          tick={{ fontSize: 12 }}
          angle={-45}
          textAnchor="end"
          height={60}
        />
        <YAxis stroke="#94a3b8" domain={['dataMin - 0.01', 'dataMax + 0.01']} tickFormatter={(value) => value.toFixed(2)} />
        <Tooltip
          content={<CustomTooltip fuelTypeName={getFuelTypeName(fuelType)} />}
        />
        <Line type="stepAfter" dataKey="value" stroke="#8884d8" strokeWidth={2} dot={false} fill="none" isAnimationActive={false} />
        {showForecast && (
          <Line type="stepAfter" dataKey="forecastValue" stroke="#82ca9d" strokeWidth={2} strokeDasharray="5 5" dot={false} fill="none" isAnimationActive={false} />
        )}
      </LineChart>
    </ResponsiveContainer>
  );
};


const FuelPriceDashboard: React.FC = () => {
  const updateIntervalSeconds = 5 * 60; // 5 Minuten
  const [fuelStations, setFuelStations] = useState<FuelStation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalFuelPrice, setModalFuelPrice] = useState<{ fuelPrice: FuelPrice; fuelType: string; stationName: string } | null>(null);
  const [modalDurationHours, setModalDurationHours] = useState<number>(24);
  const [showForecastModal, setShowForecastModal] = useState<boolean>(true); // Neu: Toggle für Prognose
  const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds); // Countdown state

  useEffect(() => {
    const fetchFuelPrices = async () => {
      try {
        const response = await fetch('/api/homeassistant/fuel-prices');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data: FuelStation[] = await response.json();

        // Sortiere die Tankstellen nach der gewünschten Reihenfolge
        const sortedData = [...data].sort((a, b) => {
          if (a.name === "ARAL Gosbach") return -1; // ARAL Gosbach kommt zuerst
          if (b.name === "ARAL Gosbach") return 1;

          if (a.name === "TotalEnergies Deggingen") {
            if (b.name === "ARAL Gosbach") return 1; // TotalEnergies Deggingen kommt nach ARAL Gosbach
            return -1; // TotalEnergies Deggingen kommt vor allen anderen, außer ARAL Gosbach
          }
          if (b.name === "TotalEnergies Deggingen") {
            if (a.name === "ARAL Gosbach") return -1; // TotalEnergies Deggingen kommt nach ARAL Gosbach
            return 1; // TotalEnergies Deggingen kommt vor allen anderen, außer ARAL Gosbach
          }

          return 0; // Behalte die ursprüngliche Reihenfolge für andere Tankstellen
        });

        setFuelStations(sortedData);
        setNextUpdate(updateIntervalSeconds); // Reset countdown on successful fetch
        setError(null);

      } catch (err) {
        console.error("Failed to fetch fuel prices:", err);
        setError("Fehler beim Laden der Tankstellendaten.");
      } finally {
        setLoading(false);
      }
    };

    fetchFuelPrices().catch(console.error);

    // Refresh data in intervall
    const interval = setInterval(fetchFuelPrices, updateIntervalSeconds * 1000);

    // Countdown timer for next update UI
    const countdownInterval = setInterval(() => {
        setNextUpdate(prev => (prev > 0 ? prev - 1 : updateIntervalSeconds));
    }, 1000);

    return () => {
        clearInterval(interval);
        clearInterval(countdownInterval);
    };
  }, []);

  const formatTimeAgo = (isoString: string) => {
    const date = new Date(isoString);
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return `${seconds} Sekunden`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes} Minuten`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} Stunden`;
    const days = Math.floor(hours / 24);
    return `${days} Tagen`;
  };

  const openModal = (fuelPrice: FuelPrice, fuelType: string, stationName: string) => {
    setModalFuelPrice({ fuelPrice, fuelType, stationName });
    setModalDurationHours(24); // Reset to default when opening
    setShowForecastModal(true); // Default: Prognose an
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setModalFuelPrice(null);
  };

  if (loading && fuelStations.length === 0) {
    return (
        <div className="space-y-8 relative mt-8">
            <div className="flex justify-end mb-4">
                <div className="h-8 bg-slate-800/80 rounded-full w-48 animate-pulse border border-slate-700"></div>
            </div>
            {[...Array(2)].map((_, i) => (
                <FuelPriceCardSkeleton key={i} />
            ))}
        </div>
    );
  }

  if (error && fuelStations.length === 0) {
    return <div className="text-red-400 text-center py-4 bg-red-900/20 border border-red-500/50 rounded-xl p-4 mt-8">Fehler: {error}</div>;
  }

  // Helper function to format seconds into minutes and seconds
  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (mins > 0) {
      return `${mins}m ${secs.toString().padStart(2, '0')}s`;
    }
    return `${secs}s`;
  };

  const renderPriceDifference = (currentValue: number, previousValue?: number | null) => {
    if (previousValue === undefined || previousValue === null) return null;

    const diff = currentValue - previousValue;
    if (Math.abs(diff) < 0.001) return null; // Ignore very small differences

    const diffInCents = Math.round(diff * 100);
    const isIncrease = diff > 0;
    const colorClass = isIncrease ? 'text-red-400' : 'text-green-400';
    const Icon = isIncrease ? TrendingUp : TrendingDown;
    const sign = isIncrease ? '+' : '';

    return (
      <div className={`flex items-center text-xs mt-1 ${colorClass}`}>
        <Icon className="w-3 h-3 mr-1" />
        <span>{sign}{diffInCents} Cent</span>
      </div>
    );
  };

  return (
    <div className="space-y-8 relative">
      {/* Nächstes Update Info */}
      <div className="flex justify-end mb-4 mt-8">
        <div className="flex items-center text-sm text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-full border border-slate-700 shadow-sm backdrop-blur-sm z-10">
          <RefreshCw className={`w-4 h-4 mr-2 ${nextUpdate < 5 ? 'animate-spin text-blue-400' : ''}`} />
          <span>Nächstes Update in {formatCountdown(nextUpdate)}</span>
        </div>
      </div>

      {fuelStations.map((station, index) => (
        <div key={index} className="bg-slate-800/50 p-6 rounded-xl border border-slate-700 shadow-lg mt-8">
          <h4 className="text-xl font-bold text-white mb-4 flex items-center">
            <Fuel className="w-6 h-6 mr-2 text-blue-400" />
            {station.name}
            {station.status !== undefined && (
              <span className={`ml-3 px-2 py-1 text-xs font-semibold rounded-full ${station.status ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                {station.status ? 'Geöffnet' : 'Geschlossen'}
              </span>
            )}
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            {Object.entries(station.fuelPrices).map(([fuelType, fuelPrice]) => (
              <div
                key={fuelType}
                className="bg-slate-900/50 p-4 rounded-lg border border-slate-700 cursor-pointer hover:bg-slate-800/70 transition-colors duration-200 relative z-0"
                onClick={() => openModal(fuelPrice, fuelType, station.name)}
              >
                <p className="text-slate-300 text-sm mb-1">
                  {getFuelTypeName(fuelType)}
                </p>
                <div className="flex flex-col">
                  <p className="text-2xl font-bold text-white flex items-baseline">
                    {fuelPrice.value.toFixed(3)}<span className="ml-1">€</span>
                  </p>
                  {renderPriceDifference(fuelPrice.value, fuelPrice.previousValue)}
                </div>
                <p className="text-slate-500 text-xs mt-2 flex items-center">
                  <Clock className="w-3 h-3 mr-1" />
                  Vor {formatTimeAgo(fuelPrice.lastChanged)}
                </p>
                {/* Das Diagramm in den Kacheln verwendet jetzt wieder die Standardhöhe (200px) ohne Prognose */}
                <div className="mt-4">
                  <FuelPriceChart entityId={fuelPrice.entityId} fuelType={fuelType} durationHours={24} />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {/* Modal-Fenster */}
      {isModalOpen && modalFuelPrice && (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4" onClick={closeModal}>
          <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-lg w-full max-w-4xl relative flex flex-col max-h-[90vh]" onClick={e => e.stopPropagation()}>
            <button onClick={closeModal} className="absolute top-4 right-4 text-slate-400 hover:text-white z-10">
              <X size={24} />
            </button>

            <div className="flex justify-between items-center mb-6 mr-8">
              <h3 className="text-2xl font-bold text-white">
                {modalFuelPrice.stationName} - {getFuelTypeName(modalFuelPrice.fuelType)}
              </h3>

              <div className="flex items-center space-x-6">
                {/* Prognose-Toggle */}
                <label className="flex items-center space-x-2 text-sm text-slate-300 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={showForecastModal}
                    onChange={(e) => setShowForecastModal(e.target.checked)}
                    className="rounded border-slate-600 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900 w-4 h-4"
                  />
                  <span>12h Prognose</span>
                </label>

                {/* Zeitraum-Auswahl */}
                <div className="flex space-x-2 bg-slate-900 rounded-lg p-1">
                  {[
                    { label: '24h', value: 24 },
                    { label: '3 Tage', value: 72 },
                    { label: '7 Tage', value: 168 }
                  ].map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setModalDurationHours(option.value)}
                      className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                        modalDurationHours === option.value
                          ? 'bg-blue-600 text-white'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex-grow min-h-[400px]">
              <FuelPriceChart
                entityId={modalFuelPrice.fuelPrice.entityId}
                fuelType={modalFuelPrice.fuelType}
                height={400}
                durationHours={modalDurationHours}
                showForecast={showForecastModal}
              />
            </div>

            <div className="mt-4 text-slate-300 flex justify-between items-center">
              <div>
                Aktueller Preis: <span className="font-bold text-white">{modalFuelPrice.fuelPrice.value.toFixed(3)} €</span>
              </div>
              <div className="text-sm text-slate-500">
                Letzte Änderung: {new Date(modalFuelPrice.fuelPrice.lastChanged).toLocaleString('de-DE')}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FuelPriceDashboard;
