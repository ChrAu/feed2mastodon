import React, { useEffect, useState, useRef } from 'react';
import { ServerMetrics } from '../data/proxmox';

const ProxmoxDashboard: React.FC = () => {
    const [metrics, setMetrics] = useState<ServerMetrics | null>(null);
    const [error, setError] = useState<string | null>(null);
    const eventSourceRef = useRef<EventSource | null>(null);
    const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    const reconnectInterval = 5000; // 5 seconds

    const connect = () => {
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        const eventSource = new EventSource('/api/traffic-stream');
        eventSourceRef.current = eventSource;

        eventSource.onmessage = (event) => {
            setMetrics(JSON.parse(event.data));
            setError(null); // Clear error on successful message
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
                reconnectTimeoutRef.current = null;
            }
        };

        eventSource.onerror = (err) => {
            console.error("EventSource failed:", err);
            eventSource.close(); // Close current connection
            setError("Verbindung verloren. Versuche erneut zu verbinden...");

            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            reconnectTimeoutRef.current = setTimeout(() => {
                connect();
            }, reconnectInterval);
        };
    };

    useEffect(() => {
        connect();

        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
        };
    }, []);

    if (error) return <div className="text-red-400 p-4">{error}</div>;
    if (!metrics) return <div className="text-gray-400 p-4 animate-pulse">Lade Metriken...</div>;

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 p-4 bg-transparent">

            {/* CPU Karte - Dunkel mit Akzent */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700">
                <div className="text-xs text-orange-400 uppercase font-black tracking-wider">CPU Load</div>
                <div className="h-4"></div> {/* Unsichtbarer Platzhalter für zweite Zeile */}
                <div className="text-2xl font-mono font-bold text-white mt-1">
                    {metrics.cpuUsage.toFixed(1)}<span className="text-lg text-slate-400">%</span>
                </div>
                <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4">
                    <div className="bg-orange-500 h-1.5 rounded-full transition-all duration-500"
                         style={{ width: `${metrics.cpuUsage}%` }}></div>
                </div>
            </div>

            {/* RAM Karte */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700">
                <div className="text-xs text-yellow-400 uppercase font-black tracking-wider">Memory</div>
                <div className="h-4"></div> {/* Unsichtbarer Platzhalter für zweite Zeile */}
                <div className="text-2xl font-mono font-bold text-white mt-1">
                    {metrics.memUsage.toFixed(1)}<span className="text-lg text-slate-400">%</span>
                </div>
                <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4">
                    <div className="bg-yellow-500 h-1.5 rounded-full transition-all duration-500"
                         style={{ width: `${metrics.memUsage}%` }}></div>
                </div>
            </div>

            {/* Download */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700">
                <div className="text-xs text-cyan-400 uppercase font-black tracking-wider">Net In</div>
                <div className="h-4"></div> {/* Unsichtbarer Platzhalter für zweite Zeile */}
                <div className="text-2xl font-mono font-bold text-white mt-1">
                    {metrics.netInMBs.toFixed(2)}
                </div>
                <div className="text-sm text-slate-400 whitespace-nowrap mt-1">MB/s</div>
                <div className="text-xs text-slate-500 mt-2 font-mono">Realtime Traffic</div>
            </div>

            {/* Upload */}
            <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700">
                <div className="text-xs text-pink-400 uppercase font-black tracking-wider">Net Out</div>
                <div className="h-4"></div> {/* Unsichtbarer Platzhalter für zweite Zeile */}
                <div className="text-2xl font-mono font-bold text-white mt-1">
                    {metrics.netOutMBs.toFixed(2)}
                </div>
                <div className="text-sm text-slate-400 whitespace-nowrap mt-1">MB/s</div>
                <div className="text-xs text-slate-500 mt-2 font-mono">Realtime Traffic</div>
            </div>

        </div>
    );
};

export default ProxmoxDashboard;
