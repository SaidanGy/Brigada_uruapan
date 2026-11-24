package com.example.brigadauruapanclient;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimuladorCompartidoUruapan {

    public interface Listener {
        void onActualizado(List<UnidadSimulada> unidades);
    }

    public static class PuntoGeo {
        public final double lat;
        public final double lon;

        public PuntoGeo(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static class UnidadSimulada {
        public final String id;
        public final String tipo;
        public final String nombreBase;
        public String estado; // AVAILABLE, BUSY, ON_SCENE
        public double lat;
        public double lon;
        public final boolean enMovimiento;
        public final List<PuntoGeo> waypoints;

        private int indiceActual = 0;
        private double avance = 0.0;
        private int ticksEnPunto = 0;
        private int ticksTrafico = 0;

        public UnidadSimulada(String id, String tipo, String nombreBase, String estado, List<PuntoGeo> waypoints, boolean enMovimiento) {
            this.id = id;
            this.tipo = tipo;
            this.nombreBase = nombreBase;
            this.estado = estado;
            this.waypoints = waypoints;
            this.enMovimiento = enMovimiento;
            if (waypoints != null && !waypoints.isEmpty()) {
                this.lat = waypoints.get(0).lat;
                this.lon = waypoints.get(0).lon;
            }
        }

        public String getEstadoLegible() {
            if ("BUSY".equalsIgnoreCase(estado)) return "En ruta / Ocupada";
            if ("ON_SCENE".equalsIgnoreCase(estado)) return "En el punto";
            return "Disponible";
        }
    }

    private static SimuladorCompartidoUruapan instancia;

    public static synchronized SimuladorCompartidoUruapan getInstance() {
        if (instancia == null) {
            instancia = new SimuladorCompartidoUruapan();
        }
        return instancia;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<UnidadSimulada> unidades = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private boolean corriendo = false;
    private int cicloGlobal = 0;

    private final Runnable loopRunnable = new Runnable() {
        @Override
        public void run() {
            if (!corriendo) return;
            cicloGlobal++;
            avanzarPaso();
            notificarListeners();
            handler.postDelayed(this, 1700);
        }
    };

    private SimuladorCompartidoUruapan() {
        inicializar();
    }

    public synchronized void suscribir(Listener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
            l.onActualizado(unidades);
        }
        iniciar();
    }

    public synchronized void desuscribir(Listener l) {
        listeners.remove(l);
        if (listeners.isEmpty()) {
            detener();
        }
    }

    public List<UnidadSimulada> getUnidades() {
        return unidades;
    }

    public void iniciar() {
        if (corriendo) return;
        corriendo = true;
        handler.removeCallbacks(loopRunnable);
        handler.postDelayed(loopRunnable, 1500);
    }

    public void detener() {
        corriendo = false;
        handler.removeCallbacks(loopRunnable);
    }

    private void notificarListeners() {
        for (Listener l : new ArrayList<>(listeners)) {
            l.onActualizado(unidades);
        }
    }

    private PuntoGeo puntoFijo(double lat, double lon) {
        List<PuntoGeo> lista = new ArrayList<>();
        lista.add(new PuntoGeo(lat, lon));
        return lista.get(0);
    }

    private List<PuntoGeo> listaFija(double lat, double lon) {
        List<PuntoGeo> lista = new ArrayList<>();
        lista.add(new PuntoGeo(lat, lon));
        return lista;
    }

    private void inicializar() {
        unidades.clear();

        // ==========================================
        // 1. PUNTOS FIJOS (SIN MOVIMIENTO): BOMBEROS
        // ==========================================
        unidades.add(new UnidadSimulada("BOM-VOL-1", "FIRE", "H. Cuerpo Voluntario de Bomberos Uruapan", "AVAILABLE", listaFija(19.4235, -102.0710), false));
        unidades.add(new UnidadSimulada("BOM-VOL-2", "FIRE", "Heroico Cuerpo De Bomberos Voluntarios de Uruapan", "AVAILABLE", listaFija(19.4252, -102.0625), false));
        unidades.add(new UnidadSimulada("BOM-PRO", "FIRE", "Bomberos Profesionales de Uruapan A. C.", "AVAILABLE", listaFija(19.4138, -102.0695), false));
        unidades.add(new UnidadSimulada("BOM-URU", "FIRE", "Cuerpo de Bomberos de Uruapan", "AVAILABLE", listaFija(19.4125, -102.0565), false));
        unidades.add(new UnidadSimulada("BOM-EQP", "FIRE", "Equipos de Bombeo", "AVAILABLE", listaFija(19.4038, -102.0632), false));

        // ==========================================
        // 2. PUNTOS FIJOS (SIN MOVIMIENTO): POLICÍA
        // ==========================================
        unidades.add(new UnidadSimulada("POL-FED-1", "POLICE", "Policía Federal Preventiva SSP", "AVAILABLE", listaFija(19.4228, -102.0612), false));
        unidades.add(new UnidadSimulada("POL-MIN", "POLICE", "Policía Ministerial", "AVAILABLE", listaFija(19.4205, -102.0548), false));
        unidades.add(new UnidadSimulada("POL-TUM", "POLICE", "Comandancia de Policía Tumbiscatío Villa Ruiz", "AVAILABLE", listaFija(19.4112, -102.0492), false));
        unidades.add(new UnidadSimulada("POL-FED-2", "POLICE", "Policía Federal Estación Uruapan", "AVAILABLE", listaFija(19.4068, -102.0485), false));
        unidades.add(new UnidadSimulada("POL-FIS", "POLICE", "Fiscalía Regional de Uruapan", "AVAILABLE", listaFija(19.4175, -102.0255), false));

        // ==========================================
        // 3. UNIDADES EN MOVIMIENTO: AMBULANCIAS
        // ==========================================
        List<PuntoGeo> rutaAmbVillarosas = new ArrayList<>();
        rutaAmbVillarosas.add(new PuntoGeo(19.4215, -102.0608)); // Ambulancias Villarosas
        rutaAmbVillarosas.add(new PuntoGeo(19.4192, -102.0575));
        rutaAmbVillarosas.add(new PuntoGeo(19.4165, -102.0538));
        rutaAmbVillarosas.add(new PuntoGeo(19.4138, -102.0496));
        rutaAmbVillarosas.add(new PuntoGeo(19.4105, -102.0452));
        rutaAmbVillarosas.add(new PuntoGeo(19.4052, -102.0418)); // Hacia Hospital General
        unidades.add(new UnidadSimulada("AMB-VIL", "AMBULANCE", "Ambulancias Villarosas", "BUSY", rutaAmbVillarosas, true));

        List<PuntoGeo> rutaAmbScaph = new ArrayList<>();
        rutaAmbScaph.add(new PuntoGeo(19.4152, -102.0668)); // Ambulancias SCAPH
        rutaAmbScaph.add(new PuntoGeo(19.4130, -102.0620));
        rutaAmbScaph.add(new PuntoGeo(19.4112, -102.0568));
        rutaAmbScaph.add(new PuntoGeo(19.4095, -102.0512));
        rutaAmbScaph.add(new PuntoGeo(19.4072, -102.0455));
        unidades.add(new UnidadSimulada("AMB-SCP", "AMBULANCE", "Ambulancias SCAPH", "BUSY", rutaAmbScaph, true));

        // ==========================================
        // 4. UNIDADES EN MOVIMIENTO: PROTECCIÓN CIVIL
        // ==========================================
        List<PuntoGeo> rutaPcMun = new ArrayList<>();
        rutaPcMun.add(new PuntoGeo(19.4032, -102.0615)); // Protección Civil Municipal
        rutaPcMun.add(new PuntoGeo(19.4068, -102.0585));
        rutaPcMun.add(new PuntoGeo(19.4108, -102.0552));
        rutaPcMun.add(new PuntoGeo(19.4148, -102.0518));
        rutaPcMun.add(new PuntoGeo(19.4188, -102.0485));
        unidades.add(new UnidadSimulada("PC-MUN", "CIVIL_PROTECTION", "Protección Civil Municipal", "BUSY", rutaPcMun, true));

        List<PuntoGeo> rutaPcEst = new ArrayList<>();
        rutaPcEst.add(new PuntoGeo(19.4115, -102.0378)); // Protección Civil Estatal Delegación Uruapan
        rutaPcEst.add(new PuntoGeo(19.4142, -102.0365));
        rutaPcEst.add(new PuntoGeo(19.4175, -102.0352));
        rutaPcEst.add(new PuntoGeo(19.4205, -102.0342));
        rutaPcEst.add(new PuntoGeo(19.4228, -102.0385));
        unidades.add(new UnidadSimulada("PC-EST", "CIVIL_PROTECTION", "Protección Civil Estatal Uruapan", "BUSY", rutaPcEst, true));

        List<PuntoGeo> rutaPanam = new ArrayList<>();
        rutaPanam.add(new PuntoGeo(19.4222, -102.0525)); // Servicio Panamericano de Protección
        rutaPanam.add(new PuntoGeo(19.4212, -102.0472));
        rutaPanam.add(new PuntoGeo(19.4202, -102.0418));
        rutaPanam.add(new PuntoGeo(19.4185, -102.0368));
        unidades.add(new UnidadSimulada("PC-PAN", "CIVIL_PROTECTION", "Servicio Panamericano de Protección", "BUSY", rutaPanam, true));
    }

    private void avanzarPaso() {
        for (int i = 0; i < unidades.size(); i++) {
            UnidadSimulada u = unidades.get(i);
            if (!u.enMovimiento || u.waypoints == null || u.waypoints.size() < 2) {
                continue;
            }

            if ("ON_SCENE".equals(u.estado)) {
                u.ticksEnPunto++;
                if (u.ticksEnPunto >= 6) {
                    u.ticksEnPunto = 0;
                    u.estado = "AVAILABLE";
                    u.indiceActual = 0;
                    u.avance = 0.0;
                    u.lat = u.waypoints.get(0).lat;
                    u.lon = u.waypoints.get(0).lon;
                }
                continue;
            }

            if ("AVAILABLE".equals(u.estado)) {
                u.estado = "BUSY";
            }

            if (u.ticksTrafico > 0) {
                u.ticksTrafico--;
                continue;
            }
            if ((cicloGlobal + i * 3) % 9 == 0) {
                u.ticksTrafico = 1;
                continue;
            }

            boolean tramoConTrafico = ((u.indiceActual + i) % 2 == 1);
            double paso = tramoConTrafico ? 0.035 : 0.065;

            PuntoGeo origen = u.waypoints.get(u.indiceActual);
            PuntoGeo destino = u.waypoints.get(u.indiceActual + 1);

            u.avance += paso;
            if (u.avance >= 1.0) {
                u.avance = 0.0;
                u.indiceActual++;

                if (u.indiceActual >= u.waypoints.size() - 1) {
                    PuntoGeo finalPt = u.waypoints.get(u.waypoints.size() - 1);
                    u.lat = finalPt.lat;
                    u.lon = finalPt.lon;
                    u.estado = "ON_SCENE";
                    continue;
                }
                origen = u.waypoints.get(u.indiceActual);
                destino = u.waypoints.get(u.indiceActual + 1);
            }

            u.lat = origen.lat + (destino.lat - origen.lat) * u.avance;
            u.lon = origen.lon + (destino.lon - origen.lon) * u.avance;
        }
    }

    public static String generarFolioCorto(String prefijoTipo) {
        String pref = (prefijoTipo == null || prefijoTipo.isEmpty()) ? "BRG" : prefijoTipo.substring(0, Math.min(3, prefijoTipo.length())).toUpperCase(Locale.ROOT);
        int num = (int) (Math.random() * 9000) + 1000;
        return pref + "-" + num;
    }
}