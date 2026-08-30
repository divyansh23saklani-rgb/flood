// src/lib/geo.ts
export function haversineKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }) {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const R = 6371; // km
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

export function nearestN<T extends { lat: number; lng: number }>(
  list: (T & { id: string; name?: string; address?: string; type?: string })[],
  origin: { lat: number; lng: number },
  n: number
) {
  return list
    .map(item => ({ ...item, distanceKm: haversineKm(origin, { lat: item.lat, lng: item.lng }) }))
    .sort((a, b) => a.distanceKm - b.distanceKm)
    .slice(0, n);
}
