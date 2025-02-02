window.wdwFirebaseBridge = {
 firebaseConfig: {
  apiKey: "AIzaSyA3Pq3Xkl4seIVYUJeWngBUKIEIisloRK0",
  authDomain: "wdw-app-52a3c.firebaseapp.com",
  projectId: "wdw-app-52a3c",
  storageBucket: "wdw-app-52a3c.firebasestorage.app",
  messagingSenderId: "321889980783",
  appId: "1:321889980783:web:8dd7ef9b8d265e2660589a",
  measurementId: "G-4374PRF0WF"
},

  app: null,
  messaging: null,

  initFirebase: async function() {
      if (!this.app) {
        try {
          this.app = firebase.initializeApp(this.firebaseConfig);
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
//                  icon: payload.notification.icon || 'firebase-logo.png'
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
//          console.log("ServiceWorker registered:", registration);
        })
        .catch((err) => {
          console.error("ServiceWorker registration failed:", err);
          throw err;
        });

    },

  requestNotificationPermission: async function() {
//   console.log("requestNotificationPermission called");
   const permission = await Notification.requestPermission();
//   console.log("Notification.requestPermission result:", permission);
   return permission;
 },

 getFcmToken: async function() {
   try {
    if (typeof this.getTokenFn !== 'function') {
        throw new Error("this.getTokenFn is NOT a function! (Direct import issue?)");
    }
    const token = await this.getTokenFn(
    this.messaging,
    { vapidKey: "BEDMYda2sdkb5LAP5WDx56YMnP4V5yxTdNuNbB-xLoesKb6EAE9q7g6x6R4LKEqZGdSEjfowc7kG5G0jrwZoEuI" });
    return token;
   } catch (err) {
     console.error("Error getting FCM token:", err);
     throw err;
   }
 },
 getTokenFn: null,
}