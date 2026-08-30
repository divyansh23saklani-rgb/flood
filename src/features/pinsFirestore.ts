// src/features/pinsFirestore.ts
import { initializeFirebaseIfNeeded, db } from '../lib/firebase';
import { addDoc, collection, onSnapshot, query, serverTimestamp, where, orderBy } from 'firebase/firestore';

export type Report = {
  id: string;
  type: 'distress' | 'blocked' | 'yellow';
  note?: string;
  lat: number;
  lng: number;
  createdAt: any;
  score: number;
};

export function subscribeReportsFirestore(cb: (list: Report[]) => void) {
  initializeFirebaseIfNeeded();
  const cutoff = Date.now() - 12 * 60 * 60 * 1000;
  const q = query(
    collection(db, 'reports'),
    where('createdAtMs', '>=', cutoff),
    where('score', '>=', -2),
    orderBy('createdAtMs', 'desc')
  );
  const unsub = onSnapshot(q, (snap) => {
    const list: Report[] = [];
    snap.forEach((doc) => {
      const d = doc.data();
      list.push({
        id: doc.id,
        type: d.type,
        note: d.note,
        lat: d.lat,
        lng: d.lng,
        createdAt: d.createdAt,
        score: d.score ?? 0,
      });
    });
    cb(list);
  });
  return unsub;
}

export async function addReportFirestore(type: 'distress' | 'blocked' | 'yellow', note: string | undefined, lat: number, lng: number) {
  initializeFirebaseIfNeeded();
  await addDoc(collection(db, 'reports'), {
    type, note, lat, lng,
    createdAt: serverTimestamp(),
    createdAtMs: Date.now(),
    score: 0,
  });
}
