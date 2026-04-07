import {Clock, Fuel, X, RefreshCw, TrendingUp, TrendingDown, ChevronDown, ChevronUp} from 'lucide-react'; // X-Icon für den Schließen-Button, RefreshCw für Update-Info
import React, {useEffect, useState} from 'react';
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import FuelPriceCardSkeleton from './FuelPriceCardSkeleton'; // Import Skeleton

// Globale Konfiguration für die Prognoseberechnung
const FORECAST_DAYS_HISTORY = 7; // Auf 7 Tage angepasst
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
  aiForecastValue?: number;
}

interface FuelPriceChartProps {
  entityId: string;
  fuelType: string;
  height?: number; // Optional height prop for chart
  durationHours?: number; // New prop for duration
  selectedForecastOption?: 'none' | 'trend_12h_holt' | '24h_holt' | '48h_holt'; // New prop
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
        {payload.map((entry, index) => {
          let labelText = '';
          if (entry.dataKey === 'value') labelText = `${fuelTypeName}: `;
          else if (entry.dataKey === 'forecastValue') labelText = 'Prognose (Trend): ';
          else if (entry.dataKey === 'aiForecastValue') labelText = 'Prognose (Holt-Winters): ';

          return (
            <p key={index} style={{ color: entry.color }}>
              {labelText}
              {Number(entry.value).toFixed(3)} €
            </p>
          );
        })}
      </div>
    );
  }
  return null;
};


