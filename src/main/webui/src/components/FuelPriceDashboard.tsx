import React, { useState, useEffect } from 'react';
import { Clock, Fuel } from 'lucide-react';

// Angepasste Interfaces, um den Backend-DTOs zu entsprechen
interface FuelPrice {
  value: number;
  unit: string;
  lastChanged: string; // ZonedDateTime wird als String empfangen
}

interface FuelStation {
  name: string;
  fuelPrices: { [key: string]: FuelPrice }; // Map<String, FuelPriceDto>
  status: boolean;
}

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
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

export default FuelPriceDashboard;
