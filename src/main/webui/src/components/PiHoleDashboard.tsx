import React, { useEffect, useState } from 'react';
import { PiHoleEntity, subscribeToPiHole } from '../data/pihole';

const PiHoleDashboard: React.FC = () => {
    const [metrics, setMetrics] = useState<PiHoleEntity[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const unsubscribe = subscribeToPiHole((data) => {
            setMetrics(data);
            setLoading(false); // Sobald die ersten Daten da sind, stoppt das Laden
        });
        return unsubscribe;
    }, []);

    const getEntity = (id: string) => metrics.find(m => m.entity_id === id);

    const formatValue = (id: string, decimals: number = 1) => {
        const entity = getEntity(id);
        if (!entity) return '--';
        const value = parseFloat(entity.state);
        return isNaN(value) ? entity.state : value.toFixed(decimals);
    };

    // Komponente für ein einzelnes Statistik-Feld mit Skeleton-Support
    const StatBox = ({ title, value, colorClass }: { title: string, value: string, colorClass: string }) => (
        <div className="bg-slate-800/50 p-4 rounded-xl shadow-inner flex flex-col justify-between border border-slate-700 min-h-[140px]">
            <p className="text-xs text-slate-400 mb-2 font-semibold uppercase tracking-wider">{title}</p>
            {loading ? (
                <div className="h-8 w-full bg-slate-700 animate-pulse rounded mt-auto"></div>
            ) : (
                <div className="mt-auto">
                    <p className={`text-2xl lg:text-3xl font-mono font-bold mt-auto ${colorClass}`}>{value}</p>
                    <div className="h-1.5 mt-4 invisible"></div> {/* Platzhalter um auf selbe höhe wie proxmax ladebalken zu kommen */}
                </div>
            )}
        </div>
    );

    return (
        <div className="p-4 bg-transparent text-white rounded-lg h-full flex flex-col justify-between">
            <div className="flex items-center justify-between mb-6">
                <h2 className="text-sm font-bold flex items-center text-slate-300 uppercase tracking-widest">
                    <span className={`w-2.5 h-2.5 mr-3 rounded-full ${
                        !loading && getEntity('binary_sensor.pi_hole_status')?.state === 'on'
                            ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]'
                            : 'bg-red-500 animate-pulse'
                    }`}></span>
                    Live-Status
                </h2>
                {loading && <span className="text-xs text-slate-500 animate-pulse bg-slate-800/50 px-2 py-1 rounded-md border border-slate-700">Warte auf Stream...</span>}
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 flex-grow">
                <StatBox
                    title="Blockiert"
                    value={formatValue('sensor.pi_hole_blockierte_anzeigen', 0)}
                    colorClass="text-blue-400"
                />
                <StatBox
                    title="Anteil"
                    value={`${formatValue('sensor.pi_hole_anteil_blockierter_anzeigen', 1)}%`}
                    colorClass="text-purple-400"
                />
                <StatBox
                    title="Anfragen"
                    value={formatValue('sensor.pi_hole_dns_abfragen', 0)}
                    colorClass="text-green-400"
                />
                <StatBox
                    title="Clients"
                    value={formatValue('sensor.pi_hole_eindeutige_dns_clients', 0)}
                    colorClass="text-yellow-400"
                />
            </div>
        </div>
    );
};

export default PiHoleDashboard;
