import { 
  Shield, 
  Cloud, 
  Mail, 
  BookOpen, 
  Edit3, 
  FileText, 
  Lock
} from 'lucide-react';

/**
 * Konfiguration der Dienste basierend auf dem Bild und der Anfrage.
 * Jede Farbe entspricht dem visuellen Stil des Dienstes im Bild.
 */
export const SERVICES = [
  {
    id: 'keycloak',
    name: 'Keycloak',
    description: 'Login-Dienst (IAM)',
    icon: Shield,
    color: 'border-purple-500',
    glow: 'group-hover:shadow-purple-500/50',
    bg: 'bg-purple-500/10',
    text: 'text-purple-400',
    url: 'https://sso.codeheap.dev'
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
    url: 'https://nextcloud.codeheap.dev'
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
    url: 'https://mail.codeheap.dev'
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
    url: 'https://vaultwarden.codeheap.dev'
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
    url: 'https://audiobookshelf.codeheap.dev'
  },
  {
    id: 'blinko',
    name: 'Blinko',
    description: 'Schnelle Notizen',
    icon: Edit3,
    color: 'border-amber-500',
    glow: 'group-hover:shadow-amber-500/50',
    bg: 'bg-amber-500/10',
    text: 'text-amber-400',
    url: 'https://blinko.codeheap.dev'
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
    url: 'https://paperless.codeheap.dev'
  }
];