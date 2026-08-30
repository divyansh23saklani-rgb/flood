// src/lib/firebase.ts
// Only needed if you enable Firestore. Otherwise this file is never executed.
import { initializeApp, getApps } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';

let _db: any;

export const firebaseConfig = {
  apiKey: 'YOUR_API_KEY',
  authDomain: 'YOUR_PROJECT.firebaseapp.com',
  projectId: 'YOUR_PROJECT',
  storageBucket: 'YOUR_PROJECT.appspot.com',
  messagingSenderId: '000000000000',
  appId: '1:000000000000:web:000000000000',
};

export function initializeFirebaseIfNeeded() {
  if (!getApps().length) {
    initializeApp(firebaseConfig);
  }
  if (!_db) _db = getFirestore();
}

export const db = {
  get current() {
    initializeFirebaseIfNeeded();
    return _db;
  }
} as any;
