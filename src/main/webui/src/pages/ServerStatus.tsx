import { useEffect } from 'react';
import ProxmoxDashboard from '../components/ProxmoxDashboard';
import CpuDashboard from '../components/CpuDashboard';
import PiHoleDashboard from '../components/PiHoleDashboard';

const ServerStatus = () => {
    useEffect(() => {
        document.title = "Server Status - codeheap.dev";
    }, []);

    return (
        <div className="container mx-auto px-6 max-w-7xl pb-24">
            <div className="mt-16 text-center">
                <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                    System-Status <br className="hidden md:block" />
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-green-400 via-teal-400 to-cyan-400">
                        Übersicht
                    </span>
                </h2>
            </div>
            
            <div className="space-y-12">
                <div>
                    <h3 className="text-2xl font-bold text-white mb-4">Proxmox Server</h3>
                    <ProxmoxDashboard />
                </div>
                <div>
                    <h3 className="text-2xl font-bold text-white mb-4">CPU Auslastung</h3>
                    <CpuDashboard />
                </div>
                <div>
                    <h3 className="text-2xl font-bold text-white mb-4">Pi-Hole Status</h3>
                    <PiHoleDashboard />
                </div>
            </div>
        </div>
    );
};

export default ServerStatus;
