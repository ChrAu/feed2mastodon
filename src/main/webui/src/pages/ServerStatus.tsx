import React from 'react';
import ProxmoxDashboard from '../components/ProxmoxDashboard';
import CpuDashboard from '../components/CpuDashboard';
import PiHoleDashboard from '../components/PiHoleDashboard';

const ServerStatus: React.FC = () => {
    return (
        <>
            <div className="container mx-auto">
                <h1 className="text-xl font-bold p-4">Server Status</h1>
                <ProxmoxDashboard />
            </div>
            <div className="container mx-auto">
                <h1 className="text-xl font-bold p-4">CPU Auslastung</h1>
                <CpuDashboard />
            </div>
            <div className="container mx-auto">
                <h1 className="text-xl font-bold p-4">Pi Hole Status</h1>
                <PiHoleDashboard />
            </div>
        </>
    );
};

export default ServerStatus;
