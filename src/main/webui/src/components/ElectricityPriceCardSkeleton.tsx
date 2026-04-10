import { Zap } from 'lucide-react';

const ElectricityPriceCardSkeleton = () => {
    return (
        <div className="bg-slate-900/50 p-4 rounded-lg border border-slate-700 animate-pulse">
            <div className="flex items-center mb-4">
                <div className="w-6 h-6 mr-2 text-blue-400">
                    <Zap />
                </div>
                <div className="h-6 bg-slate-700 rounded w-1/2"></div>
            </div>
            <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="h-4 bg-slate-700 rounded w-1/3 mb-2"></div>
                <div className="h-10 bg-slate-700 rounded w-2/3 mb-2"></div>
                <div className="h-3 bg-slate-700 rounded w-1/4"></div>
            </div>
        </div>
    );
};

export default ElectricityPriceCardSkeleton;
