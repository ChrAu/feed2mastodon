import {Clock, Fuel, X} from 'lucide-react'; // X-Icon für den Schließen-Button
import React, {useEffect, useState} from 'react';
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';

// Angepasste Interfaces, um den Backend-DTOs zu entsprechen
interface FuelPrice {
  value: number;
  unit: string;
  lastChanged: string; // ZonedDateTime wird als String empfangen
  entityId: string; // Neu hinzugefügt
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

interface FuelPriceChartProps {
  entityId: string;
  fuelType: string;
  height?: number; // Optional height prop for chart
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
      <div className="bg-slate-800 p-3 rounded-md border border-slate-700 text-white text-sm">
        <p className="font-bold mb-1">{formattedDate}</p>
        <p>{`${fuelTypeName}: ${payload[0].value.toFixed(3)} €`}</p>
      </div>
    );
  }
  return null;
};


const FuelPriceChart: React.FC<FuelPriceChartProps> = ({ entityId, fuelType, height = 200 }) => {
  const [history, setHistory] = useState<FuelPriceHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [xTicks, setXTicks] = useState<number[]>([]); // All 4-hour interval ticks for grid

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(`/api/homeassistant/fuel-prices/history?entityId=${entityId}&durationHours=24`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data: FuelPriceHistory[] = await response.json();
        const formattedData = data.map(item => ({
          ...item,
          timestampMs: new Date(item.timestamp).getTime()
        }));
        setHistory(formattedData);

        if (formattedData.length > 0) {
          const firstDataTimestamp = formattedData[0].timestampMs;
          const lastDataTimestamp = formattedData[formattedData.length - 1].timestampMs;

          const fourHoursInMs = 4 * 60 * 60 * 1000;
          const generatedGridTicks: number[] = [];

          const minTimestamp = formattedData[0].timestampMs;
          const maxTimestamp = formattedData[formattedData.length - 1].timestampMs;

          const startDate = new Date(minTimestamp);
          startDate.setHours(0, 0, 0, 0);

          let currentTickTime = startDate.getTime();

          while (currentTickTime < minTimestamp) {
            currentTickTime += fourHoursInMs;
          }

          while (currentTickTime <= maxTimestamp + fourHoursInMs) {
            generatedGridTicks.push(currentTickTime);
            currentTickTime += fourHoursInMs;
          }
          setXTicks(generatedGridTicks);

          // Determine specific ticks for labels
          const newLabelTicks: number[] = [];
          newLabelTicks.push(firstDataTimestamp); // First data point's timestamp

          // Add intermediate ticks from generatedGridTicks
          const intermediateTicks = generatedGridTicks.filter(tick =>
            tick > firstDataTimestamp && tick < lastDataTimestamp
          );

          if (intermediateTicks.length > 0) {
            // Try to pick two well-spaced intermediate ticks
            if (intermediateTicks.length >= 2) {
              newLabelTicks.push(intermediateTicks[Math.floor(intermediateTicks.length / 3)]);
              newLabelTicks.push(intermediateTicks[Math.floor(2 * intermediateTicks.length / 3)]);
            } else {
              newLabelTicks.push(intermediateTicks[0]); // If only one intermediate, add it
            }
          }

          // Only add lastDataTimestamp if it's not too close to the last intermediate tick
          // or if there are no intermediate ticks
          const lastLabelTime = newLabelTicks[newLabelTicks.length - 1];
          if (Math.abs(lastDataTimestamp - lastLabelTime) > (2 * 60 * 60 * 1000) || newLabelTicks.length < 3) { // 2 hours buffer
            newLabelTicks.push(lastDataTimestamp); // Last data point's timestamp
          }

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

    fetchHistory();
  }, [entityId]);

  if (loading) {
    return <div className="text-slate-500 text-center text-sm py-2">Lade Verlauf...</div>;
  }

  if (error) {
    return <div className="text-red-400 text-center text-sm py-2">Fehler: {error}</div>;
  }

  if (history.length === 0) {
    return <div className="text-slate-500 text-center text-sm py-2">Keine Verlaufsdaten verfügbar.</div>;
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={history} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
        <XAxis
          dataKey="timestampMs" // Verwende den numerischen Zeitstempel
          type="number" // Wichtig für Zeitskalierung
          scale="time" // Wichtig für Zeitskalierung
          domain={['dataMin', 'dataMax']} // Stellt sicher, dass die Achse den gesamten Datenbereich abdeckt
          ticks={xTicks} // Alle 4-Stunden-Ticks für das Raster
          tickFormatter={(timestampMs, index) => {

              if(index == 1  || index == 3 ||  index == 5 || index == 6){
                  return '';
              }
            const date = new Date(timestampMs);
              return date.toLocaleTimeString('de-DE', {hour: '2-digit', minute: '2-digit'});
          }}
          stroke="#94a3b8"
          interval={0} // Erzwingt die Anzeige aller Ticks (für das Raster)
          angle={-45} // Dreht die Beschriftungen um 45 Grad
          textAnchor="end" // Richtet den Text am Ende aus
          height={60} // Gibt mehr Höhe für die gedrehten Beschriftungen
        />
        <YAxis stroke="#94a3b8" domain={['dataMin - 0.01', 'dataMax + 0.01']} tickFormatter={(value) => value.toFixed(2)} />
        <Tooltip
          content={<CustomTooltip fuelTypeName={getFuelTypeName(fuelType)} />}
        />
        <Line type="stepAfter" dataKey="value" stroke="#8884d8" strokeWidth={2} dot={false} fill="none" />
      </LineChart>
    </ResponsiveContainer>
  );
};


