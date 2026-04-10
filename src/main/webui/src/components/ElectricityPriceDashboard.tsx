import { RefreshCw } from 'lucide-react';
import React, { useEffect, useState, useCallback } from 'react';
import ElectricityPriceCardSkeleton from './ElectricityPriceCardSkeleton';
import { ElectricityPriceChart, VisibleLinesState } from './ElectricityPriceChart'; // Removed ElectricityPriceForecastDto

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

// Removed SavedForecastDto interface as it's no longer used.

const HISTORY_OPTIONS: { label: string; value: number }[] = [
    { label: '24h', value: 24 },
    { label: '3 Tage', value: 72 },
    { label: '7 Tage', value: 168 },
    { label: '30 Tage', value: 720 }
];

const ElectricityPriceDashboard: React.FC = () => {
    const updateIntervalSeconds = 5 * 60; // 5 Minuten
    const [apiResponseData, setApiResponseData] = useState<ApiResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [nextUpdate, setNextUpdate] = useState<number>(updateIntervalSeconds);

    const [modalDurationHours, setModalDurationHours] = useState<number>(24);
    const [visibleLines, setVisibleLines] = useState<VisibleLinesState>({
        value: true,
        aiForecastValue: true,
        // Removed savedForecastValue
    });
    const [isCheckMode, setIsCheckMode] = useState<boolean>(false);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        setIsCheckMode(params.get('check') === 'true');
    }, []);

    useEffect(() => {
        const fetchElectricityPrice = async () => {
            try {
                const response = await fetch('/api/homeassistant/electricity-price');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const data: ApiResponse = await response.json();
                setApiResponseData(data);
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

    // Removed useEffect for fetching saved forecasts
    useCallback((lineKey: keyof VisibleLinesState) => {
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

    const totalPrice = apiResponseData?.prices.total_price;

    return (
        <div className="space-y-8 relative">
            <div className="flex justify-end mb-4 mt-8">
                <div className="flex items-center text-sm text-slate-400 bg-slate-800/80 px-3 py-1.5 rounded-full border border-slate-700 shadow-sm backdrop-blur-sm z-10">
                    <RefreshCw className={`w-4 h-4 mr-2 ${nextUpdate < 5 ? 'animate-spin text-blue-400' : ''}`} />
                    <span>Nächstes Update in {formatCountdown(nextUpdate)}</span>
                </div>
            </div>

            {apiResponseData && totalPrice && (
                <>

                    <div className="bg-slate-800 p-4 sm:p-6 rounded-xl border border-slate-700 shadow-lg w-full relative flex flex-col">
                        <div className="flex flex-col mb-4 sm:mb-6">
                            <h3 id="modal-title" className="text-xl sm:text-2xl font-bold text-white mb-4">
                                {apiResponseData.friendlyName}
                                {apiResponseData.provider && (
                                    <span className="ml-3 px-2 py-1 text-xs font-semibold rounded-full bg-blue-500/20 text-blue-400">
                                        {apiResponseData.provider}
                                    </span>
                                )}
                                {apiResponseData.region && (
                                    <span className="ml-3 px-2 py-1 text-xs font-semibold rounded-full bg-purple-500/20 text-purple-400">
                                        {apiResponseData.region}
                                    </span>
                                )}
                            </h3>

                            <div className="flex flex-col gap-4 w-full">
                                {/* Data Range Options Row */}
                                <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2">
                                    <span className="text-slate-400 text-sm font-medium min-w-30">Historie:</span>
                                    <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto">
                                        {HISTORY_OPTIONS.map((option) => (
                                            <button
                                                key={option.value}
                                                onClick={() => setModalDurationHours(option.value)}
                                                className={`flex-1 sm:flex-none px-3 py-1.5 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                                                    modalDurationHours === option.value
                                                        ? 'bg-blue-600 text-white'
                                                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                                                }`}
                                            >
                                                {option.label}
                                            </button>
                                        ))}
                                    </div>
                                    {/* Removed Backtest dropdown and related logic */}
                                </div>
                            </div>
                        </div>

                        <div className="shrink-0 w-full relative">
                            <ElectricityPriceChart
                                entityId={apiResponseData.entityId}
                                height={400}
                                durationHours={modalDurationHours}
                                // selectedForecastOption is hardcoded inside ElectricityPriceChart
                                // Removed savedForecastData prop
                                isCheckMode={isCheckMode}
                                visibleLines={visibleLines}
                            />
                        </div>


                        <div className="mt-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-slate-300 border-t border-slate-700 pt-4">
                            <div className="flex items-center space-x-2">
                                <span className="text-sm text-slate-400">Aktueller Preis:</span>
                                <span className="font-bold text-white text-lg whitespace-nowrap">{totalPrice.value.toFixed(3)} {totalPrice.unit || '€/kWh'}</span>
                            </div>
                            <div className="flex items-center space-x-2 text-sm text-slate-500">
                                <span className="text-slate-400">Letzte Änderung:</span>
                                <span className="whitespace-nowrap">{new Date(totalPrice.lastChanged).toLocaleString('de-DE')}</span>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ElectricityPriceDashboard;
