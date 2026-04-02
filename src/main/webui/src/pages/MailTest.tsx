import { useEffect, useState } from 'react';

type MailProviderStats = {
    provider: string;
    lastSent: string | null;
    lastSentStatus: string;
    lastMailReceptionStatus: string;
    failuresLast7Days: number;
    lastSuccessfulLogin: string | null;
}

const MailTest = () => {
    const [mailStats, setMailStats] = useState<MailProviderStats[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        document.title = "Mail Test - codeheap.dev";
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
            <div className="container mx-auto px-6 max-w-7xl pb-24 text-slate-300">
                <div className="mt-16 text-center">
                    <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                        E-Mail Test <br className="hidden md:block" />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400">
                            und Statistiken
                        </span>
                    </h2>
                </div>
                <p className="mb-8 text-center max-w-3xl mx-auto text-lg">
                    Diese Seite dient der Auswertung der E-Mail-Zustellbarkeit. Regelmäßig werden Test-E-Mails vom Server an verschiedene Anbieter gesendet,
                    um zu prüfen, ob diese erfolgreich zugestellt werden und wie gewünscht im Posteingang erscheinen.
                    Dies ist wichtig, da manche Mail-Provider E-Mails von Mailservern plötzlich als Spam markieren können.
                    Hier können Sie den Status der letzten gesendeten E-Mails, den Empfangsstatus und die Anzahl der Fehler in den letzten 7 Tagen einsehen.
                </p>
                <h3 className="text-2xl font-bold text-white mb-4">E-Mail Anbieter Statistiken</h3>
                <div className="overflow-x-auto mt-4">
                    <table className="min-w-full bg-slate-800/50 border border-slate-700 rounded-xl shadow-lg animate-pulse backdrop-blur-sm">
                        <thead>
                            <tr className="bg-slate-900/50">
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Anbieter</th>
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Zuletzt gesendet</th>
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Sendestatus</th>
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Empfangsstatus</th>
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Fehler (letzte 7 Tage)</th>
                                <th className="py-3 px-4 border-b border-slate-700 text-left text-slate-300 font-semibold">Letzter Login</th>
                            </tr>
                        </thead>
                        <tbody>
                            {[...Array(3)].map((_, index) => ( // Placeholder for 3 rows
                                <tr key={index} className="hover:bg-slate-800/70 transition-colors">
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-3/4"></div>
                                    </td>
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-full"></div>
                                    </td>
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-1/2"></div>
                                    </td>
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-2/3"></div>
                                    </td>
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-1/4"></div>
                                    </td>
                                    <td className="py-3 px-4 border-b border-slate-700/50">
                                        <div className="h-4 bg-slate-700 rounded w-1/2"></div>
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
            <div className="container mx-auto px-6 max-w-7xl pb-24 text-slate-300">
                <div className="mt-16 text-center">
                    <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                        E-Mail Test <br className="hidden md:block" />
                        <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400">
                            und Statistiken
                        </span>
                    </h2>
                </div>
                <div className="bg-red-900/20 border border-red-500/50 rounded-xl p-4 text-red-400 text-center">
                    Fehler beim Laden der E-Mail Statistiken: {error}
                </div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 max-w-7xl pb-24 text-slate-300">
            <div className="mt-16 text-center">
                <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
                    E-Mail Test <br className="hidden md:block" />
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400">
                        und Statistiken
                    </span>
                </h2>
            </div>

            <p className="mb-8 text-center max-w-3xl mx-auto text-lg leading-relaxed">
                Diese Seite dient der Auswertung der E-Mail-Zustellbarkeit. Regelmäßig werden Test-E-Mails vom Server an verschiedene Anbieter gesendet,
                um zu prüfen, ob diese erfolgreich zugestellt werden und wie gewünscht im Posteingang erscheinen.
                Dies ist wichtig, da manche Mail-Provider E-Mails von Mailservern plötzlich als Spam markieren können.
                Hier können Sie den Status der letzten gesendeten E-Mails, den Empfangsstatus und die Anzahl der Fehler in den letzten 7 Tagen einsehen.
            </p>

            <h3 className="text-2xl font-bold text-white mb-4 flex items-center">
                E-Mail Anbieter Statistiken
            </h3>
            {mailStats.length > 0 ? (
                <div className="overflow-x-auto rounded-xl border border-slate-700 shadow-xl bg-slate-800/30 backdrop-blur-sm">
                    <table className="min-w-full">
                        <thead>
                            <tr className="bg-slate-900/60 border-b border-slate-700">
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Anbieter</th>
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Zuletzt gesendet</th>
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Sendestatus</th>
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Empfangsstatus</th>
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Fehler (letzte 7 Tage)</th>
                                <th className="py-4 px-6 text-left text-sm font-semibold text-slate-200">Letzter Login</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700/50">
                            {mailStats.map((stats, index) => (
                                <tr key={index} className="hover:bg-slate-800/50 transition-colors">
                                    <td className="py-4 px-6 font-medium text-white">{stats.provider}</td>
                                    <td className="py-4 px-6 text-slate-400">{stats.lastSent ? new Date(stats.lastSent).toLocaleString() : 'N/A'}</td>
                                    <td className="py-4 px-6">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${stats.lastSentStatus === 'SUCCESS' ? 'bg-green-500/10 text-green-400' : 'bg-red-500/10 text-red-400'}`}>
                                            {stats.lastSentStatus}
                                        </span>
                                    </td>
                                    <td className="py-4 px-6">
                                        <div className="flex items-center">
                                            {stats.lastMailReceptionStatus === 'PENDING_CHECK' && (
                                                <span title="Empfang ausstehend" className="mr-2 text-yellow-400">⚠️</span>
                                            )}
                                            {stats.lastMailReceptionStatus === 'NOT_RECEIVED' && (
                                                <span title="Nicht empfangen (vermutlich Spam oder geblockt)" className="mr-2 text-red-400">❌</span>
                                            )}
                                            {stats.lastMailReceptionStatus === 'RECEIVED' && (
                                                <span title="Erfolgreich empfangen" className="mr-2 text-green-400">✓</span>
                                            )}
                                            <span className={
                                                stats.lastMailReceptionStatus === 'RECEIVED' ? 'text-green-400' :
                                                stats.lastMailReceptionStatus === 'NOT_RECEIVED' ? 'text-red-400' :
                                                stats.lastMailReceptionStatus === 'PENDING_CHECK' ? 'text-yellow-400' : 'text-slate-300'
                                            }>
                                                {stats.lastMailReceptionStatus}
                                            </span>
                                        </div>
                                    </td>
                                    <td className="py-4 px-6">
                                        <div className="flex items-center">
                                            {stats.failuresLast7Days > 0 && (
                                                <span title={`${stats.failuresLast7Days} Fehler in den letzten 7 Tagen`} className="mr-2 text-red-400">⚠️</span>
                                            )}
                                            <span className={stats.failuresLast7Days > 0 ? 'text-red-400 font-bold' : 'text-slate-400'}>
                                                {stats.failuresLast7Days}
                                            </span>
                                        </div>
                                    </td>
                                    <td className="py-4 px-6 text-slate-400">{stats.lastSuccessfulLogin ? new Date(stats.lastSuccessfulLogin).toLocaleString() : 'N/A'}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="bg-slate-800/30 border border-slate-700 rounded-xl p-8 text-center text-slate-400">
                    Keine E-Mail Anbieter Statistiken verfügbar.
                </div>
            )}

            <div className="mt-12 bg-slate-900/40 rounded-2xl p-6 border border-slate-700 shadow-lg">
                <h4 className="text-lg font-bold text-white mb-4">Erläuterung der Werte:</h4>
                <ul className="space-y-3 text-slate-300">
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2">•</span>
                        <span><strong className="text-white">Anbieter:</strong> Der Name des E-Mail-Dienstanbieters (z.B. hexix.de, gmx.de).</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2">•</span>
                        <span><strong className="text-white">Zuletzt gesendet:</strong> Der Zeitstempel, wann die letzte E-Mail über diesen Anbieter gesendet wurde.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2">•</span>
                        <span><strong className="text-white">Sendestatus:</strong> Der Status des letzten Sendeversuchs (z.B. <span className="text-green-400">SUCCESS</span>, <span className="text-red-400">FAILED</span>).</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2 mt-1">•</span>
                        <div>
                            <strong className="text-white">Empfangsstatus:</strong> Der Status des Empfangs der zuletzt gesendeten E-Mail.
                            <ul className="mt-2 space-y-1 ml-4 border-l-2 border-slate-700 pl-4">
                                <li><code className="bg-slate-800 px-1 py-0.5 rounded text-green-400 text-sm">RECEIVED</code>: Die E-Mail wurde erfolgreich empfangen.</li>
                                <li><code className="bg-slate-800 px-1 py-0.5 rounded text-yellow-400 text-sm">PENDING_CHECK</code>: Der Empfang der E-Mail wird noch überprüft.</li>
                                <li><code className="bg-slate-800 px-1 py-0.5 rounded text-red-400 text-sm">NOT_RECEIVED</code>: Die E-Mail wurde nach Abschluss der Überprüfung nicht empfangen. Dies bedeutet vermutlich, dass sie als Spam markiert oder vom Anbieter geblockt wurde.</li>
                            </ul>
                        </div>
                    </li>
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2">•</span>
                        <span><strong className="text-white">Fehler (letzte 7 Tage):</strong> Die Anzahl der fehlgeschlagenen E-Mail-Sendeversuche in den letzten 7 Tagen.</span>
                    </li>
                    <li className="flex items-start">
                        <span className="text-blue-400 mr-2">•</span>
                        <span><strong className="text-white">Letzter Login:</strong> Der Zeitstempel des letzten erfolgreichen Logins beim Mail-Account dieses Anbieters, um E-Mails abzurufen.</span>
                    </li>
                </ul>
            </div>
        </div>
    );
};

export default MailTest;
