import {
  Shield,
  Cloud,
  Mail,
  BookOpen,
  FileText,
  Lock,
  RefreshCw,
  Utensils,
  MapPin
} from 'lucide-react';

export interface Service {
  id: string;
  name: string;
  shortDescription: string; // Renamed from description
  longDescription: string; // Added for detailed description
  icon: React.ElementType;
  color: string;
  glow: string;
  bg: string;
  text: string;
  url: string;
  monitorId?: number | string;
  glowColor: string;
}

/**
 * Konfiguration der Dienste basierend auf dem Bild und der Anfrage.
 * Jede Farbe entspricht dem visuellen Stil des Dienstes im Bild.
 */
export const SERVICES: Service[] = [
  {
    id: 'keycloak',
    name: 'Keycloak',
    shortDescription: 'Login-Dienst (IAM)',
    longDescription: 'Keycloak ist ein Open-Source Identity and Access Management (IAM) System, das Single Sign-On (SSO) für Webanwendungen und APIs bietet. Es unterstützt Standardprotokolle wie OpenID Connect, OAuth 2.0 und SAML 2.0 und ermöglicht eine zentrale Benutzerverwaltung sowie erweiterte Sicherheitsfunktionen.',
    icon: Shield,
    color: 'border-purple-500',
    glow: 'group-hover:shadow-purple-500/50',
    bg: 'bg-purple-500/10',
    text: 'text-purple-400',
    url: 'https://sso.codeheap.dev',
    monitorId: 21,
    glowColor: 'bg-purple-400/20'
  },
  {
    id: 'nextcloud',
    name: 'Nextcloud',
    shortDescription: 'Daten & Kollaboration',
    longDescription: 'Nextcloud ist eine selbstgehostete Produktivitätsplattform, die Dateisynchronisation und -freigabe, Online-Dokumentenbearbeitung, Kalender, Kontakte und Videoanrufe in einer sicheren Umgebung vereint. Es bietet eine Alternative zu kommerziellen Cloud-Diensten mit voller Kontrolle über Ihre Daten.',
    icon: Cloud,
    color: 'border-blue-500',
    glow: 'group-hover:shadow-blue-500/50',
    bg: 'bg-blue-500/10',
    text: 'text-blue-400',
    url: 'https://nextcloud.codeheap.dev',
    monitorId: 30,
    glowColor: 'bg-blue-400/20'
  },
  {
    id: 'mailcow',
    name: 'Mailcow',
    shortDescription: 'Sicherer Mailserver',
    longDescription: 'Mailcow ist eine modulare E-Mail-Server-Suite, die auf Docker basiert. Sie bietet eine vollständige E-Mail-Lösung mit Webmail, Antispam, Antivirus, Kalender- und Kontaktsynchronisation sowie einfacher Verwaltung über eine intuitive Weboberfläche. Fokus liegt auf Sicherheit und einfacher Bereitstellung.',
    icon: Mail,
    color: 'border-cyan-500',
    glow: 'group-hover:shadow-cyan-500/50',
    bg: 'bg-cyan-500/10',
    text: 'text-cyan-400',
    url: 'https://mail.codeheap.dev',
    monitorId: 26,
    glowColor: 'bg-cyan-400/20'
  },
  {
    id: 'vaultwarden',
    name: 'Vaultwarden',
    shortDescription: 'Passwort-Manager',
    longDescription: 'Vaultwarden ist eine alternative Implementierung des Bitwarden-Servers, geschrieben in Rust. Es bietet eine sichere und selbstgehostete Lösung zur Verwaltung Ihrer Passwörter, Notizen, Kreditkarten und Identitäten. Kompatibel mit allen Bitwarden-Clients für Desktop, Browser und Mobilgeräte.',
    icon: Lock,
    color: 'border-emerald-500',
    glow: 'group-hover:shadow-emerald-500/50',
    bg: 'bg-emerald-500/10',
    text: 'text-emerald-400',
    url: 'https://vaultwarden.codeheap.dev',
    monitorId: 23,
    glowColor: 'bg-emerald-400/20'
  },
  {
    id: 'audiobookshelf',
    name: 'Audiobookshelf',
    shortDescription: 'Hörbücher & Podcasts',
    longDescription: 'Audiobookshelf ist ein selbstgehosteter Hörbuch- und Podcast-Server. Er ermöglicht es Ihnen, Ihre persönliche Sammlung zu organisieren, zu streamen und auf verschiedenen Geräten zu hören. Mit Funktionen wie Fortschrittssynchronisation, Multi-User-Unterstützung und Transkodierung.',
    icon: BookOpen,
    color: 'border-red-500',
    glow: 'group-hover:shadow-red-500/50',
    bg: 'bg-red-500/10',
    text: 'text-red-400',
    url: 'https://audiobookshelf.codeheap.dev',
    monitorId: 32,
    glowColor: 'bg-red-400/20'
  },
  {
    id: 'mealie',
    name: 'Mealie',
    shortDescription: 'Rezeptverwaltung',
    longDescription: 'Mealie ist eine selbstgehostete Rezeptverwaltungsplattform. Sie können Rezepte importieren, organisieren, Essenspläne erstellen und Einkaufslisten generieren. Eine moderne und benutzerfreundliche Oberfläche macht die Verwaltung Ihrer kulinarischen Kreationen zum Vergnügen.',
    icon: Utensils,
    color: 'border-yellow-500',
    glow: 'group-hover:shadow-yellow-500/50',
    bg: 'bg-yellow-500/10',
    text: 'text-yellow-400',
    url: 'https://mealie.codeheap.dev',
    monitorId: 66,
    glowColor: 'bg-yellow-400/20'
  },
  {
    id: 'paperless',
    name: 'Paperless-ngx',
    shortDescription: 'Dokumenten-Management',
    longDescription: 'Paperless-ngx ist ein Open-Source-Dokumentenmanagementsystem, das Ihre physischen Dokumente in eine durchsuchbare digitale Bibliothek verwandelt. Es automatisiert die Verarbeitung von Dokumenten, extrahiert Informationen und ermöglicht eine einfache Archivierung und Wiederauffindung.',
    icon: FileText,
    color: 'border-orange-500',
    glow: 'group-hover:shadow-orange-500/50',
    bg: 'bg-orange-500/10',
    text: 'text-orange-400',
    url: 'https://paperless.codeheap.dev',
    monitorId: 34,
    glowColor: 'bg-orange-400/20'
  },
  {
    id: 'convertx',
    name: 'ConvertX',
    shortDescription: 'Dateikonvertierung',
    longDescription: 'ConvertX ist ein vielseitiger Online-Dienst zur Dateikonvertierung. Er unterstützt eine breite Palette von Formaten für Bilder, Dokumente, Audio und Video. Einfach hochladen, Format wählen und die konvertierte Datei herunterladen – schnell und unkompliziert.',
    icon: RefreshCw,
    color: 'border-pink-500',
    glow: 'group-hover:shadow-pink-500/50',
    bg: 'bg-pink-500/10',
    text: 'text-pink-400',
    url: 'https://converter.codeheap.dev/',
    monitorId: 11,
    glowColor: 'bg-pink-400/20'
  },
  {
    id: 'dawarich',
    name: 'Dawarich',
    shortDescription: 'Standortverlauf-Tracking',
    longDescription: 'Dawarich ist ein selbstgehosteter Dienst zur Aufzeichnung und Visualisierung Ihres Standortverlaufs. Er ermöglicht es Ihnen, Ihre Bewegungen privat zu verfolgen, Routen zu analysieren und interessante Orte zu markieren. Ideal für persönliche Reisetagebücher mit Fokus auf Datenschutz.',
    icon: MapPin,
    color: 'border-green-500',
    glow: 'group-hover:shadow-green-500/50',
    bg: 'bg-green-500/10',
    text: 'text-green-400',
    url: 'https://dawarich.codeheap.dev',
    glowColor: 'bg-green-400/20',
    monitorId: '28'
  }
];
