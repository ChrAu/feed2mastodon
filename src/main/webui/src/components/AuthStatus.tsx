import React, { useState, useEffect } from 'react';
import { Lock, LogOut } from 'lucide-react';

const AuthStatus: React.FC = () => {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    const fetchAuthStatus = async () => {
      try {
        const response = await fetch('/api/user');
          // Prüfen, ob die Antwort wirklich OK (200) ist
          if (!response.ok) {
              setIsLoggedIn(false);
              return;
          }

          const userInfo = await response.json();
          setIsLoggedIn(userInfo.loggedIn);

          if (userInfo.loggedIn) {
            if (userInfo.givenName && userInfo.familyName) {
              setUsername(`${userInfo.givenName} ${userInfo.familyName}`);
            } else if (userInfo.preferredUsername) {
              setUsername(userInfo.preferredUsername);
            } else {
              setUsername(userInfo.username || 'User');
            }
          } else {
            setUsername(null);
          }

      } catch (error) {
        console.error('Error fetching auth status:', error);
        setIsLoggedIn(false);
        setUsername(null);
      }
    };

    fetchAuthStatus();
  }, []);

    useEffect(() => {
        if (isLoggedIn) {
            const savedPath = sessionStorage.getItem('post_login_redirect');
            if (savedPath) {
                sessionStorage.removeItem('post_login_redirect');
                // Falls du React Router nutzt: navigate(savedPath);
                // Oder nativ (Vorsicht: führt zum Reload):
                if (window.location.pathname !== savedPath) {
                    window.location.href = savedPath;
                }
            }
        }
    }, [isLoggedIn]);

const handleLogin = () => {
    // 1. Aktuellen Pfad merken
    sessionStorage.setItem('post_login_redirect', window.location.pathname);

    // 2. Login triggern
    window.location.href = '/api/user/login';
};

    const handleLogout = () => {
        // 1. UI sofort auf "ausgeloggt" setzen, damit der Button verschwindet
        setIsLoggedIn(false);
        setUsername(null);

        // 2. Den Browser die Arbeit machen lassen (erledigt Redirects & Cookies)
        window.location.href = '/logout';
    };

  return (
    <div className="auth-status flex items-center space-x-4">
      {isLoggedIn ? (
        <>
          <span className="text-slate-300 text-sm">Angemeldet als: <span className="font-semibold text-white">{username}</span></span>
          <button
            onClick={handleLogout}
            className="p-1.5 bg-red-600 text-white text-sm rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-500 focus:ring-opacity-50 transition-colors duration-200 flex items-center justify-center"
            title="Logout"
          >
            <LogOut size={18} />
          </button>
        </>
      ) : (
        <button
          onClick={handleLogin}
          className="p-1.5 bg-blue-600/20 text-blue-300 text-sm rounded-md border border-blue-500/30 hover:bg-blue-600/30 hover:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 transition-colors duration-200 flex items-center justify-center"
          title="Login"
        >
          <Lock size={18} />
        </button>
      )}
    </div>
  );
};

export default AuthStatus;
