export interface PiHoleEntity {
    entity_id: string;
    state: string;
    attributes: {
        friendly_name: string;
        unit_of_measurement?: string;
    };
}

export const subscribeToPiHole = (onMessage: (data: PiHoleEntity[]) => void) => {
    let eventSource: EventSource | null = null;
    let reconnectTimeout: number | null = null;
    const reconnectInterval = 5000; // 5 seconds

    const connect = () => {
        if (eventSource) {
            eventSource.close();
        }

        eventSource = new EventSource('/api/homeassistant/pihole/stream');

        eventSource.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                onMessage(data);
                // Clear any pending reconnection attempts on successful message
                if (reconnectTimeout) {
                    clearTimeout(reconnectTimeout);
                    reconnectTimeout = null;
                }
            } catch (error) {
                console.error("Fehler beim Parsen der Pi-Hole Daten:", error);
            }
        };

        eventSource.onerror = (err) => {
            console.error("EventSource für Pi-Hole fehlgeschlagen:", err);
            if (eventSource) {
                eventSource.close();
            }
            // Attempt to reconnect after a delay
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
            }
            reconnectTimeout = setTimeout(() => {
                console.log("Attempting to reconnect to Pi-Hole stream...");
                connect();
            }, reconnectInterval);
        };
    };

    connect(); // Initial connection

    // Cleanup function
    return () => {
        if (eventSource) {
            eventSource.close();
        }
        if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
        }
    };
};
