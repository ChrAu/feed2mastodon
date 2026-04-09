const fs = require('fs');
const path = require('path');

const buildDir = path.resolve(__dirname, 'dist');
const swTemplatePath = path.resolve(__dirname, 'service-worker.js'); // Die aktuelle SW-Datei als Vorlage
const swOutputPath = path.resolve(__dirname, 'dist', 'service-worker.js'); // Die generierte SW-Datei im dist-Ordner

const CACHE_NAME = 'feed2mastodon-cache-v1';

async function generateServiceWorker() {
  try {
    const filesToCache = [];

    // Add root files (index.html, manifest.json)
    filesToCache.push('/'); // The root path itself
    if (fs.existsSync(path.join(buildDir, 'index.html'))) {
      filesToCache.push('/index.html');
    }
    if (fs.existsSync(path.join(buildDir, 'manifest.json'))) {
      filesToCache.push('/manifest.json');
    }

    // Add icons (assuming they are copied to dist/icons or directly accessible from root)
    // If your icons are in dist/icons, adjust this. For now, assuming they are directly in webui root and accessible via /icons/
    filesToCache.push('/icons/icon-192x192.png');
    filesToCache.push('/icons/icon-512x512.png');

    // Read assets directory
    const assetsDir = path.join(buildDir, 'assets');
    if (fs.existsSync(assetsDir)) {
      const assetFiles = fs.readdirSync(assetsDir);
      assetFiles.forEach(file => {
        if (file.endsWith('.js') || file.endsWith('.css')) {
          filesToCache.push(`/assets/${file}`);
        }
      });
    }

    // Read the service worker template
    let swContent = fs.readFileSync(swTemplatePath, 'utf8');

    // Replace the urlsToCache array in the template
    const urlsToCacheString = JSON.stringify(filesToCache, null, 2);
    swContent = swContent.replace(
      /const urlsToCache = \[[\s\S]*?\];/,
      `const urlsToCache = ${urlsToCacheString};`
    );

    // Ensure the CACHE_NAME is also updated if needed, or keep it static
    swContent = swContent.replace(
      /const CACHE_NAME = 'feed2mastodon-cache-v\d+';/,
      `const CACHE_NAME = '${CACHE_NAME}';`
    );


    // Write the generated service worker to the dist folder
    fs.writeFileSync(swOutputPath, swContent, 'utf8');

    console.log('Service Worker generated successfully with updated cache list!');
    console.log('Cached URLs:', filesToCache);

  } catch (error) {
    console.error('Error generating service worker:', error);
    process.exit(1);
  }
}

generateServiceWorker();
