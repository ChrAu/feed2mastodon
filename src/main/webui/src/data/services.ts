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
  description: string;
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
    description: 'Login-Dienst (IAM)',
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
    description: 'Daten & Kollaboration',
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
    description: 'Sicherer Mailserver',
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
    description: 'Passwort-Manager',
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
    description: 'Hörbücher & Podcasts',
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
    description: 'Rezeptverwaltung',
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
    description: 'Dokumenten-Management',
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
    description: 'Dateikonvertierung',
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
    description: 'Standortverlauf-Tracking',
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