const FuelPriceChart: React.FC<FuelPriceChartProps> = ({ entityId, fuelType, height = 200, durationHours = 24, selectedForecastOption = 'none' }) => {
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [xTicks, setXTicks] = useState<number[]>([]); // All interval ticks for grid
  const [xDomain, setXDomain] = useState<number[]>([0, 0]);
  const [loadingAiForecast, setLoadingAiForecast] = useState(false); // New state for AI forecast loading

  useEffect(() => {
    let isMounted = true;
    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const isTrendForecastActive = selectedForecastOption === 'trend_12h_holt';
        const isHoltWintersForecastActive = selectedForecastOption === 'trend_12h_holt' || selectedForecastOption === '24h_holt' || selectedForecastOption === '48h_holt';

        // Für die Prognose benötigen wir die entsprechende Historie in Stunden
        const requiredHistoryForTrend = FORECAST_DAYS_HISTORY * 24;
        const fetchDuration = isTrendForecastActive ? Math.max(durationHours, requiredHistoryForTrend) : durationHours;

        const historyResponse = await fetch(`/api/homeassistant/fuel-prices/history?entityId=${entityId}&durationHours=${fetchDuration}`);

        if (!historyResponse.ok) {
          throw new Error(`HTTP error! status: ${historyResponse.status}`);
        }

        const data: FuelPriceHistory[] = await historyResponse.json();
        if (!isMounted) return;

        const fullHistory = data.map(item => ({
          ...item,
          timestampMs: new Date(item.timestamp).getTime()
        }));

        let forecastData: ChartDataPoint[] = [];
        if (isTrendForecastActive && fullHistory.length > 0) {
          const lastPoint = fullHistory[fullHistory.length - 1];
          const lastTimestampMs = lastPoint.timestampMs;

          let previousForecastValue = lastPoint.value;
          const berlinTimeFormatter = new Intl.DateTimeFormat('de-DE', {
            timeZone: 'Europe/Berlin',
            hour: 'numeric'
          });

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
                const weight = Math.pow(FORECAST_WEIGHT_BASE, FORECAST_DAYS_HISTORY - daysAgo);

                weightedSum += lastKnownPoint.value * weight;
                totalWeight += weight;
              }
            }

            if (totalWeight > 0) {
              const rawForecastValue = weightedSum / totalWeight;
              let forecastValue = rawForecastValue;

              // Extrahiere die Stunde in deutscher Zeit
              const hourString = berlinTimeFormatter.format(new Date(futureMs));
              const berlinHour = parseInt(hourString, 10);
              const is12OClock = berlinHour === 12;

              // Neue Regel: Preise dürfen nur um 12 Uhr (12:00 - 12:59) steigen.
              // Zu allen anderen Zeiten wird ein eventueller Anstieg unterbunden.
              if (!is12OClock && rawForecastValue > previousForecastValue) {
                forecastValue = previousForecastValue;
              }

              forecastData.push({
                timestampMs: futureMs,
                forecastValue: forecastValue
              });

              previousForecastValue = forecastValue;
            }
          }
        }

        const now = new Date().getTime();
        const minDisplayMs = now - durationHours * 60 * 60 * 1000;

        // Filter die Daten auf die vom Nutzer gewünschte Anzeigedauer
        let displayHistory: ChartDataPoint[] = fullHistory
          .filter(pt => pt.timestampMs >= minDisplayMs)
          .map(pt => ({ timestampMs: pt.timestampMs, value: pt.value }));

        // Funktion zum Aktualisieren der Chart-Daten (inklusive optionaler AI-Prognose)
        const updateChartData = (aiForecastData: ChartDataPoint[] = []) => {
            const pointMap = new Map<number, ChartDataPoint>();

            const addPoint = (ts: number, data: Partial<ChartDataPoint>) => {
               // Wir runden den Timestamp auf volle 5 Minuten, damit die Prognosen
               // den gleichen Timestamp auf der X-Achse haben und zusammen im Tooltip auftauchen
               const roundedTs = Math.round(ts / (5 * 60 * 1000)) * (5 * 60 * 1000);

               if (!pointMap.has(roundedTs)) {
                   pointMap.set(roundedTs, { timestampMs: roundedTs });
               }
               Object.assign(pointMap.get(roundedTs)!, data);
            };

            displayHistory.forEach(pt => addPoint(pt.timestampMs, { value: pt.value }));
            forecastData.forEach(pt => addPoint(pt.timestampMs, { forecastValue: pt.forecastValue }));
            aiForecastData.forEach(pt => addPoint(pt.timestampMs, { aiForecastValue: pt.aiForecastValue }));

            // Verbinde die aktuelle Linie mit den Prognoselinien
            if (displayHistory.length > 0) {
                const lastHistoryVal = displayHistory[displayHistory.length - 1].value;
                const lastHistoryTs = displayHistory[displayHistory.length - 1].timestampMs;

                if (forecastData.length > 0) {
                    addPoint(lastHistoryTs, { forecastValue: lastHistoryVal });
                }
                if (aiForecastData.length > 0) {
                    addPoint(lastHistoryTs, { aiForecastValue: lastHistoryVal });
                }
            }

            const finalChartData = Array.from(pointMap.values()).sort((a, b) => a.timestampMs - b.timestampMs);

            // Forward-Fill für Step-Charts:
            // Da die Holt-Winters-Prognose z.B. alle 10 Minuten einen Wert hat, die historische Prognose
            // aber alle 5 Minuten, füllen wir fehlende Werte mit dem zuletzt bekannten auf.
            // Das entspricht genau der visuellen Logik eines Step-Charts und garantiert,
            // dass das Tooltip auf jedem Punkt beide Werte anzeigt.
            let currentForecast: number | undefined = undefined;
            let currentAiForecast: number | undefined = undefined;

            let forecastStartTs = 0;
            if (displayHistory.length > 0) {
                forecastStartTs = Math.round(displayHistory[displayHistory.length - 1].timestampMs / (5 * 60 * 1000)) * (5 * 60 * 1000);
            }

            finalChartData.forEach(pt => {
                if (pt.timestampMs >= forecastStartTs) {
                    if (pt.forecastValue !== undefined) {
                        currentForecast = pt.forecastValue;
                    } else if (currentForecast !== undefined) {
                        pt.forecastValue = currentForecast;
                    }

                    if (pt.aiForecastValue !== undefined) {
                        currentAiForecast = pt.aiForecastValue;
                    } else if (currentAiForecast !== undefined) {
                        pt.aiForecastValue = currentAiForecast;
                    }
                }
            });

            return finalChartData;
        };

        const initialChartData = updateChartData([]);
        setChartData(initialChartData);

        if (initialChartData.length > 0) {
          // Calculate tick interval based on duration
          let tickIntervalMs;
          let totalHours = durationHours;
          if (selectedForecastOption === 'trend_12h_holt') {
              totalHours += 12;
          } else if (selectedForecastOption === '24h_holt') {
              totalHours += 24;
          } else if (selectedForecastOption === '48h_holt') {
              totalHours += 48;
          }

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

            // NEU: Wir holen uns den exakten Zeitstempel des ersten Datenpunktes
            const dataMinTs = initialChartData[0].timestampMs;
            const nowMs = new Date().getTime();

            let expectedForecastHours = 0;
            if (selectedForecastOption === 'trend_12h_holt') expectedForecastHours = 12;
            else if (selectedForecastOption === '24h_holt') expectedForecastHours = 24;
            else if (selectedForecastOption === '48h_holt') expectedForecastHours = 48;

            const maxTimestamp = nowMs + (expectedForecastHours * 60 * 60 * 1000);

            // Links: Das Diagramm startet exakt mit den Daten
            // Rechts: Das Diagramm endet mit dem Ende der Prognose
            setXDomain([dataMinTs, maxTimestamp]);

            const startDate = new Date(dataMinTs);
            startDate.setHours(0, 0, 0, 0);

            let currentTickTime = startDate.getTime();

            // WICHTIG: Wir spulen die Ticks vor, bis sie im sichtbaren Bereich liegen.
            // Das verhindert, dass eine Uhrzeit links neben der Y-Achse auftaucht!
            while (currentTickTime < dataMinTs) {
                currentTickTime += tickIntervalMs;
            }

            // Generiere alle Ticks bis zum Ende der Prognose
            const xAxisDomainUpperBound = maxTimestamp + 1000000;
            while (currentTickTime <= xAxisDomainUpperBound) {
                generatedGridTicks.push(currentTickTime);
                currentTickTime += tickIntervalMs;
            }

            setXTicks(generatedGridTicks);
        } else {
          setXTicks([]);
        }

        // Lade die AI-Prognose asynchron, ohne den initialen Render zu blockieren
        if (isHoltWintersForecastActive) {
            setLoadingAiForecast(true); // Set AI forecast loading to true
            let holtWintersForecastHours = 0;
            if (selectedForecastOption === 'trend_12h_holt') {
                holtWintersForecastHours = 12;
            } else if (selectedForecastOption === '24h_holt') {
                holtWintersForecastHours = 24;
            } else if (selectedForecastOption === '48h_holt') {
                holtWintersForecastHours = 48;
            }

            if (holtWintersForecastHours > 0) {
                fetch(`/api/homeassistant/fuel-prices/forecast?entityId=${entityId}&forecastHours=${holtWintersForecastHours}`)
                  .then(res => {
                      if (!res.ok) throw new Error("Failed to fetch AI forecast");
                      return res.json();
                  })
                  .then(aiData => {
                      if (!isMounted) return;
                      const newAiForecastData = aiData.map((item: any) => ({
                         timestampMs: new Date(item.timestamp).getTime(),
                         aiForecastValue: item.predictedPrice
                      }));

                      const updatedChartData = updateChartData(newAiForecastData);
                      setChartData(updatedChartData);
                  })
                  .catch(e => {
                      console.warn("Failed to fetch Holt-Winters forecast", e);
                  })
                  .finally(() => { // Set AI forecast loading to false
                      if (isMounted) setLoadingAiForecast(false);
                  });
            } else {
                if (isMounted) setLoadingAiForecast(false); // No AI forecast to fetch
            }
        } else {
            if (isMounted) setLoadingAiForecast(false); // No AI forecast active
        }

      } catch (err) {
        console.error(`Failed to fetch history for ${entityId}:`, err);
        if (isMounted) setError("Fehler beim Laden der Verlaufsdaten.");
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchHistory().catch(console.error);

    return () => {
      isMounted = false;
    };
  }, [entityId, durationHours, selectedForecastOption]);

  if (loading) {
    return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25 animate-pulse bg-slate-800/30 rounded-lg">Lade Verlauf...</div>;
  }

  if (error) {
    return <div className="text-red-400 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Fehler: {error}</div>;
  }

  if (chartData.length === 0) {
    return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Keine Verlaufsdaten verfügbar.</div>;
  }

  const isTrendForecastActive = selectedForecastOption === 'trend_12h_holt';
  const isHoltWintersForecastActive = selectedForecastOption === 'trend_12h_holt' || selectedForecastOption === '24h_holt' || selectedForecastOption === '48h_holt';

  return (
    <div className="relative w-full" style={{ height: height }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 5, right: 40, left: 10, bottom: 30 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
            <XAxis
                dataKey="timestampMs"
                type="number"
                scale="time"
                domain={xDomain}
                // allowDataOverflow={true}
                ticks={xTicks}
                tickFormatter={(timestampMs) => {
                    const date = new Date(timestampMs);
                    const timeString = date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });

                    let totalHours = durationHours;
                    if (selectedForecastOption === 'trend_12h_holt') {
                        totalHours += 12;
                    } else if (selectedForecastOption === '24h_holt') {
                        totalHours += 24;
                    } else if (selectedForecastOption === '48h_holt') {
                        totalHours += 48;
                    }

                    // Bei mehr als 24 Stunden Gesamtansicht: Immer Datum + Uhrzeit anzeigen
                    if (totalHours > 24 || timeString === '00:00') {
                        // Entferne die <= 48 Einschränkung, zeige einfach immer beides
                        return `${date.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })} ${timeString}`;
                    }
                    // Bei 24h Ansicht: Nur Uhrzeit
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
          {isTrendForecastActive && (
            <Line type="stepAfter" dataKey="forecastValue" stroke="#82ca9d" strokeWidth={2} strokeDasharray="5 5" dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
          )}
          {isHoltWintersForecastActive && (
            <Line type="stepAfter" dataKey="aiForecastValue" stroke="#f59e0b" strokeWidth={2} strokeDasharray="5 5" dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
          )}
          {loadingAiForecast && (
              <g>
                  {/* Positionierung: x = (Gesamtbreite - rechter_Margin - Spinner_Breite), y = oberer_Margin */}
                  <foreignObject x="calc(100% - 40px - 32px)" y="5" width="32" height="32">
                      <div className="p-2 bg-slate-800/50 rounded-full flex items-center justify-center">
                          <RefreshCw className="w-4 h-4 animate-spin text-blue-400" />
                      </div>
                  </foreignObject>
              </g>
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
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
  const [selectedForecastOption, setSelectedForecastOption] = useState<'none' | 'trend_12h_holt' | '24h_holt' | '48h_holt'>('trend_12h_holt'); // Neu: Toggle für Prognose
  const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds); // Countdown state
  const [showHelpSection, setShowHelpSection] = useState<boolean>(false); // State for help section visibility

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
    setSelectedForecastOption('trend_12h_holt'); // Default forecast option
    setShowHelpSection(false); // Collapse help section by default when opening modal
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
                  <FuelPriceChart entityId={fuelPrice.entityId} fuelType={fuelType} durationHours={24} selectedForecastOption='none' />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {/* Modal-Fenster */}
      {isModalOpen && modalFuelPrice && (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-start justify-center z-50 p-4 sm:p-6 pt-16 sm:pt-24" onClick={closeModal}>
          <div className="bg-slate-800 p-4 sm:p-6 rounded-xl border border-slate-700 shadow-lg w-full max-w-4xl relative flex flex-col max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <button onClick={closeModal} className="absolute top-2 right-2 sm:top-4 sm:right-4 text-slate-400 hover:text-white z-10 p-2 bg-slate-800/50 rounded-full">
              <X size={24} />
            </button>

            <div className="flex flex-col mb-4 sm:mb-6 pr-8">
              <h3 className="text-xl sm:text-2xl font-bold text-white mb-4">
                {modalFuelPrice.stationName} - {getFuelTypeName(modalFuelPrice.fuelType)}
              </h3>

              <div className="flex flex-col sm:flex-row items-start sm:items-start gap-4">
                {/* Prognose-Auswahl */}
                <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto">
                  {[
                    { label: 'Keine Prognose', value: 'none' },
                    { label: 'Trend + 12h HW', value: 'trend_12h_holt' }, // HW for Holt-Winters
                    { label: '24h HW', value: '24h_holt' },
                    { label: '48h HW', value: '48h_holt' }
                  ].map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setSelectedForecastOption(option.value as 'none' | 'trend_12h_holt' | '24h_holt' | '48h_holt')}
                      className={`flex-1 sm:flex-none px-3 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                        selectedForecastOption === option.value
                          ? 'bg-blue-600 text-white'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>

                {/* Zeitraum-Auswahl */}
                <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto">
                  {[
                    { label: '24h', value: 24 },
                    { label: '3 Tage', value: 72 },
                    { label: '7 Tage', value: 168 }
                  ].map((option) => (
                    <button
                      key={option.value}
                      onClick={() => setModalDurationHours(option.value)}
                      className={`flex-1 sm:flex-none px-3 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
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

              <div className="shrink-0 w-full">
                  <FuelPriceChart
                      entityId={modalFuelPrice.fuelPrice.entityId}
                      fuelType={modalFuelPrice.fuelType}
                      height={400}
                      durationHours={modalDurationHours}
                      selectedForecastOption={selectedForecastOption}
                  />
              </div>

            {/* Help Section */}
            <div className="mt-8 border-t border-slate-700 pt-4">
              <button
                type="button"
                onClick={() => setShowHelpSection(!showHelpSection)}
                className="flex items-center justify-between w-full text-slate-300 hover:text-white text-lg font-semibold py-2"
              >
                Hilfe & Erklärungen
                {showHelpSection ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
              </button>
              <div
                className={`overflow-hidden transition-all duration-300 ease-in-out ${
                  showHelpSection ? 'max-h-screen opacity-100' : 'max-h-0 opacity-0'
                }`}
              >
                <div className="text-sm text-slate-400 space-y-3 pb-4">
                  <p>
                    Dieses Diagramm zeigt den historischen Preisverlauf der ausgewählten Kraftstoffart.
                    Sie können verschiedene Prognosemodelle und Zeiträume auswählen:
                  </p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>
                      <strong>Keine Prognose:</strong> Zeigt nur den tatsächlichen Preisverlauf an.
                    </li>
                    <li>
                      <strong>Trend + 12h HW:</strong> Zeigt eine 12-Stunden-Prognose basierend auf einem gewichteten Durchschnitt der letzten 7 Tage (grüne Linie)
                      und zusätzlich eine 12-Stunden-Prognose mittels Holt-Winters-Modell (orange Linie).
                      Die Trend-Prognose berücksichtigt, dass Preise in der Regel nur um 12 Uhr mittags steigen.
                    </li>
                    <li>
                      <strong>24h HW:</strong> Zeigt eine 24-Stunden-Prognose mittels Holt-Winters-Modell (orange Linie).
                    </li>
                    <li>
                      <strong>48h HW:</strong> Zeigt eine 48-Stunden-Prognose mittels Holt-Winters-Modell (orange Linie).
                    </li>
                  </ul>
                  <p>
                    Die Holt-Winters-Prognose ist ein statistisches Modell, das saisonale Trends und Muster in den Daten erkennt,
                    um zukünftige Werte vorherzusagen.
                  </p>
                  <p>
                    Die Linien im Diagramm bedeuten:
                  </p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>
                      <span className="inline-block w-4 h-0.5 bg-[#8884d8] mr-2"></span>
                      Aktueller Preisverlauf
                    </li>
                    <li>
                      <span className="inline-block w-4 h-0.5 bg-[#82ca9d] mr-2"></span>
                      Prognose (Trend)
                    </li>
                    <li>
                      <span className="inline-block w-4 h-0.5 bg-[#f59e0b] mr-2"></span>
                      Prognose (Holt-Winters)
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <div className="mt-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-slate-300 border-t border-slate-700 pt-4">
              <div className="flex items-center space-x-2">
                <span className="text-sm text-slate-400">Aktueller Preis:</span>
                <span className="font-bold text-white text-lg whitespace-nowrap">{modalFuelPrice.fuelPrice.value.toFixed(3)} €</span>
              </div>
              <div className="flex items-center space-x-2 text-sm text-slate-500">
                <span className="text-slate-400">Letzte Änderung:</span>
                <span className="whitespace-nowrap">{new Date(modalFuelPrice.fuelPrice.lastChanged).toLocaleString('de-DE')}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FuelPriceDashboard;
