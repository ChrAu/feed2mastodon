const Datenschutz = () => {
  return (
    <div className="max-w-4xl mx-auto px-6 py-12 text-slate-300">
      <h1 className="text-3xl font-bold text-white mb-8">Datenschutzerklärung</h1>

      <div className="bg-slate-900/40 border border-white/5 rounded-3xl p-8 backdrop-blur-sm space-y-8">

        {/* 1. Datenschutz auf einen Blick */}
        <section>
            <h2 className="text-xl font-semibold text-white mb-4">1. Datenschutz auf einen Blick</h2>

            <h3 className="text-lg font-medium text-white mb-2">Allgemeine Hinweise</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Die folgenden Hinweise geben einen einfachen Überblick darüber, was mit Ihren personenbezogenen Daten passiert, wenn Sie diese Website besuchen. Personenbezogene Daten sind alle Daten, mit denen Sie persönlich identifiziert werden können. Ausführliche Informationen zum Thema Datenschutz entnehmen Sie unserer unter diesem Text aufgeführten Datenschutzerklärung.
            </p>

            <h3 className="text-lg font-medium text-white mb-2">Datenerfassung auf dieser Website</h3>
            <div className="space-y-4 text-sm leading-relaxed text-slate-400 text-justify">
                <p>
                    <strong>Wer ist verantwortlich für die Datenerfassung auf dieser Website?</strong><br/>
                    Die Datenverarbeitung auf dieser Website erfolgt durch den Websitebetreiber. Dessen Kontaktdaten können Sie dem Abschnitt „Hinweis zur Verantwortlichen Stelle“ in dieser Datenschutzerklärung entnehmen.
                </p>
                <p>
                    <strong>Wie erfassen wir Ihre Daten?</strong><br/>
                    Ihre Daten werden zum einen dadurch erhoben, dass Sie uns diese mitteilen. Hierbei kann es sich z. B. um Daten handeln, die Sie in ein Kontaktformular eingeben.<br/>
                    Andere Daten werden automatisch oder nach Ihrer Einwilligung beim Besuch der Website durch unsere IT-Systeme erfasst. Das sind vor allem technische Daten (z. B. Internetbrowser, Betriebssystem oder Uhrzeit des Seitenaufrufs). Die Erfassung dieser Daten erfolgt automatisch, sobald Sie diese Website betreten.
                </p>
                <p>
                    <strong>Wofür nutzen wir Ihre Daten?</strong><br/>
                    Ein Teil der Daten wird erhoben, um eine fehlerfreie Bereitstellung der Website zu gewährleisten. Andere Daten können zur Analyse Ihres Nutzerverhaltens verwendet werden.
                </p>
            </div>
        </section>

        <hr className="border-white/10 my-8" />

        {/* 2. Allgemeine Hinweise und Pflichtinformationen */}
        <section>
            <h2 className="text-xl font-semibold text-white mb-4">2. Allgemeine Hinweise und Pflichtinformationen</h2>

            <h3 className="text-lg font-medium text-white mb-2">Datenschutz</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
               Die Betreiber dieser Seiten nehmen den Schutz Ihrer persönlichen Daten sehr ernst. Wir behandeln Ihre personenbezogenen Daten vertraulich und entsprechend den gesetzlichen Datenschutzvorschriften sowie dieser Datenschutzerklärung. Wenn Sie diese Website benutzen, werden verschiedene personenbezogene Daten erhoben. Diese Datenschutzerklärung erläutert, welche Daten wir erheben und wofür wir sie nutzen. Sie erläutert auch, wie und zu welchem Zweck das geschieht.
            </p>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Wir weisen darauf hin, dass die Datenübertragung im Internet (z. B. bei der Kommunikation per E-Mail) Sicherheitslücken aufweisen kann. Ein lückenloser Schutz der Daten vor dem Zugriff durch Dritte ist nicht möglich.
            </p>

            <h3 className="text-lg font-medium text-white mb-2">Hinweis zur verantwortlichen Stelle</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Die verantwortliche Stelle für die Datenverarbeitung auf dieser Website ist:
            </p>
            <div className="mb-6 p-4 bg-slate-800/50 rounded-xl border border-white/5">
                <p className="font-medium text-white">Christopher Auth</p>
                <p>Drackensteiner Str. 93</p>
                <p>73342 Bad Ditzenbach</p>
                <p className="mt-2">Telefon: 0155-63897322</p>
                <p>E-Mail: auth@codeheap.dev</p>
            </div>

            <h3 className="text-lg font-medium text-white mb-2">Speicherdauer</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Soweit innerhalb dieser Datenschutzerklärung keine speziellere Speicherdauer genannt wurde, verbleiben Ihre personenbezogenen Daten bei uns, bis der Zweck für die Datenverarbeitung entfällt. Wenn Sie ein berechtigtes Löschersuchen geltend machen oder eine Einwilligung zur Datenverarbeitung widerrufen, werden Ihre Daten gelöscht, sofern wir keine anderen rechtlich zulässigen Gründe für die Speicherung Ihrer personenbezogenen Daten haben (z. B. steuer- oder handelsrechtliche Aufbewahrungsfristen); im letztgenannten Fall erfolgt die Löschung nach Fortfall dieser Gründe.
            </p>
        </section>

        <hr className="border-white/10 my-8" />

        {/* 3. Datenerfassung auf dieser Website */}
        <section>
            <h2 className="text-xl font-semibold text-white mb-4">3. Datenerfassung auf dieser Website</h2>

            <h3 className="text-lg font-medium text-white mb-2">Cookies</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Unsere Internetseiten verwenden so genannte „Cookies“. Cookies sind kleine Textdateien und richten auf Ihrem Endgerät keinen Schaden an. Sie werden entweder vorübergehend für die Dauer einer Sitzung (Session-Cookies) oder dauerhaft (permanente Cookies) auf Ihrem Endgerät gespeichert. Session-Cookies werden nach Ende Ihres Besuchs automatisch gelöscht. Permanente Cookies bleiben auf Ihrem Endgerät gespeichert, bis Sie diese selbst löschen oder eine automatische Löschung durch Ihren Webbrowser erfolgt.
            </p>

            <h3 className="text-lg font-medium text-white mb-2">Kontaktformular</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Wenn Sie uns per Kontaktformular Anfragen zukommen lassen, werden Ihre Angaben aus dem Anfrageformular inklusive der von Ihnen dort angegebenen Kontaktdaten zwecks Bearbeitung der Anfrage und für den Fall von Anschlussfragen bei uns gespeichert. Diese Daten geben wir nicht ohne Ihre Einwilligung weiter.
            </p>
        </section>

        <hr className="border-white/10 my-8" />

        {/* 4. Plugins und Tools */}
        <section>
            <h2 className="text-xl font-semibold text-white mb-4">4. Plugins und Tools</h2>

            <h3 className="text-lg font-medium text-white mb-2">Google Web Fonts</h3>
            <p className="text-sm leading-relaxed text-slate-400 text-justify mb-4">
                Diese Seite nutzt zur einheitlichen Darstellung von Schriftarten so genannte Web Fonts, die von Google bereitgestellt werden. Beim Aufruf einer Seite lädt Ihr Browser die benötigten Web Fonts in ihren Browsercache, um Texte und Schriftarten korrekt anzuzeigen. Zu diesem Zweck muss der von Ihnen verwendete Browser Verbindung zu den Servern von Google aufnehmen. Hierdurch erlangt Google Kenntnis darüber, dass über Ihre IP-Adresse diese Website aufgerufen wurde. Die Nutzung von Google WebFonts erfolgt auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO. Der Websitebetreiber hat ein berechtigtes Interesse an der einheitlichen Darstellung des Schriftbildes auf seiner Website. Sofern eine entsprechende Einwilligung abgefragt wurde, erfolgt die Verarbeitung ausschließlich auf Grundlage von Art. 6 Abs. 1 lit. a DSGVO; die Einwilligung ist jederzeit widerrufbar.
            </p>
        </section>

      </div>
    </div>
  );
};

export default Datenschutz;
