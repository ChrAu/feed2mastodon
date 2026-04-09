import { RefreshCw } from 'lucide-react';
import React, { useEffect, useState } from 'react';
import FuelPriceCardSkeleton from './FuelPriceCardSkeleton';
import FuelStationCard from './FuelStationCard';
import FuelPriceDetailModal from './FuelPriceDetailModal';

interface FuelPrice {
    value: number;
    unit: string;
    lastChanged: string;
    entityId: string;
    previousValue?: number | null;
}

interface FuelStation {
    name: string;
    fuelPrices: { [key: string]: FuelPrice };
    status: boolean;
}

const FuelPriceDashboard: React.FC = () => {
    const updateIntervalSeconds = 5 * 60;
    const [fuelStations, setFuelStations] = useState<FuelStation[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalFuelPrice, setModalFuelPrice] = useState<{ fuelPrice: FuelPrice; fuelType: string; stationName: string } | null>(null);
    const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds);
    const [isCheckMode, setIsCheckMode] = useState<boolean>(false);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        setIsCheckMode(params.get('check') === 'true');

        const fetchFuelPrices = async () => {
            try {
                const response = await fetch('/api/homeassistant/fuel-prices');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const data: FuelStation[] = await response.json();

                const sortedData = [...data].sort((a, b) => {
                    if (a.name === "ARAL Gosbach") return -1;
                    if (b.name === "ARAL Gosbach") return 1;
                    if (a.name === "TotalEnergies Deggingen") return b.name === "ARAL Gosbach" ? 1 : -1;
                    if (b.name === "TotalEnergies Deggingen") return a.name === "ARAL Gosbach" ? -1 : 1;
                    return 0;
                });

                setFuelStations(sortedData);
                setNextUpdate(updateIntervalSeconds);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch fuel prices:", err);
                setError("Fehler beim Laden der Tankstellendaten.");
            } finally {
                setLoading(false);
            }
        };

        fetchFuelPrices().catch(console.error);
        const interval = setInterval(fetchFuelPrices, updateIntervalSeconds * 1000);
        const countdownInterval = setInterval(() => setNextUpdate(prev => (prev > 0 ? prev - 1 : updateIntervalSeconds)), 1000);

        return () => {
            clearInterval(interval);
            clearInterval(countdownInterval);
        };
    }, []);

    const openModal = (fuelPrice: FuelPrice, fuelType: string, stationName: string) => {
        setModalFuelPrice({ fuelPrice, fuelType, stationName });
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
                {[...Array(2)].map((_, i) => <FuelPriceCardSkeleton key={i} />)}
            </div>
        );
    }

    if (error && fuelStations.length === 0) {
        return <div className="text-red-400 text-center py-4 bg-red-900/20 border border-red-500/50 rounded-xl p-4 mt-8">Fehler: {error}</div>;
    }

    const formatCountdown = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        if (mins > 0) return `${mins}m ${secs.toString().padStart(2, '0')}s`;
        return `${secs}s`;
    };

    return (
        <div className="space-y-8 relative">
            <div className="flex justify-end mb-4 mt-8">
                <div className="flex items-center text-sm text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-full border border-slate-700 shadow-sm backdrop-blur-sm z-10">
                    <RefreshCw className={`w-4 h-4 mr-2 ${nextUpdate < 5 ? 'animate-spin text-blue-400' : ''}`} />
                    <span>Nächstes Update in {formatCountdown(nextUpdate)}</span>
                </div>
            </div>

            {fuelStations.map((station, index) => (
                <FuelStationCard key={index} station={station} openModal={openModal} />
            ))}

            {isModalOpen && modalFuelPrice && (
                <FuelPriceDetailModal
                    isOpen={isModalOpen}
                    onClose={closeModal}
                    fuelPrice={modalFuelPrice.fuelPrice}
                    fuelType={modalFuelPrice.fuelType}
                    stationName={modalFuelPrice.stationName}
                    isCheckMode={isCheckMode}
                />
            )}
        </div>
    );
};

export default FuelPriceDashboard;
