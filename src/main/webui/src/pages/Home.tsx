import { useState, useEffect, useCallback } from 'react';
import {
  ChevronRight,
  ExternalLink,
  Server,
  Shield,
  Fuel, // Import Fuel icon for fuel prices
  X // Import X icon for close button
} from 'lucide-react';
import { motion, AnimatePresence, Variants }
from 'framer-motion';
import { SERVICES, Service } from '../data/services'; // Import Service interface
import StatusIndicator from '../components/StatusIndicator';
import ProxmoxDashboard from "../components/ProxmoxDashboard";
import PiHoleDashboard from "../components/PiHoleDashboard";
import FuelPriceDashboard from "../components/FuelPriceDashboard"; // Import FuelPriceDashboard

const Home = () => {
  const [showServiceModal, setShowServiceModal] = useState(false);
  const [selectedService, setSelectedService] = useState<Service | null>(null);

  const handleMouseMove = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    const { currentTarget } = e;
    const rect = currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    currentTarget.style.setProperty('--x', `${x}px`);
    currentTarget.style.setProperty('--y', `${y}px`);
  }, []);

  const heroContent = [
    {
      headlinePrimary: "Deine Dienste, vereint",
      headlineGradient: "an einem Ort.",
      description: "Dezentral, sicher und vollständig selbstgehostet auf deiner Infrastruktur. Wähle einen Dienst aus, um fortzufahren."
    },
    {
      headlinePrimary: "Volle Kontrolle,",
      headlineGradient: "einfache Verwaltung.",
      description: "Behalte den Überblick über all deine selbstgehosteten Dienste. Ein Dashboard, alle Informationen auf einen Blick."
    },
    {
      headlinePrimary: "Sicher. Dezentral.",
      headlineGradient: "Dein Zuhause.",
      description: "Erlebe die Freiheit der Selbstverwaltung mit maximaler Sicherheit und Privatsphäre, direkt auf deiner Infrastruktur."
    }
  ];

  const [currentTextIndex, setCurrentTextIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTextIndex((prevIndex) => (prevIndex + 1) % heroContent.length);
    }, 8000);

    return () => clearInterval(interval);
  }, [heroContent.length]);

  // New animation variants for hero section text
  const containerVariants: Variants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.05,
      },
    },
    exit: {
      opacity: 0,
      transition: {
        duration: 0.3,
      },
    },
  };

  const itemVariants: Variants = {
    hidden: { y: 30, opacity: 0, rotateX: -45, scale: 0.9 },
    visible: {
      y: 0,
      opacity: 1,
      rotateX: 0,
      scale: 1,
      transition: {
        type: "spring",
        damping: 15,
        stiffness: 100,
        mass: 1,
      },
    },
    exit: {
      y: -30,
      opacity: 0,
      rotateX: 45,
      scale: 0.9,
      transition: {
        duration: 0.25,
        ease: [0.0, 0.0, 0.2, 1],
      },
    },
  };

  const descriptionVariants: Variants = {
    hidden: { y: 20, opacity: 0, scale: 0.98 },
    visible: {
      y: 0,
      opacity: 1,
      scale: 1,
      transition: {
        delay: 0.2,
        type: "spring",
        damping: 15,
        stiffness: 100,
      },
    },
    exit: {
      y: -20,
      opacity: 0,
      scale: 0.98,
      transition: {
        duration: 0.25,
        ease: [0.0, 0.0, 0.2, 1],
      },
    },
  };

  // Variants for cards
  const cardVariants: Variants = {
    hidden: { opacity: 0, y: 50 },
    visible: {
      opacity: 1,
      y: 0,
      transition: {
        duration: 0.6,
        ease: [0.0, 0.0, 0.2, 1],
      },
    },
  };

  const currentHeadlinePrimary = heroContent[currentTextIndex].headlinePrimary;
  const currentHeadlineGradient = heroContent[currentTextIndex].headlineGradient;
  const currentDescription = heroContent[currentTextIndex].description;

  const wordsForPrimaryHeadline = currentHeadlinePrimary.split(" ");

  const openServiceModal = (service: Service) => {
    setSelectedService(service);
    setShowServiceModal(true);
  };

  const closeServiceModal = () => {
    setShowServiceModal(false);
    setSelectedService(null);
  };

  return (
    <>
      {/* Hero Bereich */}
      <header className="relative z-10 pt-16 pb-12 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <AnimatePresence mode="wait">
            <motion.h1
              key={currentTextIndex + "-h1"}
              className="text-4xl md:text-6xl font-extrabold text-white mb-6"
              variants={containerVariants}
              initial="hidden"
              animate="visible"
              exit="exit"
            >
              {wordsForPrimaryHeadline.map((word, index) => (
                <motion.span
                  key={index}
                  variants={itemVariants}
                  className="inline-block mr-2"
                >
                  {word}
                </motion.span>
              ))}
              {currentHeadlineGradient && (
                <>
                  <br className="hidden md:block" />
                  <motion.span
                    className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-purple-400 inline-block"
                    variants={itemVariants}
                  >
                    {currentHeadlineGradient}
                  </motion.span>
                </>
              )}
            </motion.h1>
          </AnimatePresence>
          <AnimatePresence mode="wait">
            <motion.p
              key={currentTextIndex + "-p"}
              className="text-slate-400 max-w-2xl mx-auto text-lg mb-10"
              variants={descriptionVariants}
              initial="hidden"
              animate="visible"
              exit="exit"
            >
              {currentDescription}
            </motion.p>
          </AnimatePresence>
        </div>
      </header>

      {/* Service Grid */}
      <main className="relative z-10 max-w-7xl mx-auto px-6 pb-24">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {SERVICES.map((service, index) => (
            <motion.div
              key={service.id}
              className={`group relative p-8 rounded-3xl bg-slate-900/40 border border-white/5 backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-slate-800/60 shadow-xl overflow-hidden ${service.glow} cursor-pointer`}
              onMouseMove={handleMouseMove}
              onClick={() => openServiceModal(service)}
              variants={cardVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, amount: 0.3 }}
              transition={{ delay: index * 0.1 }}
            >
              {/* Neuer Glüheffekt-Div */}
              <div className={`card-hover-glow absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 ${service.glowColor}`}></div>

              {/* Dekorative Leiterbahnen-Stil-Elemente */}
              <div className={`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-white/5 to-transparent rounded-tr-3xl pointer-events-none`}></div>

              {/* Status Indicator (wenn monitorId vorhanden) */}
              {service.monitorId && (
                <div className="absolute top-4 right-4 z-20">
                  <StatusIndicator monitorId={service.monitorId} className="!bg-slate-900/80 backdrop-blur-md !px-2 !py-0.5 text-[10px]" />
                </div>
              )}

              <div className="relative z-10">
                <div className={`w-14 h-14 ${service.bg} border ${service.color} rounded-2xl flex items-center justify-center mb-6 transition-transform duration-500 group-hover:scale-110 group-hover:rotate-3 shadow-lg shadow-black/50`}>
                  <service.icon className={`w-7 h-7 ${service.text}`} />
                </div>

                <h3
                  className="text-xl font-bold text-white mb-2 flex items-center"
                >
                  {service.name}
                  <ChevronRight className="w-4 h-4 ml-1 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all text-blue-400" />
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  {service.shortDescription} {/* Display shortDescription */}
                </p>

                {/* Direct link to service */}
                <a
                  href={service.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={(e) => e.stopPropagation()} // Prevent modal from opening when clicking this link
                  className="inline-flex items-center px-3 py-1 rounded-full border border-slate-700 bg-slate-800/50 text-xs font-semibold uppercase tracking-widest text-slate-400 group-hover:text-blue-400 group-hover:border-blue-400 transition-all relative z-20" // Neuer Stil
                  aria-label={`Starte ${service.name} direkt`}
                >
                  Dienst Starten <ExternalLink className="w-3 h-3 ml-2" />
                </a>
              </div>

              {/* Unterer Glow-Streifen */}
              <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-gradient-to-r from-transparent via-current to-transparent opacity-0 group-hover:opacity-100 transition-opacity ${service.text} pointer-events-none`}></div>
            </motion.div>
          ))}
        </div>

        {/* Neuer Bereich für System-Status */}
        <div>
          <div className="mt-16 text-center">
            <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
              System-Status <br className="hidden md:block" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-green-400 via-teal-400 to-cyan-400">
                Übersicht
              </span>
            </h2>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
            {/* Proxmox Dashboard Card */}
            <motion.div
              className={`group relative p-8 rounded-3xl bg-slate-900/40 border border-white/5 backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-slate-800/60 shadow-xl overflow-hidden`}
              onMouseMove={handleMouseMove}
              variants={cardVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, amount: 0.3 }}
              transition={{ delay: 0.1 }}
            >
              {/* Neuer Glüheffekt-Div */}
              <div className="card-hover-glow absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-blue-400/20"></div>

              {/* Link Wrapper for Proxmox */}
              <a href="/server-status" className="absolute inset-0 z-0" aria-label="Gehe zu Server Status"></a>

              <div className="relative z-10 pointer-events-none">
                <div className="w-14 h-14 bg-blue-600/20 border border-blue-500 rounded-2xl flex items-center justify-center mb-6 transition-transform duration-500 group-hover:scale-110 group-hover:rotate-3 shadow-lg shadow-black/50">
                  <Server className="w-7 h-7 text-blue-400" />
                </div>
                <h3 className="text-xl font-bold text-white mb-2 flex items-center">
                  Proxmox Server Status
                  <ChevronRight className="w-4 h-4 ml-1 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all text-blue-400" />
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  Detaillierte Informationen zu deinen Proxmox-Instanzen.
                </p>
                <ProxmoxDashboard />
              </div>
              {/* Unterer Glow-Streifen */}
              <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-gradient-to-r from-transparent via-current to-transparent opacity-0 group-hover:opacity-100 transition-opacity text-blue-400 pointer-events-none`}></div>
            </motion.div>

            {/* Pi-Hole Dashboard Card */}
            <motion.div
              className={`group relative p-8 rounded-3xl bg-slate-900/40 border border-white/5 backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-slate-800/60 shadow-xl overflow-hidden`}
              onMouseMove={handleMouseMove}
              variants={cardVariants}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, amount: 0.3 }}
              transition={{ delay: 0.2 }}
            >
              {/* Neuer Glüheffekt-Div */}
              <div className="card-hover-glow absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-green-400/20"></div>

              {/* Link Wrapper for Pi-Hole */}
              <a href="/server-status" className="absolute inset-0 z-0" aria-label="Gehe zu Server Status"></a>

              <div className="relative z-10 pointer-events-none">
                <div className="w-14 h-14 bg-green-600/20 border border-green-500 rounded-2xl flex items-center justify-center mb-6 transition-transform duration-500 group-hover:scale-110 group-hover:rotate-3 shadow-lg shadow-black/50">
                  <Shield className="w-7 h-7 text-green-400" />
                </div>
                <h3 className="text-xl font-bold text-white mb-2 flex items-center">
                  Pi-Hole Status
                  <ChevronRight className="w-4 h-4 ml-1 opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all text-blue-400" />
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  Überblick über deine Netzwerk-Werbeblockierung.
                </p>
                <PiHoleDashboard />
              </div>
              {/* Unterer Glow-Streifen */}
              <div className={`absolute bottom-0 left-8 right-8 h-[2px] bg-gradient-to-r from-transparent via-current to-transparent opacity-0 group-hover:opacity-100 transition-opacity text-green-400 pointer-events-none`}></div>
            </motion.div>
          </div>
        </div>

        {/* Neuer Bereich für Tankstellenpreise */}
        <div>
          <div className="mt-16 text-center">
            <h2 className="text-3xl md:text-4xl font-extrabold text-white mb-8">
              Aktuelle Tankstellenpreise <br className="hidden md:block" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-yellow-400 via-orange-400 to-red-400">
                Übersicht
              </span>
            </h2>
          </div>
          <div className="mt-8">
            <FuelPriceDashboard />
          </div>
        </div>
      </main>

      {/* Service Detail Modal */}
      <AnimatePresence>
        {showServiceModal && selectedService && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
            onClick={closeServiceModal} // Close modal when clicking outside
          >
            <motion.div
              initial={{ y: 50, opacity: 0, scale: 0.9 }}
              animate={{ y: 0, opacity: 1, scale: 1 }}
              exit={{ y: 50, opacity: 0, scale: 0.9 }}
              transition={{ type: "spring", damping: 20, stiffness: 300 }}
              className="relative w-full max-w-lg p-8 rounded-3xl bg-slate-900/90 border border-white/10 shadow-2xl"
              onClick={(e) => e.stopPropagation()} // Prevent modal from closing when clicking inside
            >
              <button
                onClick={closeServiceModal}
                className="absolute top-4 right-4 text-slate-400 hover:text-white transition-colors"
                aria-label="Modalfenster schließen"
              >
                <X size={24} />
              </button>

              <div className="flex items-center mb-6">
                <div className={`w-16 h-16 ${selectedService.bg} border ${selectedService.color} rounded-2xl flex items-center justify-center mr-4 shadow-lg`}>
                  <selectedService.icon className={`w-8 h-8 ${selectedService.text}`} />
                </div>
                <div>
                  <h3 className="text-3xl font-bold text-white">{selectedService.name}</h3>
                  {selectedService.monitorId && (
                    <StatusIndicator monitorId={selectedService.monitorId} className="mt-2 !bg-slate-800/70 backdrop-blur-md !px-3 !py-1 text-sm" />
                  )}
                </div>
              </div>

              <p className="text-slate-300 text-base leading-relaxed mb-6">
                {selectedService.longDescription}
              </p>

              <a
                href={selectedService.url}
                target="_blank"
                rel="noopener noreferrer"
                className={`inline-flex items-center px-6 py-3 rounded-full font-semibold text-white transition-all duration-300
                  ${selectedService.bg.replace('/10', '/30')} border ${selectedService.color}
                  hover:bg-opacity-80 hover:shadow-lg hover:shadow-current/20`}
                style={{ borderColor: selectedService.color.replace('border-', '') }} // Dynamic border color
              >
                Dienst Starten
                <ExternalLink className="w-4 h-4 ml-2" />
              </a>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default Home;