const FuelPriceDashboard: React.FC = () => {
  const [fuelStations, setFuelStations] = useState<FuelStation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalFuelPrice, setModalFuelPrice] = useState<{ fuelPrice: FuelPrice; fuelType: string; stationName: string } | null>(null);

  useEffect(() => {
    const fetchFuelPrices = async () => {
      setLoading(true);
      setError(null);
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

      } catch (err) {
        console.error("Failed to fetch fuel prices:", err);
        setError("Fehler beim Laden der Tankstellendaten.");
      } finally {
        setLoading(false);
      }
    };

    fetchFuelPrices();
    // Refresh data every 5 minutes
    const interval = setInterval(fetchFuelPrices, 5 * 60 * 1000);
    return () => clearInterval(interval);
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
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setModalFuelPrice(null);
  };

  if (loading) {
    return <div className="text-slate-400 text-center py-4">Lade Tankstellendaten...</div>;
  }

  if (error) {
    return <div className="text-red-400 text-center py-4">Fehler: {error}</div>;
  }

  return (
    <div className="space-y-8">
      {fuelStations.map((station, index) => (
        <div key={index} className="bg-slate-800/50 p-6 rounded-xl border border-slate-700 shadow-lg">
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
                className="bg-slate-900/50 p-4 rounded-lg border border-slate-700 cursor-pointer hover:bg-slate-800/70 transition-colors duration-200"
                onClick={() => openModal(fuelPrice, fuelType, station.name)}
              >
                <p className="text-slate-300 text-sm mb-1">
                  {getFuelTypeName(fuelType)}
                </p>
                <p className="text-2xl font-bold text-white flex items-baseline">
                  {fuelPrice.value.toFixed(3)}<span className="ml-1">€</span>
                </p>
                <p className="text-slate-500 text-xs mt-2 flex items-center">
                  <Clock className="w-3 h-3 mr-1" />
                  Vor {formatTimeAgo(fuelPrice.lastChanged)}
                </p>
                {/* Das Diagramm in den Kacheln verwendet jetzt wieder die Standardhöhe (200px) */}
                <div className="mt-4">
                  <FuelPriceChart entityId={fuelPrice.entityId} fuelType={fuelType} />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {/* Modal-Fenster */}
      {isModalOpen && modalFuelPrice && (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4" onClick={closeModal}>
          <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-lg w-full max-w-3xl relative" onClick={e => e.stopPropagation()}>
            <button onClick={closeModal} className="absolute top-4 right-4 text-slate-400 hover:text-white">
              <X size={24} />
            </button>
            <h3 className="2xl font-bold text-white mb-4">
              {modalFuelPrice.stationName} - {getFuelTypeName(modalFuelPrice.fuelType)} Preisverlauf
            </h3>
            <div className="h-[400px]"> {/* Größere Höhe für das Diagramm im Modal */}
              <FuelPriceChart entityId={modalFuelPrice.fuelPrice.entityId} fuelType={modalFuelPrice.fuelType} height={400} />
            </div>
            <div className="mt-4 text-slate-300">
              Aktueller Preis: <span className="font-bold">{modalFuelPrice.fuelPrice.value.toFixed(3)} €</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FuelPriceDashboard;
