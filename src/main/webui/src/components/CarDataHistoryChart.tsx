import React, { useEffect, useState } from 'react';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ZoomIn, ZoomOut, ChevronLeft, ChevronRight } from 'lucide-react';

export interface CarDataHistory {
    timestamp: string;
    value: number;
    timestampMs: number;
}

export interface CarChartDataPoint {
    timestampMs: number;
    electricRange?: number;
    batteryLevel?: number;
    externalTemperature?: number;
    rangeAt100Percent?: number; // Neu hinzugefügt
}

export interface VisibleCarLinesState {
    electricRange?: boolean;
    batteryLevel?: boolean;
    externalTemperature?: boolean;
    rangeAt100Percent?: boolean; // Neu hinzugefügt
}

export interface CarDataHistoryChartProps {
    durationHours?: number;
    height?: number;
    isCheckMode?: boolean;
    visibleLines?: VisibleCarLinesState;
}

interface CustomTooltipProps {
    active?: boolean;
    payload?: any[];
    label?: number;
    visibleLines: VisibleCarLinesState;
}

const CustomTooltip: React.FC<CustomTooltipProps> = ({ active, payload, label, visibleLines }) => {
    if (active && payload && payload.length) {
        const date = new Date(label!);
        const formattedDate = date.toLocaleDateString('de-DE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        // Eigene Reihenfolge für den Tooltip festlegen
        const orderedKeys = ['electricRange', 'rangeAt100Percent', 'externalTemperature', 'batteryLevel']; // 'rangeAt100Percent' hinzugefügt

        return (
            <div className="bg-slate-800 p-3 rounded-md border border-slate-700 text-white text-sm z-50 shadow-xl">
                <p className="font-bold mb-1">{formattedDate}</p>
                {orderedKeys.map((key) => {
                    const entry = payload.find(p => p.dataKey === key);
                    if (!entry) return null;

                    if (key === 'electricRange' && !visibleLines.electricRange) return null;
                    if (key === 'batteryLevel' && !visibleLines.batteryLevel) return null;
                    if (key === 'externalTemperature' && !visibleLines.externalTemperature) return null;
                    if (key === 'rangeAt100Percent' && !visibleLines.rangeAt100Percent) return null; // Neu hinzugefügt

                    let labelText = '';
                    let unit = '';
                    if (key === 'electricRange') { labelText = 'Reichweite: '; unit = ' km'; }
                    else if (key === 'batteryLevel') { labelText = 'Batterie: '; unit = ' %'; }
                    else if (key === 'externalTemperature') { labelText = 'Außentemp.: '; unit = ' °C'; }
                    else if (key === 'rangeAt100Percent') { labelText = 'Reichw. (100%): '; unit = ' km'; } // Neu hinzugefügt

                    return (
                        <p key={key} style={{ color: entry.color }}>
                            {Number(entry.value).toFixed(1)}{unit}
                        </p>
                    );
                })}
            </div>
        );
    }
    return null;
};

export const CarDataHistoryChart: React.FC<CarDataHistoryChartProps> = ({ durationHours = 24, height = 200, isCheckMode = false, visibleLines }) => {
    // Standardmäßig alle Linien aktivieren, falls sie nicht explizit im State des Dashboards (Elternkomponente) übergeben werden
    const activeLines = {
        electricRange: visibleLines?.electricRange ?? true,
        batteryLevel: visibleLines?.batteryLevel ?? true,
        externalTemperature: visibleLines?.externalTemperature ?? true,
        rangeAt100Percent: visibleLines?.rangeAt100Percent ?? true, // Neu hinzugefügt
    };

    const [chartData, setChartData] = useState<CarChartDataPoint[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [xDomain, setXDomain] = useState<number[]>([0, 0]);
    const [yDomainLeft, setYDomainLeft] = useState<any[]>(['dataMin - 5', 'dataMax + 5']); // Nur noch für electricRange dynamisch
    const [xTicks, setXTicks] = useState<number[]>([]);

    const CAR_ENTITY_IDS = {
        electricRange: "sensor.id4_mercatis_gmbh_electric_range",
        batteryLevel: "sensor.id4_mercatis_gmbh_battery_level",
        // externalTemperature wird hier geladen, aber wir überschreiben es später mit Dummy-Werten
        externalTemperature: "sensor.temperatur_am_standort_christopher"
    };

    useEffect(() => {
        let isMounted = true;
        const fetchHistory = async () => {
            setLoading(true);
            setChartData([]); // Clear previous data to show loading animation immediately
            setError(null);
            try {
                const currentTimeMs = new Date().getTime();
                const initialMaxDisplayMs = currentTimeMs;
                const initialMinDisplayMs = currentTimeMs - durationHours * 60 * 60 * 1000;

                // Determine aggregationMinutes based on durationHours
                let aggregationMinutes: number;
                if (durationHours <= 24) {
                    aggregationMinutes = 2; // 2 minutes for up to 24 hours
                } else if (durationHours <= 168) { // 7 days
                    aggregationMinutes = 15; // 15 minutes for up to 7 days
                } else {
                    aggregationMinutes = 30; // 1 hours for longer durations
                }

                const allHistoryData: { [key: string]: CarDataHistory[] } = {};
                const fetchPromises = Object.entries(CAR_ENTITY_IDS).map(async ([key, entityId]) => {
                    const response = await fetch(`/api/homeassistant/car-data/history?entityId=${entityId}&durationHours=${durationHours}&aggregationMinutes=${aggregationMinutes}`);
                    if (!response.ok) throw new Error(`HTTP error! status: ${response.status} for ${entityId}`);
                    const data: CarDataHistory[] = await response.json();
                    allHistoryData[key] = data.map(item => ({
                        ...item,
                        timestampMs: new Date(item.timestamp).getTime()
                    }));
                });

                await Promise.all(fetchPromises);
                if (!isMounted) return;

                const pointMap = new Map<number, CarChartDataPoint>();

                const addPoint = (ts: number, data: Partial<CarChartDataPoint>) => {
                    const roundedTs = Math.round(ts / (5 * 60 * 1000)) * (5 * 60 * 1000); // Round to 5-min intervals
                    if (!pointMap.has(roundedTs)) pointMap.set(roundedTs, { timestampMs: roundedTs });
                    Object.assign(pointMap.get(roundedTs)!, data);
                };

                Object.entries(allHistoryData).forEach(([key, history]) => {
                    history.forEach(pt => {
                        if (key === 'electricRange') addPoint(pt.timestampMs, { electricRange: pt.value });
                        else if (key === 'batteryLevel') addPoint(pt.timestampMs, { batteryLevel: pt.value });
                        else if (key === 'externalTemperature') addPoint(pt.timestampMs, { externalTemperature: pt.value });
                    });
                });

                const finalChartData = Array.from(pointMap.values()).sort((a, b) => a.timestampMs - b.timestampMs);

                // Lücken füllen
                let lastElectricRange: number | undefined = undefined;
                let lastBatteryLevel: number | undefined = undefined;
                let lastExternalTemperature: number | undefined = undefined;

                finalChartData.forEach(pt => {
                    if (pt.electricRange !== undefined) lastElectricRange = pt.electricRange;
                    else pt.electricRange = lastElectricRange;

                    if (pt.batteryLevel !== undefined) lastBatteryLevel = pt.batteryLevel;
                    else pt.batteryLevel = lastBatteryLevel;

                    if (pt.externalTemperature !== undefined) lastExternalTemperature = pt.externalTemperature;
                    else pt.externalTemperature = lastExternalTemperature;
                });

                // Berechnung der Reichweite für 100% Akku
                finalChartData.forEach(pt => {
                    if (pt.electricRange !== undefined && pt.batteryLevel !== undefined && pt.batteryLevel > 0) {
                        pt.rangeAt100Percent = (pt.electricRange / pt.batteryLevel) * 100;
                    }
                });

                setChartData(finalChartData);
                setXDomain([initialMinDisplayMs, initialMaxDisplayMs]);

            } catch (err) {
                console.error("Failed to fetch car data history:", err);
                if (isMounted) setError("Fehler beim Laden der Fahrzeugverlaufsdaten.");
            } finally {
                if (isMounted) setLoading(false);
            }
        };

        fetchHistory().catch(console.error);

        return () => { isMounted = false; };
    }, [durationHours]);

    useEffect(() => {
        if (xDomain[0] === 0 || chartData.length === 0) return;

        const minTs = xDomain[0];
        const maxTs = xDomain[1];

        const rangeHours = (maxTs - minTs) / 3600000;
        let tickIntervalMs;
        if (rangeHours <= 12) tickIntervalMs = 2 * 60 * 60 * 1000;
        else if (rangeHours <= 24) tickIntervalMs = 4 * 60 * 60 * 1000;
        else if (rangeHours <= 48) tickIntervalMs = 8 * 60 * 60 * 1000;
        else if (rangeHours <= 96) tickIntervalMs = 12 * 60 * 60 * 1000;
        else if (rangeHours <= 24 * 7) tickIntervalMs = 24 * 60 * 60 * 1000;
        else if (rangeHours <= 24 * 14) tickIntervalMs = 2 * 24 * 60 * 60 * 1000;
        else tickIntervalMs = 3 * 24 * 60 * 60 * 1000;

        const generatedGridTicks: number[] = [];
        const startDate = new Date(minTs);
        startDate.setHours(0, 0, 0, 0);
        let currentTickTime = startDate.getTime();

        while (currentTickTime < minTs) currentTickTime += tickIntervalMs;
        const xAxisDomainUpperBound = maxTs + 1000000;
        while (currentTickTime <= xAxisDomainUpperBound) {
            generatedGridTicks.push(currentTickTime);
            currentTickTime += tickIntervalMs;
        }
        setXTicks(generatedGridTicks);

        // Dynamische Skalierung für Reichweite und Reichweite bei 100% (linke Achse)
        let minLeftAxisValue = Number.MAX_VALUE;
        let maxLeftAxisValue = Number.MIN_VALUE;

        const visibleData = chartData.filter(d => d.timestampMs >= minTs && d.timestampMs <= maxTs);

        visibleData.forEach(d => {
            if (activeLines.electricRange && d.electricRange !== undefined) {
                minLeftAxisValue = Math.min(minLeftAxisValue, d.electricRange);
                maxLeftAxisValue = Math.max(maxLeftAxisValue, d.electricRange);
            }
            if (activeLines.rangeAt100Percent && d.rangeAt100Percent !== undefined) {
                minLeftAxisValue = Math.min(minLeftAxisValue, d.rangeAt100Percent);
                maxLeftAxisValue = Math.max(maxLeftAxisValue, d.rangeAt100Percent);
            }
        });

        if (minLeftAxisValue !== Number.MAX_VALUE) {
            const padding = (maxLeftAxisValue - minLeftAxisValue) * 0.1;
            setYDomainLeft([minLeftAxisValue - padding, maxLeftAxisValue + padding]);
        } else {
            setYDomainLeft([0, 500]); // Fallback, wenn keine Daten sichtbar sind
        }

    }, [xDomain, chartData, activeLines.electricRange, activeLines.batteryLevel, activeLines.externalTemperature, activeLines.rangeAt100Percent]); // 'activeLines.rangeAt100Percent' hinzugefügt

    const handleZoomIn = () => {
        setXDomain(prev => {
            if (prev[0] === 0) return prev;
            const range = prev[1] - prev[0];
            const quarter = range / 4;
            return [prev[0] + quarter, prev[1] - quarter];
        });
    };

    const handleZoomOut = () => {
        setXDomain(prev => {
            if (prev[0] === 0) return prev;
            const range = prev[1] - prev[0];
            const half = range / 2;
            return [prev[0] - half, prev[1] + half];
        });
    };

    const handlePanLeft = () => {
        setXDomain(prev => {
            if (prev[0] === 0) return prev;
            const shift = (prev[1] - prev[0]) * 0.25;
            return [prev[0] - shift, prev[1] - shift];
        });
    };

    const handlePanRight = () => {
        setXDomain(prev => {
            if (prev[0] === 0) return prev;
            const shift = (prev[1] - prev[0]) * 0.25;
            return [prev[0] + shift, prev[1] + shift];
        });
    };

    if (loading) return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25 animate-pulse bg-slate-800/30 rounded-lg">Lade Fahrzeugverlauf...</div>;
    if (error) return <div className="text-red-400 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Fehler: {error}</div>;
    if (chartData.length === 0) return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Keine Fahrzeugverlaufsdaten verfügbar.</div>;

    // --- DYNAMISCHE RECHTE ACHSE LOGIK ---
    const showRightAxis = activeLines.batteryLevel || activeLines.externalTemperature;

    let rightAxisLabel = '';
    let rightAxisFormatter = (value: number) => value.toFixed(0);
    let rightAxisDomain: any[] = [-20, 100];

    if (activeLines.batteryLevel && activeLines.externalTemperature) {
        rightAxisLabel = 'Batterie (%) / Temp (°C)';
        rightAxisFormatter = (value) => value.toFixed(0);
        rightAxisDomain = [-20, 100]; // Beide sichtbar: Fixe Skala, damit beide reinpassen
    } else if (activeLines.batteryLevel) {
        rightAxisLabel = 'Batterie (%)';
        rightAxisFormatter = (value) => value.toFixed(0) + ' %';
        rightAxisDomain = [0, 100]; // Nur Batterie: 0 bis 100%
    } else if (activeLines.externalTemperature) {
        rightAxisLabel = 'Außentemperatur (°C)';
        rightAxisFormatter = (value) => value.toFixed(0) + ' °C';
        rightAxisDomain = ['dataMin - 5', 'dataMax + 5']; // Nur Temperatur: Dynamischer Zoom!
    }

    return (
        <div className="relative w-full" style={{ height: height }}>
            {isCheckMode && (
                <div className="absolute top-2 right-10 z-10 flex gap-1 bg-slate-800/90 p-1.5 rounded-lg border border-slate-600 shadow-xl backdrop-blur-sm">
                    <button onClick={handlePanLeft} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 transition-colors" title="Nach links (ältere Daten)">
                        <ChevronLeft size={18} />
                    </button>
                    <div className="w-px bg-slate-600 mx-1"></div>
                    <button onClick={handleZoomIn} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 transition-colors" title="Reinzoomen (+)">
                        <ZoomIn size={18} />
                    </button>
                    <button onClick={handleZoomOut} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 transition-colors" title="Rauszoomen (-)">
                        <ZoomOut size={18} />
                    </button>
                    <div className="w-px bg-slate-600 mx-1"></div>
                    <button onClick={handlePanRight} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 transition-colors" title="Nach rechts (neuere Daten)">
                        <ChevronRight size={18} />
                    </button>
                </div>
            )}

            <ResponsiveContainer width="100%" height="100%">
                {/* Margins angepasst für genug Platz der Achsenbeschriftungen */}
                <LineChart data={chartData} margin={{ top: 25, right: showRightAxis ? 50 : 10, left: 30, bottom: 30 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#475569" />
                    <XAxis
                        dataKey="timestampMs"
                        type="number"
                        scale="time"
                        domain={xDomain}
                        allowDataOverflow={true}
                        ticks={xTicks}
                        tickFormatter={(timestampMs) => {
                            const date = new Date(timestampMs);
                            const timeString = date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' });
                            const rangeHours = (xDomain[1] - xDomain[0]) / 3600000;

                            if (rangeHours > 24 || timeString === '00:00') {
                                return `${date.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })} ${timeString}`;
                            }
                            return timeString;
                        }}
                        stroke="#94a3b8"
                        minTickGap={20}
                        tick={{ fontSize: 12 }}
                        angle={-45}
                        textAnchor="end"
                        height={60}
                    />

                    {/* Linke Achse: Reichweite */}
                    {(activeLines.electricRange || activeLines.rangeAt100Percent) && ( // Angepasst für die neue Linie
                        <YAxis
                            yAxisId="left"
                            width={80}
                            stroke="#8884d8"
                            domain={yDomainLeft}
                            allowDataOverflow={true}
                            tickFormatter={(value) => value.toFixed(0) + ' km'}
                            orientation="left"
                            label={{
                                value: 'Reichweite',
                                angle: -90,
                                position: 'insideLeft',
                                style: { textAnchor: 'middle', fill: '#8884d8'},
                                offset: -10
                            }}
                        />
                    )}

                    {/* Rechte Achse: Dynamisch für Batterie & Temperatur */}
                    {showRightAxis && (
                        <YAxis
                            yAxisId="right"
                            orientation="right"
                            stroke="#94a3b8"
                            domain={rightAxisDomain}
                            allowDataOverflow={true}
                            tickFormatter={rightAxisFormatter}
                            label={{
                                value: rightAxisLabel,
                                angle: 90,
                                position: 'insideRight',
                                style: { textAnchor: 'middle', fill: '#94a3b8' },
                                offset: 15
                            }}
                        />
                    )}

                    <Tooltip content={<CustomTooltip visibleLines={activeLines as VisibleCarLinesState} />} />

                    {/* Linien */}
                    {activeLines.electricRange && (
                        <Line yAxisId="left" type="monotone" dataKey="electricRange" stroke="#8884d8" strokeWidth={3} dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}
                    {activeLines.rangeAt100Percent && ( // Neu hinzugefügt
                        <Line yAxisId="left" type="monotone" dataKey="rangeAt100Percent" stroke="#ffc658" strokeWidth={2} dot={false} fill="none" isAnimationActive={false} connectNulls={true} strokeDasharray="3 3" />
                    )}
                    {activeLines.batteryLevel && (
                        <Line yAxisId="right" type="monotone" dataKey="batteryLevel" stroke="#82ca9d" strokeWidth={2} dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}
                    {activeLines.externalTemperature && (
                        <Line yAxisId="right" type="monotone" dataKey="externalTemperature" stroke="#ff7300" strokeDasharray="5 5" strokeWidth={2} dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}

                </LineChart>
            </ResponsiveContainer>
        </div>
    );
};
