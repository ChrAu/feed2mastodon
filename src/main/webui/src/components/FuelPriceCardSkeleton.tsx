import { Clock, Fuel } from 'lucide-react';

const FuelPriceCardSkeleton = () => {
    return (
        <div className="bg-slate-900/50 p-4 rounded-lg border border-slate-700 animate-pulse">
            <div className="flex items-center mb-4">
                <div className="w-6 h-6 mr-2 text-blue-400">
                    <Fuel />
                </div>
                <div className="h-6 bg-slate-700 rounded w-3/4"></div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
                {[...Array(3)].map((_, i) => (
                    <div key={i} className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                        <div className="h-4 bg-slate-700 rounded w-1/2 mb-2"></div>
                        <div className="h-8 bg-slate-700 rounded w-3/4 mb-2"></div>
                        <div className="flex items-center text-slate-500 text-xs">
                            <Clock className="w-3 h-3 mr-1" />
                            <div className="h-3 bg-slate-700 rounded w-1/2"></div>
                        </div>
                        <div className="mt-4 h-24 bg-slate-700 rounded"></div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default FuelPriceCardSkeleton;
