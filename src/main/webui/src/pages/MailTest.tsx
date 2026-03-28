import React, { useEffect, useState } from 'react';

interface MailProviderStats {
    provider: string;
    lastSent: string | null;
    lastSentStatus: string;
    lastMailReceptionStatus: string;
    failuresLast7Days: number;
}

const MailTest: React.FC = () => {
    const [mailStats, setMailStats] = useState<MailProviderStats[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchMailStats = async () => {
            try {
                const response = await fetch('/api/mail-test/stats');
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const data: MailProviderStats[] = await response.json();
                setMailStats(data);
            } catch (e: any) {
                setError(e.message);
            } finally {
                setLoading(false);
            }
        };

        fetchMailStats();
    }, []);

    if (loading) {
        return (
            <div className="container mx-auto p-4 text-gray-800 dark:text-gray-200">
                <h1 className="text-xl font-bold mb-4">E-Mail Test und Statistiken</h1>
                <p className="mb-6">
                    Diese Seite dient der Auswertung der E-Mail-Zustellbarkeit. Regelmäßig werden Test-E-Mails vom Server an verschiedene Anbieter gesendet,
                    um zu prüfen, ob diese erfolgreich zugestellt werden und wie gewünscht im Posteingang erscheinen.
                    Dies ist wichtig, da manche Mail-Provider E-Mails von Mailservern plötzlich als Spam markieren können.
                    Hier können Sie den Status der letzten gesendeten E-Mails, den Empfangsstatus und die Anzahl der Fehler in den letzten 7 Tagen einsehen.
                </p>
                <h2 className="text-lg font-semibold mb-2">E-Mail Anbieter Statistiken</h2>
                <p>Lade E-Mail Statistiken...</p>
                <div className="overflow-x-auto mt-4">
                    <table className="min-w-full bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-md animate-pulse">
                        <thead>
                            <tr className="bg-gray-100 dark:bg-gray-800">
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Anbieter</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Zuletzt gesendet</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Sendestatus</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Empfangsstatus</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Fehler (letzte 7 Tage)</th>
                            </tr>
                        </thead>
                        <tbody>
                            {[...Array(3)].map((_, index) => ( // Placeholder for 3 rows
                                <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-600">
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        <div className="h-4 bg-gray-300 dark:bg-gray-600 rounded w-3/4"></div>
                                    </td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        <div className="h-4 bg-gray-300 dark:bg-gray-600 rounded w-full"></div>
                                    </td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        <div className="h-4 bg-gray-300 dark:bg-gray-600 rounded w-1/2"></div>
                                    </td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        <div className="h-4 bg-gray-300 dark:bg-gray-600 rounded w-2/3"></div>
                                    </td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        <div className="h-4 bg-gray-300 dark:bg-gray-600 rounded w-1/4"></div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="container mx-auto p-4 text-gray-800 dark:text-gray-200">
                <h1 className="text-xl font-bold mb-4">E-Mail Test und Statistiken</h1>
                <p className="text-red-500">Fehler beim Laden der E-Mail Statistiken: {error}</p>
            </div>
        );
    }

    return (
        <div className="container mx-auto p-4 text-gray-800 dark:text-gray-200">
            <h1 className="text-xl font-bold mb-4">E-Mail Test und Statistiken</h1>

            <p className="mb-6">
                Diese Seite dient der Auswertung der E-Mail-Zustellbarkeit. Regelmäßig werden Test-E-Mails vom Server an verschiedene Anbieter gesendet,
                um zu prüfen, ob diese erfolgreich zugestellt werden und wie gewünscht im Posteingang erscheinen.
                Dies ist wichtig, da manche Mail-Provider E-Mails von Mailservern plötzlich als Spam markieren können.
                Hier können Sie den Status der letzten gesendeten E-Mails, den Empfangsstatus und die Anzahl der Fehler in den letzten 7 Tagen einsehen.
            </p>

            <h2 className="text-lg font-semibold mb-2">E-Mail Anbieter Statistiken</h2>
            {mailStats.length > 0 ? (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg shadow-md">
                        <thead>
                            <tr className="bg-gray-100 dark:bg-gray-800">
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Anbieter</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Zuletzt gesendet</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Sendestatus</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Empfangsstatus</th>
                                <th className="py-2 px-4 border-b border-gray-200 dark:border-gray-600 text-left text-gray-700 dark:text-gray-300">Fehler (letzte 7 Tage)</th>
                            </tr>
                        </thead>
                        <tbody>
                            {mailStats.map((stats, index) => (
                                <tr key={index} className="hover:bg-gray-50 dark:hover:bg-gray-600">
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">{stats.provider}</td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">{stats.lastSent ? new Date(stats.lastSent).toLocaleString() : 'N/A'}</td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">{stats.lastSentStatus}</td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        {stats.lastMailReceptionStatus === 'PENDING_CHECK' && (
                                            <span title="Empfang ausstehend" className="mr-1">⚠️</span>
                                        )}
                                        {stats.lastMailReceptionStatus === 'NOT_RECEIVED' && (
                                            <span title="Nicht empfangen (vermutlich Spam oder geblockt)" className="mr-1">❌</span>
                                        )}
                                        {stats.lastMailReceptionStatus}
                                    </td>
                                    <td className="py-2 px-4 border-b border-gray-200 dark:border-gray-600">
                                        {stats.failuresLast7Days > 0 && (
                                            <span title={`${stats.failuresLast7Days} Fehler in den letzten 7 Tagen`} className="mr-1">⚠️</span>
                                        )}
                                        {stats.failuresLast7Days}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <p>Keine E-Mail Anbieter Statistiken verfügbar.</p>
            )}

            <div className="mt-8">
                <h3 className="text-md font-semibold mb-2">Erläuterung der Werte:</h3>
                <ul className="list-disc list-inside">
                    <li><strong>Anbieter:</strong> Der Name des E-Mail-Dienstanbieters (z.B. hexix.de, gmx.de).</li>
                    <li><strong>Zuletzt gesendet:</strong> Der Zeitstempel, wann die letzte E-Mail über diesen Anbieter gesendet wurde.</li>
                    <li><strong>Sendestatus:</strong> Der Status des letzten Sendeversuchs (z.B. SUCCESS, FAILED).</li>
                    <li><strong>Empfangsstatus:</strong> Der Status des Empfangs der zuletzt gesendeten E-Mail.
                        <ul className="list-disc list-inside ml-4 mt-2">
                            <li><code>RECEIVED</code>: Die E-Mail wurde erfolgreich empfangen.</li>
                            <li><code>PENDING_CHECK</code>: Der Empfang der E-Mail wird noch überprüft. Ein Warnsymbol (⚠️) wird angezeigt.</li>
                            <li><code>NOT_RECEIVED</code>: Die E-Mail wurde nach Abschluss der Überprüfung nicht empfangen. Dies bedeutet vermutlich, dass sie als Spam markiert oder vom Anbieter geblockt wurde. Ein Fehler-Symbol (❌) wird angezeigt.</li>
                            <li>Andere Status können auf Probleme beim Empfang hinweisen.</li>
                        </ul>
                    </li>
                    <li><strong>Fehler (letzte 7 Tage):</strong> Die Anzahl der fehlgeschlagenen E-Mail-Sendeversuche in den letzten 7 Tagen. Ein Warnsymbol (⚠️) wird angezeigt, wenn dieser Wert größer als 0 ist.</li>
                </ul>
            </div>
        </div>
    );
};

export default MailTest;
