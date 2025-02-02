importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: "AIzaSyA3Pq3Xkl4seIVYUJeWngBUKIEIisloRK0",
  authDomain: "wdw-app-52a3c.firebaseapp.com",
  projectId: "wdw-app-52a3c",
  storageBucket: "wdw-app-52a3c.firebasestorage.app",
  messagingSenderId: "321889980783",
  appId: "1:321889980783:web:8dd7ef9b8d265e2660589a",
  measurementId: "G-4374PRF0WF"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
//  console.log("[firebase-messaging-sw.js] Received background message ", payload);
  const notificationTitle = payload.notification?.title || payload.notification?.notification?.title;
  const notificationBody = payload.notification?.body || payload.notification?.notification?.body;
  const notificationOptions = {
    body: notificationBody
  };
  self.registration.showNotification(notificationTitle, notificationOptions);
});
