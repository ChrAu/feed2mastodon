import { useEffect } from 'react';
import ElectricityPriceDashboard from '../components/ElectricityPriceDashboard';

const Strompreis = () => {
    useEffect(() => {
        document.title = "Strompreis - codeheap.dev";
    }, []);

    return (
        <div className="container mx-auto px-6 max-w-7xl pb-24">
            <div className="mt-16 text-center">
                <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                    Aktueller Strompreis <br className="hidden md:block" />
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-teal-400">
                        Übersicht
                    </span>
                </h2>
            </div>
            <ElectricityPriceDashboard />
        </div>
    );
};

export default Strompreis;
