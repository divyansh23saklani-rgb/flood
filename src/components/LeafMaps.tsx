
import React, { useCallback, useEffect, useMemo, useRef } from "react";
import { Platform } from "react-native";
import { WebView, WebViewMessageEvent } from "react-native-webview";

type Incident = { id?: string; type: "landslide" | "flood" | "tree" | "road"; lat: number; lng: number; time: number };
type EmergencyService = { name: string; type: "hospital" | "police" | "relief"; lat: number; lng: number };

export interface LeafMapProps {
  // Center and appearance
  userLat: number;
  userLng: number;
  radiusMeters: number;
  riskColor: "red" | "orange" | "green";

  // Layers and data
  incidents: Incident[];
  emergencyServices: EmergencyService[];

  // Simulation zones toggles
  showIncidents: boolean;
  showHeat: boolean;
  showEmergency: boolean;
  showSimulationZones: boolean;
  selectedSimulation: "" | "2013" | "2021";

  // Handlers
  onMapPress?: (lat: number, lng: number) => void;
  onClearRequested?: () => void; // Called when HTML asks RN to clear incidents
  onDirections?: (lat: number, lng: number) => void; // Optional

  // Local asset selection
  // If using file assets, place leafletMap.html into android/app/src/main/assets/ and ios via require
  iosHtmlRequire?: number; // e.g. require("../../assets/leafletMap.html")
}

export default function LeafMap(props: LeafMapProps) {
  const {
    userLat,
    userLng,
    radiusMeters,
    riskColor,
    incidents,
    emergencyServices,
    showIncidents,
    showHeat,
    showEmergency,
    showSimulationZones,
    selectedSimulation,
    onMapPress,
    onClearRequested,
    onDirections,
    iosHtmlRequire,
  } = props;

  const webRef = useRef<WebView>(null);

  // Build payload sent to WebView to render state
  const payload = useMemo(
    () => ({
      type: "RN_DATA",
      payload: {
        center: { lat: userLat, lng: userLng },
        radiusMeters,
        riskColor,
        incidents,
        emergencyServices,
        showIncidents,
        showHeat,
        showEmergency,
        showSimulationZones,
        selectedSimulation,
      },
    }),
    [
      userLat,
      userLng,
      radiusMeters,
      riskColor,
      incidents,
      emergencyServices,
      showIncidents,
      showHeat,
      showEmergency,
      showSimulationZones,
      selectedSimulation,
    ]
  );

  const postState = useCallback(() => {
    if (!webRef.current) return;
    webRef.current.postMessage(JSON.stringify(payload));
  }, [payload]);

  useEffect(() => {
    postState();
  }, [postState]);

  const onMessage = useCallback(
    (e: WebViewMessageEvent) => {
      try {
        const msg = JSON.parse(e.nativeEvent.data);
        if (msg?.type === "mapPress" && msg?.lat && msg?.lng) {
          onMapPress?.(msg.lat, msg.lng);
        }
        if (msg?.type === "clearReportsRequest") {
          onClearRequested?.();
        }
        if (msg?.type === "directions" && msg?.lat && msg?.lng) {
          onDirections?.(msg.lat, msg.lng);
        }
      } catch {
        // ignore
      }
    },
    [onMapPress, onClearRequested, onDirections]
  );

  const source =
    Platform.OS === "android"
      ? { uri: "file:///android_asset/leafletMap.html" }
      : iosHtmlRequire
      ? iosHtmlRequire
      : undefined;

  return (
    <WebView
      ref={webRef}
      source={source}
      originWhitelist={["*"]}
      javaScriptEnabled
      domStorageEnabled
      allowFileAccess
      onMessage={onMessage}
      onLoadEnd={postState}
    />
  );
}

