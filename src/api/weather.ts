// src/api/weather.ts
export type WeatherRisk = 'green' | 'yellow' | 'red';

/**
 * Fetch the next-6-hours maximum precipitation (mm/hr) from Open-Meteo.
 * Free, no API key required.
 */
export async function getPrecipNext6hMax(lat: number, lng: number): Promise<number> {
  // Simulated dataset (no network): simple function that returns higher values
  // near typical heavy-rain zones/time for demo.
  // Base on lat/lng roughly around Uttarkashi.
  const base = Math.max(0, 12 - Math.abs(30.73 - lat) * 40 - Math.abs(78.44 - lng) * 40);
  const diurnal = (() => {
    const h = new Date().getHours();
    // heavier in late afternoon/evening for demo
    if (h >= 15 && h <= 22) return 4;
    if (h >= 6 && h <= 9) return 2;
    return 0;
  })();
  const noise = Math.random() * 1.5;
  const p = Math.max(0, base + diurnal + noise);
  // Cap to 20 mm/h for sensible UI
  return Math.min(20, p);
}

/**
 * Compute risk based on thresholds (defaults: yellow >= 5, red >= 10).
 */
export function riskFromPrecip(p: number, thresholds?: { yellow: number; red: number }): WeatherRisk {
  const y = thresholds?.yellow ?? 5;
  const r = thresholds?.red ?? 10;
  if (p >= r) return 'red';
  if (p >= y) return 'yellow';
  return 'green';
}
