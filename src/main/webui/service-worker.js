const CACHE_NAME = 'feed2mastodon-cache-v1';
const urlsToCache = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  '/assets/x-CZtwCJbv.js',
  '/assets/Home-Bx3mZI2I.js',
  '/assets/mail-DABjqiFb.js',
  '/assets/index-NftZiiBQ.js',
  '/assets/react-CM_0bdEm.js',
  '/assets/index-BJX4FCIq.css',
  '/assets/Tanken-DIwkuaXq.js',
  '/assets/MailTest-Di4Sy8js.js',
  '/assets/Impressum-CRhyfkU-.js',
  '/assets/LineChart-CYdzkoZN.js',
  '/assets/web-vitals-DeogYGFn.js',
  '/assets/Datenschutz-fiY40oJX.js',
  '/assets/jsx-runtime-9YgKe2Eq.js',
  '/assets/ServerStatus-BTDYXfgV.js',
  '/assets/defineProperty-D6CiH3uZ.js',
  '/assets/PiHoleDashboard-MNfkF8Lt.js',
  '/assets/FuelPriceDashboard-CamjIVA6.js'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Service Worker: Cache geöffnet');
        return cache.addAll(urlsToCache).catch(error => {
          console.error('Service Worker: Fehler beim Cachen von URLs:', error);
        });
      })
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        if (response) {
          return response;
        }
        return fetch(event.request).then(
          response => {
            if(!response || response.status !== 200 || response.type !== 'basic') {
              return response;
            }
            const responseToCache = response.clone();
            caches.open(CACHE_NAME)
              .then(cache => {
                cache.put(event.request, responseToCache);
              });
            return response;
          }
        ).catch(error => {
          console.error('Service Worker: Fetch fehlgeschlagen:', event.request.url, error);
          // Optional: Eine Offline-Seite zurückgeben
          // return caches.match('/offline.html');
        });
      })
    );
});

self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            console.log('Service Worker: Alten Cache löschen:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
