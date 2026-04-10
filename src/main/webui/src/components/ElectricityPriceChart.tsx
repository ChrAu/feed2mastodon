import React, {useEffect, useState} from 'react';
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import {RefreshCw, ZoomIn, ZoomOut, ChevronLeft, ChevronRight} from 'lucide-react';

const FORECAST_DAYS_HISTORY = 7;
const FORECAST_WEIGHT_BASE = 2;

export interface ElectricityPriceHistory {
    timestamp: string;
    value: number;
    timestampMs: number;
}

export interface ChartDataPoint {
    timestampMs: number;
    value?: number;
    forecastValue?: number;
    aiForecastValue?: number;
    savedForecastValue?: number;
}

export interface ElectricityPriceForecastDto {
    timestamp: string;
    predictedPrice: number;
}

export interface VisibleLinesState {
    value: boolean;
    forecastValue: boolean;
    aiForecastValue: boolean;
    savedForecastValue: boolean;
}

export interface ElectricityPriceChartProps {
    entityId: string;
    height?: number;
    durationHours?: number;
    customStartDate?: string;
    customEndDate?: string;
    selectedForecastOption?: 'none' | 'trend_12h_holt' | '24h_holt' | '48h_holt';
    savedForecastData?: ElectricityPriceForecastDto[];
    isCheckMode?: boolean;
    visibleLines: VisibleLinesState;
}

interface CustomTooltipProps {
    active?: boolean;
    payload?: any[];
    label?: number;
    visibleLines: VisibleLinesState;
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

        return (
            <div className="bg-slate-800 p-3 rounded-md border border-slate-700 text-white text-sm z-50 shadow-xl">
                <p className="font-bold mb-1">{formattedDate}</p>
                {payload.map((entry, index) => {
                    if (!visibleLines[entry.dataKey as keyof VisibleLinesState]) return null;

                    let labelText = '';
                    if (entry.dataKey === 'value') labelText = 'Strompreis: ';
                    else if (entry.dataKey === 'forecastValue') labelText = 'Prognose (Trend): ';
                    else if (entry.dataKey === 'aiForecastValue') labelText = 'Prognose (Holt-Winters): ';
                    else if (entry.dataKey === 'savedForecastValue') labelText = 'Gespeicherte Prognose: ';

                    return (
                        <p key={index} style={{ color: entry.color }}>
                            {labelText}
                            {Number(entry.value).toFixed(3)} €/kWh
                        </p>
                    );
                })}
            </div>
        );
    }
    return null;
};

