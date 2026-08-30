// src/data/places.ts
export type Place = {
  id: string;
  type: 'hospital' | 'shelter';
  name: string;
  lat: number;
  lng: number;
  address: string;
};

export const PLACES: Place[] = [
  // Hospitals (sample, approximate coords)
  { id: 'h1', type: 'hospital', name: 'District Hospital Uttarkashi', lat: 30.7289, lng: 78.4356, address: 'NH-34, Uttarkashi, Uttarakhand' },
  { id: 'h2', type: 'hospital', name: 'CHC Bhatwari', lat: 30.8060, lng: 78.5660, address: 'Bhatwari, Uttarkashi' },
  { id: 'h3', type: 'hospital', name: 'PHC Dharali', lat: 30.8933, lng: 79.0703, address: 'Dharali, Uttarkashi' },
  { id: 'h4', type: 'hospital', name: 'Harsil Army Medical Unit', lat: 30.7508, lng: 78.7326, address: 'Harsil' },
  { id: 'h5', type: 'hospital', name: 'Maneri Health Center', lat: 30.7922, lng: 78.4621, address: 'Maneri' },
  { id: 'h6', type: 'hospital', name: 'Purola Community Health', lat: 30.8838, lng: 78.0710, address: 'Purola' },

  // Shelters (sample)
  { id: 's1', type: 'shelter', name: 'Uttarkashi Govt School Shelter', lat: 30.7298, lng: 78.4398, address: 'Govt Inter College, Uttarkashi' },
  { id: 's2', type: 'shelter', name: 'Dharali Community Hall', lat: 30.8911, lng: 79.0631, address: 'Near Main Market, Dharali' },
  { id: 's3', type: 'shelter', name: 'Bhatwari Panchayat Bhawan', lat: 30.8075, lng: 78.5672, address: 'Bhatwari' },
  { id: 's4', type: 'shelter', name: 'Harsil GMVN Shelter', lat: 30.7535, lng: 78.7350, address: 'GMVN, Harsil' },
  { id: 's5', type: 'shelter', name: 'Maneri School Shelter', lat: 30.7935, lng: 78.4630, address: 'Maneri' },
  { id: 's6', type: 'shelter', name: 'Joshiyara Shelter', lat: 30.7214, lng: 78.4421, address: 'Joshiyara, Uttarkashi' },
];
