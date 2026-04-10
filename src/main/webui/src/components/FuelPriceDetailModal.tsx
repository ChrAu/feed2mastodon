import React, { useState, useEffect } from 'react';
import { X, ChevronDown, ChevronUp, ChevronLeft, ChevronRight, ZoomIn, ZoomOut } from 'lucide-react';
import { FuelPriceChart, FuelPriceForecastDto, VisibleLinesState } from './FuelPriceChart';

interface FuelPrice {
    value: number;
    unit: string;
    lastChanged: string;
    entityId: string;
    previousValue?: number | null;
}

interface SavedForecastDto {
    id: number;
    createdAt: string;
    forecastDurationMinutes: number;
    rasterMinutes: number;
    dataPoints: FuelPriceForecastDto[];
}

interface FuelPriceDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    fuelPrice: FuelPrice;
    fuelType: string;
    stationName: string;
    isCheckMode: boolean;
}

const FORECAST_DAYS_HISTORY = 7; // Keep this constant here for help text

const getFuelTypeName = (fuelType: string) => {
    switch (fuelType) {
        case 'diesel': return 'Diesel';
        case 'super': return 'Super';
        case 'superE10': return 'Super E10';
        default: return fuelType;
    }
};

const FuelPriceDetailModal: React.FC<FuelPriceDetailModalProps> = ({ isOpen, onClose, fuelPrice, fuelType, stationName, isCheckMode }) => {
    const [modalDurationHours, setModalDurationHours] = useState<number>(24);
    const [selectedForecastOption, setSelectedForecastOption] = useState<'none' | 'trend_12h_holt' | '24h_holt' | '48h_holt'>('trend_12h_holt');
    const [showHelpSection, setShowHelpSection] = useState<boolean>(false);
    const [savedForecasts, setSavedForecasts] = useState<SavedForecastDto[]>([]);
    const [selectedSavedForecastId, setSelectedSavedForecastId] = useState<number | ''>('');
    const [visibleLines, setVisibleLines] = useState<VisibleLinesState>({
        value: true,
        forecastValue: true,
        aiForecastValue: true,
        savedForecastValue: true,
    });
    
    const [isCustomRange, setIsCustomRange] = useState<boolean>(false);
    const [customStartDate, setCustomStartDate] = useState<string>('');
    const [customEndDate, setCustomEndDate] = useState<string>('');

    useEffect(() => {
        if (isOpen) {
            setModalDurationHours(24);
            setSelectedForecastOption('trend_12h_holt');
            setShowHelpSection(false);
            setSelectedSavedForecastId('');
            setVisibleLines({
                value: true,
                forecastValue: true,
                aiForecastValue: true,
                savedForecastValue: true,
            });
            
            setIsCustomRange(false);
            setCustomStartDate('');
            setCustomEndDate('');

            if (isCheckMode) {
                const fetchSavedForecasts = async () => {
                    try {
                        const response = await fetch(`/api/homeassistant/fuel-prices/forecast/saved?entityId=${fuelPrice.entityId}`);
                        if (response.ok) {
                            const data: SavedForecastDto[] = await response.json();
                            setSavedForecasts(data);
                        }
                    } catch (e) {
                        console.error("Failed to fetch saved forecasts:", e);
                    }
                };
                fetchSavedForecasts();
            }
        }
    }, [isOpen, isCheckMode, fuelPrice.entityId]);

    const toggleLineVisibility = (lineKey: keyof VisibleLinesState) => {
        setVisibleLines(prev => ({
            ...prev,
            [lineKey]: !prev[lineKey],
        }));
    };

    const handleApplyCustomDateRange = () => {
        if (customStartDate && customEndDate) {
            const start = new Date(customStartDate).getTime();
            const end = new Date(customEndDate).getTime();
            
            if (end > start) {
                const diffHours = (end - start) / (1000 * 60 * 60);
                setModalDurationHours(Math.round(diffHours));
            }
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-start justify-center z-50 p-4 sm:p-6 pt-16 sm:pt-24" onClick={onClose}>
            <div className="bg-slate-800 p-4 sm:p-6 rounded-xl border border-slate-700 shadow-lg w-full max-w-4xl relative flex flex-col max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                <button onClick={onClose} className="absolute top-2 right-2 sm:top-4 sm:right-4 text-slate-400 hover:text-white z-10 p-2 bg-slate-800/50 rounded-full">
                    <X size={24} />
                </button>

                <div className="flex flex-col mb-4 sm:mb-6 pr-8">
                    <h3 className="text-xl sm:text-2xl font-bold text-white mb-4">
                        {stationName} - {getFuelTypeName(fuelType)}
                    </h3>

                    <div className="flex flex-col xl:flex-row items-start gap-4 w-full">
                        <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full xl:w-auto">
                            {[
                                { label: 'Keine Prognose', value: 'none' },
                                { label: 'Trend + 12h HW', value: 'trend_12h_holt' },
                                { label: '24h HW', value: '24h_holt' },
                                { label: '48h HW', value: '48h_holt' }
                            ].map((option) => (
                                <button
                                    key={option.value}
                                    onClick={() => setSelectedForecastOption(option.value as any)}
                                    className={`flex-1 sm:flex-none px-3 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                                        selectedForecastOption === option.value
                                            ? 'bg-blue-600 text-white'
                                            : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                                    }`}
                                >
                                    {option.label}
                                </button>
                            ))}
                        </div>

                        <div className="flex flex-wrap gap-2 bg-slate-900 rounded-lg p-1 w-full xl:w-auto">
                            {[
                                { label: '24h', value: 24 },
                                { label: '3 Tage', value: 72 },
                                { label: '7 Tage', value: 168 },
                                { label: '30 Tage', value: 720 }
                            ].map((option) => (
                                <button
                                    key={option.value}
                                    onClick={() => {
                                        setModalDurationHours(option.value);
                                        setIsCustomRange(false);
                                        setCustomStartDate('');
                                        setCustomEndDate('');
                                    }}
                                    className={`flex-1 sm:flex-none px-3 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                                        modalDurationHours === option.value && !isCustomRange
                                            ? 'bg-blue-600 text-white'
                                            : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                                    }`}
                                >
                                    {option.label}
                                </button>
                            ))}
                             <button
                                onClick={() => setIsCustomRange(true)}
                                className={`flex-1 sm:flex-none px-3 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                                    isCustomRange
                                        ? 'bg-blue-600 text-white'
                                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                                }`}
                            >
                                Benutzerdef.
                            </button>
                        </div>
                        
                        {isCustomRange && (
                            <div className="flex flex-wrap items-center gap-2 bg-slate-900 rounded-lg p-1 w-full xl:w-auto">
                                <span className="text-slate-400 text-sm pl-2">Zeitraum:</span>
                                <input 
                                    type="datetime-local" 
                                    value={customStartDate} 
                                    onChange={(e) => setCustomStartDate(e.target.value)}
                                    className="bg-slate-800 text-white text-sm rounded border border-slate-700 py-1.5 px-2 outline-none focus:border-blue-500 flex-1 min-w-[130px]" 
                                />
                                <span className="text-slate-400 text-sm">-</span>
                                <input 
                                    type="datetime-local" 
                                    value={customEndDate} 
                                    onChange={(e) => setCustomEndDate(e.target.value)}
                                    className="bg-slate-800 text-white text-sm rounded border border-slate-700 py-1.5 px-2 outline-none focus:border-blue-500 flex-1 min-w-[130px]" 
                                />
                                <button 
                                    onClick={handleApplyCustomDateRange}
                                    disabled={!customStartDate || !customEndDate}
                                    className="px-3 py-1.5 text-sm font-medium rounded-md bg-blue-600 text-white disabled:opacity-50 disabled:cursor-not-allowed hover:bg-blue-700 transition-colors"
                                >
                                    Anwenden
                                </button>
                            </div>
                        )}

                        {isCheckMode && savedForecasts.length > 0 && (
                            <div className="flex items-center gap-2 bg-slate-900 rounded-lg p-1 w-full sm:w-auto mt-2 sm:mt-0">
                                <span className="text-pink-400 text-sm font-medium px-2">Backtest:</span>
                                <select
                                    value={selectedSavedForecastId}
                                    onChange={(e) => setSelectedSavedForecastId(e.target.value ? Number(e.target.value) : '')}
                                    className="bg-slate-800 text-white text-sm rounded border border-slate-700 py-1.5 px-2 outline-none focus:border-pink-500"
                                >
                                    <option value="">-- Keine gewählt --</option>
                                    {savedForecasts.map(f => {
                                        const date = new Date(f.createdAt);
                                        const label = `${date.toLocaleDateString('de-DE')} ${date.toLocaleTimeString('de-DE', {hour: '2-digit', minute:'2-digit'})} (${f.forecastDurationMinutes / 60}h)`;
                                        return <option key={f.id} value={f.id}>{label}</option>;
                                    })}
                                </select>
                            </div>
                        )}
                    </div>
                </div>

                <div className="shrink-0 w-full relative">
                    <FuelPriceChart
                        entityId={fuelPrice.entityId}
                        fuelType={fuelType}
                        height={400}
                        durationHours={modalDurationHours}
                        customStartDate={isCustomRange ? customStartDate : ''}
                        customEndDate={isCustomRange ? customEndDate : ''}
                        selectedForecastOption={selectedForecastOption}
                        savedForecastData={savedForecasts.find(f => f.id === selectedSavedForecastId)?.dataPoints}
                        isCheckMode={isCheckMode}
                        visibleLines={visibleLines}
                    />
                </div>

                <div className="mt-4 border-t border-slate-700 pt-4">
                    <h5 className="font-semibold text-slate-300 mb-2">Sichtbarkeit der Linien:</h5>
                    <div className="flex flex-wrap gap-2 text-sm">
                        <button
                            onClick={() => toggleLineVisibility('value')}
                            className={`px-3 py-2 font-medium rounded-md transition-colors whitespace-nowrap border ${
                                visibleLines.value
                                    ? 'bg-slate-700 text-[#8884d8] border-[#8884d8]'
                                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700 border-slate-700'
                            }`}
                        >
                            <span className="inline-block w-4 h-0.5 bg-[#8884d8] mr-2"></span>Aktueller Preisverlauf
                        </button>
                        <button
                            onClick={() => toggleLineVisibility('forecastValue')}
                            className={`px-3 py-2 font-medium rounded-md transition-colors whitespace-nowrap border ${
                                visibleLines.forecastValue
                                    ? 'bg-slate-700 text-[#82ca9d] border-[#82ca9d]'
                                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700 border-slate-700'
                            }`}
                        >
                            <span className="inline-block w-4 h-0.5 bg-[#82ca9d] mr-2"></span>Prognose (Trend)
                        </button>
                        <button
                            onClick={() => toggleLineVisibility('aiForecastValue')}
                            className={`px-3 py-2 font-medium rounded-md transition-colors whitespace-nowrap border ${
                                visibleLines.aiForecastValue
                                    ? 'bg-slate-700 text-[#f59e0b] border-[#f59e0b]'
                                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700 border-slate-700'
                            }`}
                        >
                            <span className="inline-block w-4 h-0.5 bg-[#f59e0b] mr-2"></span>Prognose (Holt-Winters)
                        </button>
                        {isCheckMode && (
                            <button
                                onClick={() => toggleLineVisibility('savedForecastValue')}
                                className={`px-3 py-2 font-medium rounded-md transition-colors whitespace-nowrap border ${
                                    visibleLines.savedForecastValue
                                        ? 'bg-slate-700 text-[#ec4899] border-[#ec4899]'
                                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-700 border-slate-700'
                                }`}
                            >
                                <span className="inline-block w-4 h-0.5 bg-[#ec4899] mr-2 border-dotted"></span>Gespeicherte Prognose
                            </button>
                        )}
                    </div>
                </div>


                <div className="mt-8 border-t border-slate-700 pt-4">
                    <button
                        type="button"
                        onClick={() => setShowHelpSection(!showHelpSection)}
                        className="flex items-center justify-between w-full text-slate-300 hover:text-white text-lg font-semibold py-2"
                    >
                        Hilfe & Erklärungen
                        {showHelpSection ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                    </button>
                    <div className={`overflow-hidden transition-all duration-300 ease-in-out ${showHelpSection ? 'max-h-screen opacity-100' : 'max-h-0 opacity-0'}`}>
                        <div className="text-sm text-slate-400 space-y-3 pb-4">
                            <p>Dieses Diagramm visualisiert den historischen Preisverlauf der ausgewählten Kraftstoffart und bietet verschiedene Prognoseoptionen, um zukünftige Preisentwicklungen abzuschätzen.</p>

                            <h5 className="font-semibold text-slate-300 mt-4 mb-2">Legende der Linien:</h5>
                            <ul className="list-disc list-inside space-y-1">
                                <li><span className="inline-block w-4 h-0.5 bg-[#8884d8] mr-2"></span><strong>Aktueller Preisverlauf:</strong> Zeigt die tatsächlich erfassten und historischen Kraftstoffpreise an. Diese Daten basieren auf den von Home Assistant bereitgestellten Informationen.</li>
                                <li><span className="inline-block w-4 h-0.5 bg-[#82ca9d] mr-2"></span><strong>Prognose (Trend):</strong> Eine einfache, regelbasierte Vorhersage, die auf dem historischen Trend der letzten {FORECAST_DAYS_HISTORY} Tage basiert. Sie versucht, das typische Tagesmuster zu erkennen und fortzuschreiben.</li>
                                <li><span className="inline-block w-4 h-0.5 bg-[#f59e0b] mr-2"></span><strong>Prognose (Holt-Winters):</strong> Eine erweiterte, KI-gestützte Prognose, die das Holt-Winters-Modell verwendet. Dieses Modell ist in der Lage, saisonale Schwankungen und Trends in den Preisdaten zu erkennen und eine präzisere Vorhersage zu liefern.</li>
                                {isCheckMode && (
                                    <li><span className="inline-block w-4 h-0.5 bg-[#ec4899] mr-2 border-dotted"></span><strong>Gespeicherte Prognose (Backtesting):</strong> Diese Linie wird nur im "Check-Modus" angezeigt und repräsentiert eine zuvor gespeicherte Prognose. Sie dient dazu, die Qualität und Genauigkeit der Prognosemodelle im Nachhinein zu überprüfen und zu bewerten (Backtesting).</li>
                                )}
                            </ul>

                            {isCheckMode && (
                                <>
                                    <h5 className="font-semibold text-slate-300 mt-4 mb-2">Check-Modus Funktionen:</h5>
                                    <p>Im "Check-Modus" (aktiviert über den URL-Parameter <code>?check=true</code>) stehen zusätzliche Funktionen zur Verfügung:</p>
                                    <ul className="list-disc list-inside space-y-1">
                                        <li><strong>Zoom- und Pan-Steuerung:</strong> Oberhalb des Diagramms erscheinen Bedienelemente (<ChevronLeft size={16} className="inline-block align-middle"/> <ZoomIn size={16} className="inline-block align-middle"/> <ZoomOut size={16} className="inline-block align-middle"/> <ChevronRight size={16} className="inline-block align-middle"/>), mit denen Sie den sichtbaren Bereich des Diagramms verschieben und vergrößern/verkleinern können. Dies ist nützlich, um Details im historischen Verlauf oder in den Prognosen genauer zu analysieren.</li>
                                        <li><strong>Backtesting von Prognosen:</strong> Sie können über das Dropdown-Menü "Backtest" gespeicherte Prognosen auswählen, um deren Performance gegen die tatsächlichen historischen Daten zu vergleichen.</li>
                                        <li><strong>Erweiterter Datenverlauf:</strong> Im Check-Modus werden standardmäßig mehr historische Daten geladen, um eine umfassendere Analyse zu ermöglichen, auch wenn diese außerhalb des initial sichtbaren Bereichs liegen.</li>
                                    </ul>
                                </>
                            )}

                            <p className="mt-4">Wählen Sie oben die gewünschte Prognoseoption und den Zeitraum aus, um die Darstellung anzupassen.</p>
                        </div>
                    </div>
                </div>

                <div className="mt-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-slate-300 border-t border-slate-700 pt-4">
                    <div className="flex items-center space-x-2">
                        <span className="text-sm text-slate-400">Aktueller Preis:</span>
                        <span className="font-bold text-white text-lg whitespace-nowrap">{fuelPrice.value.toFixed(3)} €</span>
                    </div>
                    <div className="flex items-center space-x-2 text-sm text-slate-500">
                        <span className="text-slate-400">Letzte Änderung:</span>
                        <span className="whitespace-nowrap">{new Date(fuelPrice.lastChanged).toLocaleString('de-DE')}</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default FuelPriceDetailModal;