export const ElectricityPriceChart: React.FC<ElectricityPriceChartProps> = ({ entityId, height = 200, durationHours = 24, customStartDate, customEndDate, selectedForecastOption = 'none', savedForecastData, isCheckMode = false, visibleLines }) => {
    const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [loadingAiForecast, setLoadingAiForecast] = useState(false);

    const [xDomain, setXDomain] = useState<number[]>([0, 0]);
    const [yDomain, setYDomain] = useState<any[]>(['dataMin - 0.01', 'dataMax + 0.01']);
    const [xTicks, setXTicks] = useState<number[]>([]);

    useEffect(() => {
        let isMounted = true;
        const fetchHistory = async () => {
            setLoading(true);
            setError(null);
            try {
                const isTrendForecastActive = selectedForecastOption === 'trend_12h_holt';
                const isHoltWintersForecastActive = selectedForecastOption === 'trend_12h_holt' || selectedForecastOption === '24h_holt' || selectedForecastOption === '48h_holt';

                const requiredHistoryForTrend = FORECAST_DAYS_HISTORY * 24;

                let fetchDuration = durationHours;
                
                // If custom dates are provided, fetch data that covers the range
                if (customStartDate && customEndDate) {
                    const startTs = new Date(customStartDate).getTime();
                    const now = new Date().getTime();
                    const diffHours = (now - startTs) / (1000 * 60 * 60);
                    if (diffHours > durationHours) {
                        fetchDuration = Math.round(diffHours);
                    }
                }
                
                fetchDuration = isCheckMode
                    ? Math.max(fetchDuration, 168)
                    : (isTrendForecastActive ? Math.max(fetchDuration, requiredHistoryForTrend) : fetchDuration);

                const historyResponse = await fetch(`/api/homeassistant/electricity-price/history?entityId=${entityId}&durationHours=${fetchDuration}`);

                if (!historyResponse.ok) {
                    throw new Error(`HTTP error! status: ${historyResponse.status}`);
                }

                const data: ElectricityPriceHistory[] = await historyResponse.json();
                if (!isMounted) return;

                const fullHistory = data.map(item => ({
                    ...item,
                    timestampMs: new Date(item.timestamp).getTime()
                }));

                const currentTimeMs = new Date().getTime();
                if (fullHistory.length > 0) {
                    const lastPoint = fullHistory[fullHistory.length - 1];
                    if (currentTimeMs - lastPoint.timestampMs > 60000) {
                        fullHistory.push({
                            timestamp: new Date(currentTimeMs).toISOString(),
                            value: lastPoint.value,
                            timestampMs: currentTimeMs
                        });
                    }
                }

                let forecastData: ChartDataPoint[] = [];
                if (isTrendForecastActive && fullHistory.length > 0) {
                    const lastPoint = fullHistory[fullHistory.length - 1];
                    const lastTimestampMs = lastPoint.timestampMs;
                    let previousForecastValue = lastPoint.value;
                    const berlinTimeFormatter = new Intl.DateTimeFormat('de-DE', { timeZone: 'Europe/Berlin', hour: 'numeric' });

                    const intervals = (12 * 60) / 5;
                    for (let i = 1; i <= intervals; i++) {
                        const futureMs = lastTimestampMs + i * 5 * 60 * 1000;
                        let weightedSum = 0;
                        let totalWeight = 0;

                        for (let daysAgo = 1; daysAgo <= FORECAST_DAYS_HISTORY; daysAgo++) {
                            const targetMs = futureMs - daysAgo * 24 * 60 * 60 * 1000;
                            let lastKnownPoint = null;
                            for (let j = fullHistory.length - 1; j >= 0; j--) {
                                if (fullHistory[j].timestampMs <= targetMs) {
                                    lastKnownPoint = fullHistory[j];
                                    break;
                                }
                            }

                            if (!lastKnownPoint && fullHistory.length > 0) lastKnownPoint = fullHistory[0];

                            if (lastKnownPoint) {
                                const weight = Math.pow(FORECAST_WEIGHT_BASE, FORECAST_DAYS_HISTORY - daysAgo);
                                weightedSum += lastKnownPoint.value * weight;
                                totalWeight += weight;
                            }
                        }

                        if (totalWeight > 0) {
                            const rawForecastValue = weightedSum / totalWeight;
                            let forecastValue = rawForecastValue;
                            const hourString = berlinTimeFormatter.format(new Date(futureMs));
                            const berlinHour = parseInt(hourString, 10);
                            const is12OClock = berlinHour === 12;

                            if (!is12OClock && rawForecastValue > previousForecastValue) {
                                forecastValue = previousForecastValue;
                            }

                            forecastData.push({ timestampMs: futureMs, forecastValue: forecastValue });
                            previousForecastValue = forecastValue;
                        }
                    }
                }

                const now = new Date().getTime();
                let minDisplayMs = isCheckMode ? 0 : now - durationHours * 60 * 60 * 1000;
                
                if (customStartDate && customEndDate) {
                    minDisplayMs = new Date(customStartDate).getTime();
                }

                let displayHistory: ChartDataPoint[] = fullHistory
                    .filter(pt => pt.timestampMs >= minDisplayMs)
                    .map(pt => ({ timestampMs: pt.timestampMs, value: pt.value }));

                const updateChartData = (aiForecastData: ChartDataPoint[] = []) => {
                    const pointMap = new Map<number, ChartDataPoint>();

                    const addPoint = (ts: number, data: Partial<ChartDataPoint>) => {
                        const roundedTs = Math.round(ts / (5 * 60 * 1000)) * (5 * 60 * 1000);
                        if (!pointMap.has(roundedTs)) pointMap.set(roundedTs, { timestampMs: roundedTs });
                        Object.assign(pointMap.get(roundedTs)!, data);
                    };

                    displayHistory.forEach(pt => addPoint(pt.timestampMs, { value: pt.value }));
                    forecastData.forEach(pt => addPoint(pt.timestampMs, { forecastValue: pt.forecastValue }));
                    aiForecastData.forEach(pt => addPoint(pt.timestampMs, { aiForecastValue: pt.aiForecastValue }));

                    if (savedForecastData) {
                        savedForecastData.forEach(pt => {
                            const ts = new Date(pt.timestamp).getTime();
                            addPoint(ts, { savedForecastValue: pt.predictedPrice });
                        });
                    }

                    if (displayHistory.length > 0) {
                        const lastHistoryVal = displayHistory[displayHistory.length - 1].value;
                        const lastHistoryTs = displayHistory[displayHistory.length - 1].timestampMs;
                        if (forecastData.length > 0) addPoint(lastHistoryTs, { forecastValue: lastHistoryVal });
                        if (aiForecastData.length > 0) addPoint(lastHistoryTs, { aiForecastValue: lastHistoryVal });
                    }

                    const finalChartData = Array.from(pointMap.values()).sort((a, b) => a.timestampMs - b.timestampMs);

                    let currentValue: number | undefined = undefined;
                    let currentForecast: number | undefined = undefined;
                    let currentAiForecast: number | undefined = undefined;
                    let currentSavedForecast: number | undefined = undefined;

                    finalChartData.forEach(pt => {
                        if (pt.value !== undefined) currentValue = pt.value;
                        else if (currentValue !== undefined && pt.timestampMs <= currentTimeMs) pt.value = currentValue;

                        if (pt.forecastValue !== undefined) currentForecast = pt.forecastValue;
                        else if (currentForecast !== undefined && pt.timestampMs >= (forecastData[0]?.timestampMs || 0)) pt.forecastValue = currentForecast;

                        if (pt.aiForecastValue !== undefined) currentAiForecast = pt.aiForecastValue;
                        else if (currentAiForecast !== undefined && pt.timestampMs >= (aiForecastData[0]?.timestampMs || 0)) pt.aiForecastValue = currentAiForecast;

                        if (pt.savedForecastValue !== undefined) currentSavedForecast = pt.savedForecastValue;
                        else if (currentSavedForecast !== undefined && savedForecastData && pt.timestampMs >= new Date(savedForecastData[0].timestamp).getTime()) pt.savedForecastValue = currentSavedForecast;
                    });

                    return finalChartData;
                };

                const initialChartData = updateChartData([]);
                setChartData(initialChartData);

                if (initialChartData.length > 0) {
                    let expectedForecastHours = 0;
                    if (selectedForecastOption === 'trend_12h_holt') expectedForecastHours = 12;
                    else if (selectedForecastOption === '24h_holt') expectedForecastHours = 24;
                    else if (selectedForecastOption === '48h_holt') expectedForecastHours = 48;

                    let initialMinTs = now - (durationHours * 60 * 60 * 1000);
                    let maxTimestamp = now + (expectedForecastHours * 60 * 60 * 1000);
                    
                    if (customStartDate && customEndDate) {
                        initialMinTs = new Date(customStartDate).getTime();
                        maxTimestamp = new Date(customEndDate).getTime();
                    }
                    
                    setXDomain([initialMinTs, maxTimestamp]);
                }

                if (isHoltWintersForecastActive) {
                    setLoadingAiForecast(true);
                    let holtWintersForecastHours = 0;
                    if (selectedForecastOption === 'trend_12h_holt') holtWintersForecastHours = 12;
                    else if (selectedForecastOption === '24h_holt') holtWintersForecastHours = 24;
                    else if (selectedForecastOption === '48h_holt') holtWintersForecastHours = 48;

                    if (holtWintersForecastHours > 0) {
                        fetch(`/api/homeassistant/electricity-price/forecast?entityId=${entityId}&forecastHours=${holtWintersForecastHours}`)
                            .then(res => {
                                if (!res.ok) throw new Error("Failed to fetch AI forecast");
                                return res.json();
                            })
                            .then(aiData => {
                                if (!isMounted) return;
                                const newAiForecastData = aiData.map((item: any) => ({
                                    timestampMs: new Date(item.timestamp).getTime(),
                                    aiForecastValue: item.predictedPrice
                                }));
                                const updatedChartData = updateChartData(newAiForecastData);
                                setChartData(updatedChartData);
                            })
                            .catch(e => console.warn("Failed to fetch Holt-Winters forecast", e))
                            .finally(() => { if (isMounted) setLoadingAiForecast(false); });
                    } else {
                        if (isMounted) setLoadingAiForecast(false);
                    }
                } else {
                    if (isMounted) setLoadingAiForecast(false);
                }

            } catch (err) {
                console.error(`Failed to fetch history for ${entityId}:`, err);
                if (isMounted) setError("Fehler beim Laden der Verlaufsdaten.");
            } finally {
                if (isMounted) setLoading(false);
            }
        };

        fetchHistory().catch(console.error);

        return () => { isMounted = false; };
    }, [entityId, durationHours, customStartDate, customEndDate, selectedForecastOption, savedForecastData, isCheckMode]);


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
        else if (rangeHours <= 24 * 7) tickIntervalMs = 24 * 60 * 60 * 1000; // 1 day
        else if (rangeHours <= 24 * 14) tickIntervalMs = 2 * 24 * 60 * 60 * 1000; // 2 days
        else tickIntervalMs = 3 * 24 * 60 * 60 * 1000; // 3 days

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

        const visibleData = chartData.filter(d => d.timestampMs >= minTs && d.timestampMs <= maxTs);
        let visibleMin = Number.MAX_VALUE;
        let visibleMax = Number.MIN_VALUE;

        visibleData.forEach(d => {
            if (visibleLines.value && d.value !== undefined) { visibleMin = Math.min(visibleMin, d.value); visibleMax = Math.max(visibleMax, d.value); }
            if (visibleLines.forecastValue && d.forecastValue !== undefined) { visibleMin = Math.min(visibleMin, d.forecastValue); visibleMax = Math.max(visibleMax, d.forecastValue); }
            if (visibleLines.aiForecastValue && d.aiForecastValue !== undefined) { visibleMin = Math.min(visibleMin, d.aiForecastValue); visibleMax = Math.max(visibleMax, d.aiForecastValue); }
            if (visibleLines.savedForecastValue && d.savedForecastValue !== undefined) { visibleMin = Math.min(visibleMin, d.savedForecastValue); visibleMax = Math.max(visibleMax, d.savedForecastValue); }
        });

        if (visibleMin !== Number.MAX_VALUE) {
            setYDomain([visibleMin - 0.01, visibleMax + 0.01]);
        }

    }, [xDomain, chartData, visibleLines]);

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


    if (loading) return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25 animate-pulse bg-slate-800/30 rounded-lg">Lade Verlauf...</div>;
    if (error) return <div className="text-red-400 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Fehler: {error}</div>;
    if (chartData.length === 0) return <div className="text-slate-500 text-center text-sm py-2 h-full flex items-center justify-center min-h-25">Keine Verlaufsdaten verfügbar.</div>;

    const isTrendForecastActive = selectedForecastOption === 'trend_12h_holt';
    const isHoltWintersForecastActive = selectedForecastOption === 'trend_12h_holt' || selectedForecastOption === '24h_holt' || selectedForecastOption === '48h_holt';

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
                <LineChart data={chartData} margin={{ top: 25, right: 40, left: 10, bottom: 30 }}>
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
                    <YAxis
                        stroke="#94a3b8"
                        domain={yDomain}
                        allowDataOverflow={true}
                        tickFormatter={(value) => value.toFixed(3) + ' €/kWh'}
                    />
                    <Tooltip content={<CustomTooltip visibleLines={visibleLines} />} />
                    {visibleLines.value && <Line type="stepAfter" dataKey="value" stroke="#8884d8" strokeWidth={2} dot={false} fill="none" isAnimationActive={false} />}

                    {visibleLines.forecastValue && isTrendForecastActive && (
                        <Line type="stepAfter" dataKey="forecastValue" stroke="#82ca9d" strokeWidth={2} strokeDasharray="5 5" dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}
                    {visibleLines.aiForecastValue && isHoltWintersForecastActive && (
                        <Line type="stepAfter" dataKey="aiForecastValue" stroke="#f59e0b" strokeWidth={2} strokeDasharray="5 5" dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}

                    {visibleLines.savedForecastValue && savedForecastData && savedForecastData.length > 0 && (
                        <Line type="stepAfter" dataKey="savedForecastValue" stroke="#ec4899" strokeWidth={2} strokeDasharray="3 3" dot={false} fill="none" isAnimationActive={false} connectNulls={true} />
                    )}

                    {loadingAiForecast && (
                        <g>
                            <foreignObject x="calc(100% - 40px - 32px)" y="25" width="32" height="32">
                                <div className="p-2 bg-slate-800/50 rounded-full flex items-center justify-center">
                                    <RefreshCw className="w-4 h-4 animate-spin text-blue-400" />
                                </div>
                            </foreignObject>
                        </g>
                    )}
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
};
