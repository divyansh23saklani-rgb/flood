// src/features/pins.ts
import { CONFIG } from '../config';
import type { Report as LocalReport } from './pinslocal';
import { subscribeReportsLocal, addReportLocal, startSimulateLiveLocal, stopSimulateLiveLocal } from './pinslocal';

// We type alias so App imports a single type.
export type Report = LocalReport;

let startedSim = false;

export function subscribeReports(cb: (list: Report[]) => void) {
  if (CONFIG.USE_FIREBASE) {
    // Dynamic import only if enabled so you don't need firebase installed.
    // @ts-ignore
    return import('./pinsFirestore').then((mod) => mod.subscribeReportsFirestore(cb));
  } else {
    if (CONFIG.SIMULATE_LIVE_LOCAL && !startedSim) { startSimulateLiveLocal(); startedSim = true; }
    return subscribeReportsLocal(cb);
  }
}

export async function addReport(type: Report['type'], note: string | undefined, lat: number, lng: number) {
  if (CONFIG.USE_FIREBASE) {
    // @ts-ignore
    const mod = await import('./pinsFirestore');
    return mod.addReportFirestore(type, note, lat, lng);
  } else {
    return addReportLocal(type, note, lat, lng);
  }
}
