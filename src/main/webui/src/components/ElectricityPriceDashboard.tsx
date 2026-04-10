import { RefreshCw } from 'lucide-react';
import React, { useEffect, useState } from 'react';
import ElectricityPriceCard from './ElectricityPriceCard'; // Wird noch erstellt
import ElectricityPriceDetailModal from './ElectricityPriceDetailModal'; // Wird noch erstellt
import ElectricityPriceCardSkeleton from './ElectricityPriceCardSkeleton'; // Wird noch erstellt

interface PriceDetail {
    entityId: string;
    friendlyName: string;
    value: number;
    unit: string | null;
    lastChanged: string;
    previousValue?: number | null;
    currency?: string | null;
    provider?: string | null;
    region?: string | null;
}

interface Prices {
    total_price: PriceDetail;
    quantile: PriceDetail;
    average_price: PriceDetail;
    lowest_price: PriceDetail;
    highest_price: PriceDetail;
    market_price: PriceDetail;
    rank: PriceDetail;
    median_price: PriceDetail;
}

interface ApiResponse {
    entityId: string;
    friendlyName: string;
    unit: string | null;
    lastChanged: string;
    currency: string | null;
    provider: string | null;
    region: string | null;
    prices: Prices;
}

const ElectricityPriceDashboard: React.FC = () => {
    const updateIntervalSeconds = 5 * 60; // 5 Minuten
    const [apiResponseData, setApiResponseData] = useState<ApiResponse | null>(null); // Store the full API response
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds);

    useEffect(() => {
        const fetchElectricityPrice = async () => {
            try {
                const response = await fetch('/api/homeassistant/electricity-price'); // Neue API-Route
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const data: ApiResponse = await response.json();
                setApiResponseData(data); // Set the full API response
                setNextUpdate(updateIntervalSeconds);
                setError(null);
            } catch (err) {
                console.error("Failed to fetch electricity price:", err);
                setError("Fehler beim Laden der Strompreisdaten.");
            } finally {
                setLoading(false);
            }
        };

        fetchElectricityPrice().catch(console.error);
        const interval = setInterval(fetchElectricityPrice, updateIntervalSeconds * 1000);
        const countdownInterval = setInterval(() => setNextUpdate(prev => (prev > 0 ? prev - 1 : updateIntervalSeconds)), 1000);

        return () => {
            clearInterval(interval);
            clearInterval(countdownInterval);
        };
    }, []);

    const openModal = () => {
        setIsModalOpen(true);
    };

    const closeModal = () => {
        setIsModalOpen(false);
    };

    if (loading && !apiResponseData) {
        return (
            <div className="space-y-8 relative mt-8">
                <div className="flex justify-end mb-4">
                    <div className="h-8 bg-slate-800/80 rounded-full w-48 animate-pulse border border-slate-700"></div>
                </div>
                <ElectricityPriceCardSkeleton />
            </div>
        );
    }

    if (error && !apiResponseData) {
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

            {apiResponseData && apiResponseData.prices.total_price && (
                <ElectricityPriceCard electricityPrice={apiResponseData.prices.total_price} openModal={openModal} />
            )}

            {isModalOpen && apiResponseData && (
                <ElectricityPriceDetailModal
                    isOpen={isModalOpen}
                    onClose={closeModal}
                    apiResponse={apiResponseData} // Pass the full API response
                />
            )}
        </div>
    );
};

export default ElectricityPriceDashboard;
