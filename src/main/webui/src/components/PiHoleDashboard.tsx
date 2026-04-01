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
    const StatBox = ({ title, value, valueClassName = "text-white" }: { title: string, value: React.ReactNode, valueClassName?: string }) => (
        <div className="bg-slate-800/50 backdrop-blur-md shadow-xl rounded-xl p-4 border border-slate-700 h-[166px]"> {/* Feste Höhe hinzugefügt */}
            <div className="h-8"> {/* Feste Höhe für den Titelbereich, um zwei Zeilen aufzunehmen */}
                <div className="text-xs text-orange-400 uppercase font-black tracking-wider">{title}</div>
            </div>
            {loading ? (
                <>
                    <div className="h-8 w-24 bg-slate-700 animate-pulse rounded mt-2"></div> {/* h-8 für bessere Übereinstimmung mit text-2xl */}
                    {/* Unsichtbarer Platzhalter, um die Höhe der Proxmox-Karten anzupassen */}
                    <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4 opacity-0"></div>
                </>
            ) : (
                <>
                    <div className={`text-2xl font-mono font-bold mt-2 ${valueClassName}`}>{value}</div>
                    {/* Unsichtbarer Platzhalter, um die Höhe der Proxmox-Karten anzupassen */}
                    <div className="w-full bg-slate-900 rounded-full h-1.5 mt-4 opacity-0"></div>
                </>
            )}
        </div>
    );

    return (
        <div className="p-4 bg-transparent">
            {/* Der vorherige h2-Block wurde entfernt */}
            {loading && <span className="text-xs text-gray-500 animate-pulse">Warte auf Stream...</span>}

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatBox
                    title="Blockiert (Heute)"
                    value={formatValue('sensor.pi_hole_blockierte_anzeigen', 0)}
                    valueClassName="text-red-400"
                />
                <StatBox
                    title="Anteil"
                    value={<span className="whitespace-nowrap">{formatValue('sensor.pi_hole_anteil_blockierter_anzeigen', 1)} %</span>}
                    valueClassName="text-blue-400"
                />
                <StatBox
                    title="DNS-Anfragen"
                    value={formatValue('sensor.pi_hole_dns_abfragen', 0)}
                    valueClassName="text-green-400"
                />
                <StatBox
                    title="Clients"
                    value={formatValue('sensor.pi_hole_eindeutige_dns_clients', 0)}
                    valueClassName="text-yellow-400"
                />
            </div>
        </div>
    );
};

export default PiHoleDashboard;
