import React, { useEffect, useState } from "react";
import { View, StyleSheet } from "react-native";
import { WebView } from "react-native-webview";

// Demo community reports (distress/block/yellow)
const communityReports = [
  { lat: 30.730, lng: 78.440, type: "distress", note: "Road blocked" },
  { lat: 30.725, lng: 78.430, type: "yellow", note: "Water rising" },
];

// Demo safe places
const safePlaces = [
  { lat: 30.735, lng: 78.445, type: "hospital", name: "Govt Hospital" },
  { lat: 30.720, lng: 78.435, type: "shelter", name: "Community Shelter" },
];

interface MapWebViewProps {
  userLat?: number;
  userLng?: number;
  risk?: "green" | "yellow" | "red";
  circleRadius?: number; // in meters
}

export default function MapWebView({
  userLat = 30.7268,
  userLng = 78.435,
  risk = "green",
  circleRadius = 3000,
}: MapWebViewProps) {
  const [html, setHtml] = useState("");

  useEffect(() => {
    const riskColor =
      risk === "red" ? "red" : risk === "yellow" ? "orange" : "green";

    const htmlContent = `
      <!DOCTYPE html>
      <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <link
            rel="stylesheet"
            href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          />
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <style>
            #map { height: 100vh; width: 100vw; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            var map = L.map('map').setView([${userLat}, ${userLng}], 13);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19
            }).addTo(map);

            // User marker
            L.marker([${userLat}, ${userLng}]).addTo(map).bindPopup("You");

            // Risk circle
            L.circle([${userLat}, ${userLng}], {
              color: '${riskColor}',
              fillColor: '${riskColor}',
              fillOpacity: 0.25,
              radius: ${circleRadius}
            }).addTo(map);

            // **NEW CODE TO HANDLE MAP CLICKS**
            map.on('click', function(e) {
              const data = {
                type: 'mapPress',
                payload: {
                  latitude: e.latlng.lat,
                  longitude: e.latlng.lng
                }
              };
              if (window.ReactNativeWebView) {
                window.ReactNativeWebView.postMessage(JSON.stringify(data));
              }
            });

            // Community reports
            const reports = ${JSON.stringify(communityReports)};
            reports.forEach(r => {
              let color = r.type === "distress" ? "red" : r.type === "yellow" ? "orange" : "blue";
              L.marker([r.lat, r.lng], {icon: L.icon({iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png'})})
               .addTo(map)
               .bindPopup(r.note || r.type);
            });

            // Safe places
            const safe = ${JSON.stringify(safePlaces)};
            safe.forEach(p => {
              let iconColor = p.type === "hospital" ? "green" : "blue";
              L.circleMarker([p.lat, p.lng], {
                color: iconColor,
                radius: 8,
                fillOpacity: 0.9
              }).addTo(map).bindPopup(p.name + " (" + p.type + ")");
            });
          </script>
        </body>
      </html>
    `;

    setHtml(htmlContent);
  }, [userLat, userLng, risk, circleRadius]);

  return (
    <View style={styles.container}>
      <WebView originWhitelist={["*"]} source={{ html }} style={{ flex: 1 }} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
});