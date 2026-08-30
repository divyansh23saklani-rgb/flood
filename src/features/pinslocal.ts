// src/features/pinsLocal.ts
export type Report = {
  id: string;
  type: 'distress' | 'blocked' | 'yellow';
  note?: string;
  lat: number;
  lng: number;
  createdAt: number; // ms epoch
  score: number;     // hide if < -2
};

const seedBase: Report[] = [
  // Historical-like samples (Uttarkashi/Dharali region)
  { id: 'h1', type: 'yellow', note: 'High river discharge reported', lat: 30.7298, lng: 78.4398, createdAt: Date.now() - 1000 * 60 * 60 * 10, score: 1 },
  { id: 'h2', type: 'blocked', note: 'Road blocked due to landslide', lat: 30.8075, lng: 78.5672, createdAt: Date.now() - 1000 * 60 * 60 * 8, score: 0 },
  { id: 'h3', type: 'distress', note: 'Village cut off near Harsil', lat: 30.7535, lng: 78.7350, createdAt: Date.now() - 1000 * 60 * 60 * 6, score: 0 },
  { id: 'h4', type: 'yellow', note: 'Bridge approach waterlogged', lat: 30.7922, lng: 78.4621, createdAt: Date.now() - 1000 * 60 * 60 * 5, score: 2 },
  { id: 'h5', type: 'blocked', note: 'Tree fallen at bend', lat: 30.7240, lng: 78.4335, createdAt: Date.now() - 1000 * 60 * 60 * 3, score: 0 },
  { id: 'h6', type: 'distress', note: 'Evac required near Joshiyara', lat: 30.7214, lng: 78.4421, createdAt: Date.now() - 1000 * 60 * 60 * 2, score: 1 },
];

let store: Report[] = [...seedBase];

const listeners = new Set<(list: Report[]) => void>();

function notify() {
  const cutoff = Date.now() - 12 * 60 * 60 * 1000; // last 12h
  const filtered = store.filter(r => r.createdAt >= cutoff && r.score >= -2);
  for (const cb of listeners) cb(filtered);
}

export function subscribeReportsLocal(cb: (list: Report[]) => void) {
  listeners.add(cb);
  notify();
  return () => { listeners.delete(cb); };
}

export async function addReportLocal(type: Report['type'], note: string | undefined, lat: number, lng: number) {
  const r: Report = {
    id: Math.random().toString(36).slice(2),
    type, note, lat, lng,
    createdAt: Date.now(),
    score: 0,
  };
  store.unshift(r);
  notify();
}

// fun: simulate live updates (for demo)
let simTimer: any;
export function startSimulateLiveLocal() {
  // disabled by config; kept for compatibility
}
export function stopSimulateLiveLocal() {
  if (simTimer) clearInterval(simTimer);
  simTimer = undefined;
}
