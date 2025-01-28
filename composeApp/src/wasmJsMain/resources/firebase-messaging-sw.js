importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging-compat.js');

const firebaseConfig: {
 apiKey: "REDACTED",
 authDomain: "REDACTED",
 projectId: "REDACTED",
 storageBucket: "REDACTED",
 messagingSenderId: "REDACTED",
 appId: "REDACTED",
 measurementId: "REDACTED"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log("[firebase-messaging-sw.js] Received background message ", payload);
  const notificationTitle = payload.notification?.title || payload.notification?.notification?.title;
  const notificationBody = payload.notification?.body || payload.notification?.notification?.body;
  const notificationOptions = {
    body: notificationBody
  };
  self.registration.showNotification(notificationTitle, notificationOptions);
});
