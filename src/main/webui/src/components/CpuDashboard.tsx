import React, { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Cpu } from '../data/cpu';

const CpuDashboard: React.FC = () => {
    const [data, setData] = useState<Cpu[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = () => {
            fetch('/api/homeassistant/cpu')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                .then(data => {
                    const sortedData = data.sort((a: Cpu, b: Cpu) => new Date(a.lastChanged).getTime() - new Date(b.lastChanged).getTime());
                    setData(sortedData);
                    setError(null);
                })
                .catch(error => {
                    setError("Verbindung verloren...");
                    console.error('Error fetching CPU data:', error);
                });
        };

        fetchData();
        const interval = setInterval(fetchData, 30000); // Refresh every 30 seconds

        return () => clearInterval(interval);
    }, []);

    if (error) return <div className="text-red-400 p-4">{error}</div>;
    if (!data.length) return <div className="text-gray-400 p-4 animate-pulse">Lade CPU Daten...</div>;

    const chartData = data.map(d => ({
        time: new Date(d.lastChanged).toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' }),
        usage: parseFloat(d.state)
    }));

    return (
        <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700 mt-4">
            <div className="text-xs text-orange-400 uppercase font-black tracking-wider">CPU-Auslastung (%)</div>
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
