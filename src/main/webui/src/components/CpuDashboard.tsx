import { useEffect, useState, useRef } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Cpu } from '../data/cpu';

const CpuDashboard = () => {
    const [data, setData] = useState<Cpu[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'error'>('connecting');
    const eventSourceRef = useRef<EventSource | null>(null);
    const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

    const reconnectInterval = 5000; // 5 seconds

    const connect = () => {
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
            reconnectTimeoutRef.current = null;
        }

        setConnectionStatus('connecting');
        const eventSource = new EventSource('/api/homeassistant/cpu/stream');
        eventSourceRef.current = eventSource;

        eventSource.onopen = () => {
            setConnectionStatus('connected');
            setError(null);
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
                reconnectTimeoutRef.current = null;
            }
        };

        eventSource.onmessage = (event) => {
            try {
                const parsedData: Cpu[] = JSON.parse(event.data);
                const sortedData = parsedData.sort((a, b) => new Date(a.lastChanged).getTime() - new Date(b.lastChanged).getTime());
                setData(sortedData);
                setError(null); // Clear error on successful message
                if (reconnectTimeoutRef.current) {
                    clearTimeout(reconnectTimeoutRef.current);
                    reconnectTimeoutRef.current = null;
                }
            } catch (e) {
                console.error('Fehler beim Parsen der CPU Daten:', e);
            }
        };

        eventSource.onerror = (err) => {
            console.error("EventSource Fehler (CPU):", err);
            setConnectionStatus('error');
            setError("Verbindung verloren. Versuche neu zu verbinden...");
            eventSource.close(); // Close current connection

            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
            }
            reconnectTimeoutRef.current = setTimeout(() => {
                console.log("Attempting to reconnect to CPU stream...");
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

    if (error && data.length === 0) return <div className="text-red-400 p-4">{error}</div>;
    if (!data.length && connectionStatus === 'connecting') return <div className="text-gray-400 p-4 animate-pulse">Lade CPU Daten...</div>;

    const chartData = data.map(d => ({
        time: new Date(d.lastChanged).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' }),
        usage: parseFloat(d.state)
    }));

    return (
        <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700 mt-4">
            <div className="flex justify-between items-center mb-2">
                <div className="text-xs text-orange-400 uppercase font-black tracking-wider">CPU-Auslastung (%)</div>
                <div className="flex items-center space-x-2 text-xs">
                    {connectionStatus === 'connected' && <span className="text-green-400 flex items-center"><span className="w-2 h-2 rounded-full bg-green-400 mr-1 animate-pulse"></span>Live</span>}
                    {connectionStatus === 'connecting' && <span className="text-yellow-400 animate-pulse">Verbinde...</span>}
                    {connectionStatus === 'error' && <span className="text-red-400">Getrennt</span>}
                </div>
            </div>
            <div style={{ width: '100%', height: 300 }}>
                <ResponsiveContainer>
                    <LineChart
                        data={chartData}
                        margin={{
                            top: 20, right: 30, left: 0, bottom: 5,
                        }}
                    >
                        <CartesianGrid stroke="#334155" strokeDasharray="3 3" />
                        <XAxis dataKey="time" tick={{ fill: '#94a3b8' }} />
                        <YAxis tick={{ fill: '#94a3b8' }} unit="%" />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: 'rgba(30, 41, 59, 0.9)',
                                borderColor: '#334155',
                                borderRadius: '0.5rem',
                            }}
                            labelStyle={{ color: '#cbd5e1' }}
                            itemStyle={{ color: '#fb923c', fontWeight: 'bold' }}
                        />
                        <Legend wrapperStyle={{ color: '#94a3b8' }} />
                        <Line
                            type="monotone"
                            dataKey="usage"
                            name="Auslastung"
                            stroke="#fb923c" // orange-400
                            strokeWidth={2}
                            dot={false}
                            activeDot={{ r: 6, fill: '#fb923c', stroke: '#1e293b' }}
                        />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default CpuDashboard;
