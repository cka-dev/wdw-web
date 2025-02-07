window.wdwFirebaseBridge = {
 firebaseConfig: {
   apiKey: "REDACTED",
   authDomain: "REDACTED",
   projectId: "REDACTED",
   storageBucket: "REDACTED",
   messagingSenderId: "REDACTED",
   appId: "REDACTED",
   measurementId: "REDACTED"
  },

  app: null,
  messaging: null,
  auth: null,

  initFirebase: async function() {
    if (!this.app) {
      try {
        this.app = firebase.initializeApp(this.firebaseConfig);
        const authModule = await import("https://www.gstatic.com/firebasejs/11.2.0/firebase-auth.js");
        this.auth = authModule.getAuth(this.app);

        this.modularSignInWithCustomToken = authModule.signInWithCustomToken;
        this.setPersistenceFunc = authModule.setPersistence;
        this.browserLocalPersistence = authModule.browserLocalPersistence;
        this._onAuthStateChanged= authModule.onAuthStateChanged;


      } catch (appInitError) {
        console.error("Error during firebase.initializeApp:", appInitError);
        return;
      }

      try {
        const messagingModule = await import("https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging.js");
        this.messaging = messagingModule.getMessaging(this.app);
        this.getTokenFn = messagingModule.getToken;
        this.onMessageFn = messagingModule.onMessage;
        this.onMessageFn(this.messaging, (payload) => {
          const notificationTitle = payload.notification.title;
          const notificationOptions = {
            body: payload.notification.body,
            // icon: payload.notification.icon || 'firebase-logo.png'
          };
          new Notification(payload.notification.title, notificationOptions);
        });
      } catch (messagingError) {
        console.error("Error during DIRECT IMPORT of messaging:", messagingError);
        return;
      }
    }

    navigator.serviceWorker.register("firebase-messaging-sw.js")
      .then((registration) => {
        // console.log("ServiceWorker registered:", registration);
      })
      .catch((err) => {
        console.error("ServiceWorker registration failed:", err);
        throw err;
      });
  },

  signInWithCustomToken: async function(token) {
      try {
        await this.modularSignInWithCustomToken(this.auth, token);
      } catch (error) {
        throw error;
    }
  },


  persistSession: async function() {
    try {
      await this.setPersistenceFunc(this.auth, this.browserLocalPersistence)
    } catch (error) {
      throw error;
    }
  },

  signOut: async function() {
    try {
      await this.auth.signOut();
    } catch (error) {
      throw error;
    }
  },

  getCurrentUser: async function() {
    return this.auth.currentUser;
  },

  observeAuthState: async function(callback) {
    return this._onAuthStateChanged(this.auth, (user) => {
      try {
        callback(user ? { uid: user.uid, email: user.email } : null);
      } catch (e) {
        throw e;
      }
    });
  },

  waitUntilInitialized: async function() {
    return new Promise((resolve) => {
      const checkInitialized = () => {
        if (this.auth) {
          this.setPersistenceFunc(this.auth, this.browserLocalPersistence).then(() => {
            resolve();
          });
        } else {
          setTimeout(checkInitialized, 100);
        }
      };
      checkInitialized();
    });
  },


  requestNotificationPermission: async function() {
    const permission = await Notification.requestPermission();
    return permission;
  },

  getFcmToken: async function() {
    try {
      if (typeof this.getTokenFn !== 'function') {
          throw new Error("this.getTokenFn is NOT a function! (Direct import issue?)");
      }
      const token = await this.getTokenFn(
      this.messaging,
      { vapidKey: "REDACTED"});
      return token;
    } catch (err) {
      console.error("Error getting FCM token:", err);
      throw err;
    }
  },
  getTokenFn: null,
};