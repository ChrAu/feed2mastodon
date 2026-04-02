import { useEffect } from 'react';
import FuelPriceDashboard from '../components/FuelPriceDashboard';

const Tanken = () => {
    useEffect(() => {
        document.title = "Tanken - codeheap.dev";
    }, []);

    return (
        <div className="container mx-auto px-6 max-w-7xl pb-24">
            <div className="mt-16 text-center">
                <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                    Aktuelle Tankstellenpreise <br className="hidden md:block" />
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-yellow-400 via-orange-400 to-red-400">
                        Übersicht
                    </span>
                </h2>
            </div>
            <FuelPriceDashboard />
        </div>
    );
};

export default Tanken;
