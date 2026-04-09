import React from 'react';
import { Clock, Fuel, TrendingUp, TrendingDown } from 'lucide-react';
import { FuelPriceChart, VisibleLinesState } from './FuelPriceChart'; // Assuming FuelPriceChart is in the same directory

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

interface FuelStationCardProps {
    station: FuelStation;
    openModal: (fuelPrice: FuelPrice, fuelType: string, stationName: string) => void;
}

const getFuelTypeName = (fuelType: string) => {
    switch (fuelType) {
        case 'diesel': return 'Diesel';
        case 'super': return 'Super';
        case 'superE10': return 'Super E10';
        default: return fuelType;
    }
};

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
    if (Math.abs(diff) < 0.001) return null;

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

const FuelStationCard: React.FC<FuelStationCardProps> = ({ station, openModal }) => {
    // Default visibleLines for the small chart in the card
    const defaultVisibleLines: VisibleLinesState = {
        value: true,
        forecastValue: false,
        aiForecastValue: false,
        savedForecastValue: false
    };

    return (
        <div className="bg-slate-800/50 p-6 rounded-xl border border-slate-700 shadow-lg mt-8">
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
                        <p className="text-slate-300 text-sm mb-1">{getFuelTypeName(fuelType)}</p>
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
                        <div className="mt-4">
                            <FuelPriceChart
                                entityId={fuelPrice.entityId}
                                fuelType={fuelType}
                                durationHours={24}
                                selectedForecastOption='none'
                                isCheckMode={false}
                                visibleLines={defaultVisibleLines}
                            />
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default FuelStationCard;
