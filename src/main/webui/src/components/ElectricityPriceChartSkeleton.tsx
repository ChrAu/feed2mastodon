import React from 'react';

const ElectricityPriceChartSkeleton: React.FC = () => {
    return (
        <div className="bg-slate-800 p-4 sm:p-6 rounded-xl border border-slate-700 shadow-lg w-full relative flex flex-col animate-pulse">
            {/* Header/Title Placeholder */}
            <div className="flex flex-col mb-4 sm:mb-6">
                <div className="h-7 bg-slate-700 rounded w-3/4 mb-4"></div>
                <div className="flex flex-col sm:flex-row items-start sm:items-center gap-2">
                    <div className="h-5 bg-slate-700 rounded w-20"></div>
                    <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto">
                        <div className="h-8 w-16 bg-slate-700 rounded-md"></div>
                        <div className="h-8 w-16 bg-slate-700 rounded-md"></div>
                        <div className="h-8 w-16 bg-blue-600 rounded-md"></div>
                        <div className="h-8 w-16 bg-slate-700 rounded-md"></div>
                    </div>
                </div>
            </div>

            {/* Chart Area Placeholder */}
            <div className="shrink-0 w-full relative bg-slate-900 rounded-lg" style={{ height: '400px' }}>
                <div className="absolute inset-0 flex items-center justify-center">
                    <div className="h-2/3 w-11/12 bg-slate-700 rounded-lg opacity-50"></div>
                </div>
            </div>

            {/* Footer/Price Info Placeholder */}
            <div className="mt-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-t border-slate-700 pt-4">
                <div className="flex items-center space-x-2">
                    <div className="h-5 bg-slate-700 rounded w-24"></div>
                    <div className="h-6 bg-slate-700 rounded w-32"></div>
                </div>
                <div className="flex items-center space-x-2">
                    <div className="h-5 bg-slate-700 rounded w-28"></div>
                    <div className="h-5 bg-slate-700 rounded w-40"></div>
                </div>
            </div>
        </div>
    );
};

export default ElectricityPriceChartSkeleton;
