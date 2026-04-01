import React, { useState, useEffect } from 'react';
import { Clock, Fuel } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

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
}

interface FuelPriceChartProps {
  entityId: string;
  fuelType: string;
}

const FuelPriceChart: React.FC<FuelPriceChartProps> = ({ entityId}) => {
  const [history, setHistory] = useState<FuelPriceHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
        // Daten für Recharts aufbereiten: Zeitstempel formatieren
        const formattedData = data.map(item => ({
          ...item,
          timestamp: new Date(item.timestamp).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
        }));
        setHistory(formattedData);
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
    <ResponsiveContainer width="100%" height={200}>
      <LineChart data={history} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
        <XAxis dataKey="timestamp" stroke="#94a3b8" />
        <YAxis stroke="#94a3b8" domain={['dataMin - 0.01', 'dataMax + 0.01']} tickFormatter={(value) => value.toFixed(2)} />
        <Tooltip
          contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #475569', color: '#e2e8f0' }}
          labelStyle={{ color: '#94a3b8' }}
        />
        <Line type="monotone" dataKey="value" stroke="#8884d8" dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
};


const FuelPriceDashboard: React.FC = () => {
  const [fuelStations, setFuelStations] = useState<FuelStation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
        setFuelStations(data);

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
                {station.status ? 'Online' : 'Offline'}
              </span>
            )}
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            {Object.entries(station.fuelPrices).map(([fuelType, fuelPrice]) => (
              <div key={fuelType} className="bg-slate-900/50 p-4 rounded-lg border border-slate-700">
                <p className="text-slate-300 text-sm mb-1">
                  {fuelType === 'diesel' && 'Diesel'}
                  {fuelType === 'super' && 'Super'}
                  {fuelType === 'superE10' && 'Super E10'}
                </p>
                <p className="text-2xl font-bold text-white flex items-baseline">
                  {fuelPrice.value.toFixed(3)}<span className="text-base ml-1">{fuelPrice.unit}</span>
                </p>
                <p className="text-slate-500 text-xs mt-2 flex items-center">
                  <Clock className="w-3 h-3 mr-1" />
                  Vor {formatTimeAgo(fuelPrice.lastChanged)}
                </p>
                {/* Hier wird das Diagramm hinzugefügt */}
                <div className="mt-4">
                  <FuelPriceChart entityId={fuelPrice.entityId} fuelType={fuelType} />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

export default FuelPriceDashboard;
