// src/components/SettingsModal.tsx
import React, { useState, useEffect } from 'react';
import { Modal, View, Text, Pressable, StyleSheet, TextInput } from 'react-native';

type Props = {
  visible: boolean;
  onClose: () => void;
  radiusKm: number;
  setRadiusKm: (v: number) => void | Promise<void>;
  thresholds: { yellow: number; red: number };
  setThresholds: (t: { yellow: number; red: number }) => void | Promise<void>;
};

export const SettingsModal: React.FC<Props> = ({ visible, onClose, radiusKm, setRadiusKm, thresholds, setThresholds }) => {
  const [radius, setRadius] = useState(String(radiusKm));
  const [y, setY] = useState(String(thresholds.yellow));
  const [r, setR] = useState(String(thresholds.red));

  useEffect(() => { setRadius(String(radiusKm)); }, [radiusKm]);
  useEffect(() => { setY(String(thresholds.yellow)); setR(String(thresholds.red)); }, [thresholds]);

  const save = async () => {
    const rk = Math.min(10, Math.max(3, parseInt(radius || '3')));
    const yy = Math.max(0, parseFloat(y || '5'));
    const rr = Math.max(yy, parseFloat(r || '10'));
    await setRadiusKm(rk);
    await setThresholds({ yellow: yy, red: rr });
    onClose();
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>Settings</Text>

          <View style={styles.row}>
            <Text style={styles.label}>alert radius</Text>
            <TextInput style={styles.input} keyboardType="numeric" value={radius} onChangeText={setRadius} />
          </View>

          <View style={styles.row}>
            <Text style={styles.label}>Yellow threshold (mm/h)</Text>
            <TextInput style={styles.input} keyboardType="numeric" value={y} onChangeText={setY} />
          </View>

          <View style={styles.row}>
            <Text style={styles.label}>Red threshold (mm/h)</Text>
            <TextInput style={styles.input} keyboardType="numeric" value={r} onChangeText={setR} />
          </View>

          <View style={styles.actions}>
            <Pressable style={[styles.btn, styles.cancel]} onPress={onClose}><Text>Cancel</Text></Pressable>
            <Pressable style={[styles.btn, styles.save]} onPress={save}><Text style={{ color: 'white', fontWeight: '700' }}>Save</Text></Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.35)', justifyContent: 'center', alignItems: 'center' },
  card: { width: '90%', backgroundColor: 'white', borderRadius: 12, padding: 16 },
  title: { fontWeight: '800', fontSize: 18, marginBottom: 12 },
  row: { marginBottom: 10 },
  label: { marginBottom: 6, color: '#333' },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 10 },
  actions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 12 },
  btn: { paddingHorizontal: 14, paddingVertical: 10, borderRadius: 8 },
  cancel: { backgroundColor: '#eee' },
  save: { backgroundColor: '#2563eb' },
});
