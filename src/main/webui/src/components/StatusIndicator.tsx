import React, { useState, useEffect } from 'react';

const StatusIndicator = () => {
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    // Wir nutzen den Proxy-Endpunkt, um das SVG-Badge abzurufen und CORS-Probleme zu vermeiden.
    fetch('/api/status')
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok');
        }
        return response.text();
      })
      .then(svgText => {
        // Wir prüfen den Textinhalt des SVGs, um den Status zu bestimmen.
        // Uptime Kuma verwendet "UP" in Großbuchstaben im Badge.
        if (svgText.includes('>Up</text>')) {
          setStatus('online');
        } else if (svgText.includes('>Pending</text>')) {
          setStatus('pending');
        } else {
          setStatus('offline');
        }
      })
      .catch(() => {
        setStatus('offline');
      });
  }, []);

  if (status === 'loading') {
    return (
      <div className="flex items-center space-x-2 px-3 py-1 bg-gray-500/10 border border-gray-500/20 rounded-full">
        <div className="w-2 h-2 bg-gray-500 rounded-full"></div>
        <span className="text-xs font-medium text-gray-400 uppercase tracking-wider">Lade Status...</span>
      </div>
    );
  }

  if (status === 'pending') {
    return (
      <a href="https://kuma.codeheap.dev/status/codeheap" target="_blank" rel="noopener noreferrer" className="flex items-center space-x-2 px-3 py-1 bg-amber-500/10 border border-amber-500/20 rounded-full">
        <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse"></div>
        <span className="text-xs font-medium text-amber-400 uppercase tracking-wider">Status: Pending</span>
      </a>
    );
  }

  if (status === 'offline') {
    return (
      <a href="https://kuma.codeheap.dev/status/codeheap" target="_blank" rel="noopener noreferrer" className="flex items-center space-x-2 px-3 py-1 bg-red-500/10 border border-red-500/20 rounded-full">
        <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
        <span className="text-xs font-medium text-red-400 uppercase tracking-wider">Wartung</span>
      </a>
    );
  }

  return (
    <a href="https://kuma.codeheap.dev/status/codeheap" target="_blank" rel="noopener noreferrer" className="flex items-center space-x-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 rounded-full">
      <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></div>
      <span className="text-xs font-medium text-emerald-400 uppercase tracking-wider">Systeme Online</span>
    </a>
  );
};

export default StatusIndicator;