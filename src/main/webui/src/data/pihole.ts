export interface PiHoleEntity {
    entity_id: string;
    state: string;
    attributes: {
        friendly_name: string;
        unit_of_measurement?: string;
    };
}

export const subscribeToPiHole = (onMessage: (data: PiHoleEntity[]) => void) => {
    // Nutzt den neuen SSE Endpoint
    const eventSource = new EventSource('/api/homeassistant/pihole/stream');

    eventSource.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            onMessage(data);
        } catch (error) {
            console.error("Fehler beim Parsen der Pi-Hole Daten:", error);
        }
    };

    eventSource.onerror = (err) => {
        console.error("EventSource fehlgeschlagen:", err);
        eventSource.close();
    };

    return () => eventSource.close(); // Cleanup-Funktion
};