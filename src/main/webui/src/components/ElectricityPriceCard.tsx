import React from 'react';
import { Clock, Zap, TrendingUp, TrendingDown } from 'lucide-react';

// Interface for individual price details (corresponds to backend ElectricityPriceDto)
interface PriceDetail {
    value: number;
    unit: string | null;
    lastChanged: string; // LocalDateTime from backend will be string
    entityId: string;
    previousValue?: number | null;
    currency?: string | null;
    provider?: string | null;
    region?: string | null;
    friendlyName: string; // Added friendlyName as it's part of PriceDetail now
}

interface ElectricityPriceCardProps {
    electricityPrice: PriceDetail; // Changed to accept a single PriceDetail
    openModal: () => void; // Changed to not require an entityId
}

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

const renderPriceDifference = (currentValue: number, previousValue?: number | null) => {
    if (previousValue === undefined || previousValue === null) return null;
    const diff = currentValue - previousValue;
    if (Math.abs(diff) < 0.0001) return null; // Kleinere Toleranz für Strompreise

    const diffInCents = Math.round(diff * 10000) / 100; // Differenz in Cent/kWh
    const isIncrease = diff > 0;
    const colorClass = isIncrease ? 'text-red-400' : 'text-green-400';
    const Icon = isIncrease ? TrendingUp : TrendingDown;
    const sign = isIncrease ? '+' : '';

    return (
        <div className={`flex items-center text-xs mt-1 ${colorClass}`}>
            <Icon className="w-3 h-3 mr-1" />
            <span>{sign}{diffInCents.toFixed(2)} Cent/kWh</span>
        </div>
    );
};

const ElectricityPriceCard: React.FC<ElectricityPriceCardProps> = ({ electricityPrice, openModal }) => {
    if (!electricityPrice) {
        return <div className="text-red-400 text-center py-4 bg-red-900/20 border border-red-500/50 rounded-xl p-4 mt-8">Fehler: Strompreisdaten unvollständig oder fehlerhaft.</div>;
    }

    // Use electricityPrice directly as it's now a single PriceDetail object
    const { value, unit, lastChanged, friendlyName, previousValue, provider, region } = electricityPrice;

    return (
        <div className="bg-slate-800/50 p-6 rounded-xl border border-slate-700 shadow-lg mt-8">
            <h4 className="text-xl font-bold text-white mb-4 flex items-center">
                <Zap className="w-6 h-6 mr-2 text-blue-400" />
                {friendlyName} {/* Display friendlyName from the PriceDetail */}
                {provider && (
                    <span className="ml-3 px-2 py-1 text-xs font-semibold rounded-full bg-blue-500/20 text-blue-400">
                        {provider}
                    </span>
                )}
                {region && (
                    <span className="ml-3 px-2 py-1 text-xs font-semibold rounded-full bg-purple-500/20 text-purple-400">
                        {region}
                    </span>
                )}
            </h4>
            <div
                className="bg-slate-900/50 p-4 rounded-lg border border-slate-700 cursor-pointer hover:bg-slate-800/70 transition-colors duration-200 relative z-0"
                onClick={openModal} // Open modal for the main price
            >
                <p className="text-slate-300 text-sm mb-1">Aktueller Preis</p>
                <div className="flex flex-col">
                    <p className="text-2xl font-bold text-white flex items-baseline">
                        {value.toFixed(3)}<span className="ml-1">{unit || '€/kWh'}</span>
                    </p>
                    {renderPriceDifference(value, previousValue)}
                </div>
                <p className="text-slate-500 text-xs mt-2 flex items-center">
                    <Clock className="w-3 h-3 mr-1" />
                    Vor {formatTimeAgo(lastChanged)}
                </p>
            </div>
        </div>
    );
};

export default ElectricityPriceCard;
