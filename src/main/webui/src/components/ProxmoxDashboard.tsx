import React, { useEffect, useState } from 'react';
import { ServerMetrics } from '../data/proxmox';

const ProxmoxDashboard: React.FC = () => {
    const [metrics, setMetrics] = useState<ServerMetrics | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const eventSource = new EventSource('/api/traffic-stream');
        eventSource.onmessage = (event) => {
            setMetrics(JSON.parse(event.data));
            setError(null);
        };
        eventSource.onerror = () => {
            setError("Verbindung verloren...");
            eventSource.close();
        };
        return () => eventSource.close();
    }, []);

    if (error) return <div className="text-red-400 p-4">{error}</div>;
    if (!metrics) return <div className="text-gray-400 p-4 animate-pulse">Lade Metriken...</div>;

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 bg-transparent h-full pt-4">

            {/* CPU Karte - Dunkel mit Akzent */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-inner rounded-xl p-4 border border-slate-700 flex flex-col justify-between min-h-[140px]">
                <div className="text-xs text-orange-400 uppercase font-black tracking-wider">CPU Load</div>
                <div className="mt-auto">
                    <div className="text-2xl lg:text-3xl font-mono font-bold text-white mt-1">
                        {metrics.cpuUsage.toFixed(1)}<span className="text-sm lg:text-lg text-slate-400">%</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4">
                        <div className="bg-orange-500 h-1.5 rounded-full transition-all duration-500"
                             style={{ width: `${metrics.cpuUsage}%` }}></div>
                    </div>
                </div>
            </div>

            {/* RAM Karte */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-inner rounded-xl p-4 border border-slate-700 flex flex-col justify-between min-h-[140px]">
                <div className="text-xs text-yellow-400 uppercase font-black tracking-wider">Memory</div>
                <div className="mt-auto">
                    <div className="text-2xl lg:text-3xl font-mono font-bold text-white mt-1">
                        {metrics.memUsage.toFixed(1)}<span className="text-sm lg:text-lg text-slate-400">%</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4">
                        <div className="bg-yellow-500 h-1.5 rounded-full transition-all duration-500"
                             style={{ width: `${metrics.memUsage}%` }}></div>
                    </div>
                </div>
            </div>

            {/* Download */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-inner rounded-xl p-4 border border-slate-700 flex flex-col justify-between min-h-[140px]">
                <div className="text-xs text-cyan-400 uppercase font-black tracking-wider">Net In</div>
                <div className="mt-auto">
                    <div className="text-2xl lg:text-3xl font-mono font-bold text-white mt-1">
                        {metrics.netInMBs.toFixed(2)}<span className="text-sm lg:text-lg text-slate-400 ml-1">MB/s</span>
                    </div>
                    <div className="text-[10px] lg:text-xs text-slate-500 mt-2 font-mono h-1.5 leading-none">Realtime Traffic</div>
                </div>
            </div>

            {/* Upload */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-inner rounded-xl p-4 border border-slate-700 flex flex-col justify-between min-h-[140px]">
                <div className="text-xs text-pink-400 uppercase font-black tracking-wider">Net Out</div>
                <div className="mt-auto">
                    <div className="text-2xl lg:text-3xl font-mono font-bold text-white mt-1">
                        {metrics.netOutMBs.toFixed(2)}<span className="text-sm lg:text-lg text-slate-400 ml-1">MB/s</span>
                    </div>
                    <div className="text-[10px] lg:text-xs text-slate-500 mt-2 font-mono h-1.5 leading-none">Realtime Traffic</div>
                </div>
            </div>

        </div>
    );
};

export default ProxmoxDashboard;
