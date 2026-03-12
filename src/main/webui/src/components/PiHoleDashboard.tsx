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
        <div className="bg-gray-800 p-3 rounded shadow-inner">
            <p className="text-sm text-gray-400 mb-1">{title}</p>
            {loading ? (
                <div className="h-8 w-24 bg-gray-700 animate-pulse rounded"></div>
            ) : (
                <p className={`text-2xl font-mono ${colorClass}`}>{value}</p>
            )}
        </div>
    );

    return (
        <div className="p-4 bg-gray-900 text-white rounded-lg shadow-xl border border-gray-800">
            <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold flex items-center">
                    <span className={`w-3 h-3 mr-2 rounded-full ${
                        !loading && getEntity('binary_sensor.pi_hole_status')?.state === 'on'
                            ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]'
                            : 'bg-red-500 animate-pulse'
                    }`}></span>
                    Pi-Hole Live-Status
                </h2>
                {loading && <span className="text-xs text-gray-500 animate-pulse">Warte auf Stream...</span>}
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatBox
                    title="Blockiert (Heute)"
                    value={formatValue('sensor.pi_hole_blockierte_anzeigen', 0)}
                    colorClass="text-blue-400"
                />
                <StatBox
                    title="Anteil"
                    value={`${formatValue('sensor.pi_hole_anteil_blockierter_anzeigen', 1)} %`}
                    colorClass="text-purple-400"
                />
                <StatBox
                    title="DNS-Anfragen"
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