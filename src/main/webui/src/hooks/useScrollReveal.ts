import { useRef, useEffect, useState } from 'react';

const useScrollReveal = (options?: IntersectionObserverInit) => {
  const [isVisible, setIsVisible] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      // Setzt isVisible auf true, sobald das Element sichtbar wird
      // und bleibt true, auch wenn es wieder aus dem Sichtfeld scrollt,
      // um die Animation nicht zu wiederholen.
      if (entry.isIntersecting) {
        setIsVisible(true);
      }
    }, options);

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [options]);

  return [ref, isVisible] as const;
};

export default useScrollReveal;
