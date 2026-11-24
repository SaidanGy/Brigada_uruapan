package com.example.brigadauruapanclient;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class SimuladorRutasUruapan {

    public interface OnSimulacionUpdateListener {
        void onUnidadesActualizadas(List<UnidadSimulada> unidades);
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
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<UnidadSimulada> unidades = new ArrayList<>();
    private OnSimulacionUpdateListener listener;
    private boolean activa = false;
    private int cicloGlobal = 0;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!activa) return;
            cicloGlobal++;
            avanzarUnPaso();
            if (listener != null) {
                listener.onUnidadesActualizadas(unidades);
            }
            handler.postDelayed(this, 1700);
        }
    };

    public SimuladorRutasUruapan() {
        inicializarUnidades();
    }

    public void setListener(OnSimulacionUpdateListener listener) {
        this.listener = listener;
    }

    public List<UnidadSimulada> getUnidades() {
        return unidades;
    }

    public void iniciar() {
        if (activa) return;
        activa = true;
        handler.removeCallbacks(tickRunnable);
        handler.postDelayed(tickRunnable, 1500);
    }

    public void detener() {
        activa = false;
        handler.removeCallbacks(tickRunnable);
    }

    private void inicializarUnidades() {
        unidades.clear();

        // 1. Quieta en base / punto fijo en Uruapan
        List<PuntoGeo> fijaPc = new ArrayList<>();
        fijaPc.add(new PuntoGeo(19.4192, -102.0581));
        unidades.add(new UnidadSimulada(
                "BRG-01",
                "CIVIL_PROTECTION",
                "Base Operativa · Uruapan",
                "AVAILABLE",
                fijaPc,
                false
        ));

        // 2. Quieta en punto de vigilancia urbano
        List<PuntoGeo> fijaPol = new ArrayList<>();
        fijaPol.add(new PuntoGeo(19.4125, -102.0583));
        unidades.add(new UnidadSimulada(
                "POL-03",
                "POLICE",
                "Punto de Vigilancia · Centro",
                "AVAILABLE",
                fijaPol,
                false
        ));

        // 3. Ambulancia AMB-02: Avance sutil por carretera/avenida
        List<PuntoGeo> calleAmb = new ArrayList<>();
        calleAmb.add(new PuntoGeo(19.4118, -102.0645));
        calleAmb.add(new PuntoGeo(19.4124, -102.0602));
        calleAmb.add(new PuntoGeo(19.4131, -102.0558));
        calleAmb.add(new PuntoGeo(19.4148, -102.0516));
        calleAmb.add(new PuntoGeo(19.4176, -102.0475));
        calleAmb.add(new PuntoGeo(19.4205, -102.0432));
        calleAmb.add(new PuntoGeo(19.4234, -102.0388));
        unidades.add(new UnidadSimulada(
                "AMB-02",
                "AMBULANCE",
                "Corredor Urbano · Uruapan",
                "BUSY",
                calleAmb,
                true
        ));

        // 4. Bomberos BOM-04: Avance sutil sobre corredor oriente / enlace vial
        List<PuntoGeo> calleBom = new ArrayList<>();
        calleBom.add(new PuntoGeo(19.4198, -102.0315));
        calleBom.add(new PuntoGeo(19.4162, -102.0322));
        calleBom.add(new PuntoGeo(19.4124, -102.0329));
        calleBom.add(new PuntoGeo(19.4086, -102.0338));
        calleBom.add(new PuntoGeo(19.4048, -102.0352));
        calleBom.add(new PuntoGeo(19.4012, -102.0374));
        unidades.add(new UnidadSimulada(
                "BOM-04",
                "FIRE",
                "Corredor Oriente · Uruapan",
                "BUSY",
                calleBom,
                true
        ));
    }

    private void avanzarUnPaso() {
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

            // Simulación sutil de tráfico: cada ciertos ciclos hace una pausa breve o avanza más lento
            if (u.ticksTrafico > 0) {
                u.ticksTrafico--;
                continue;
            }
            if ((cicloGlobal + i * 3) % 9 == 0) {
                u.ticksTrafico = 1; // Pausa corta de 1 tick simulando tráfico/semáforo
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
}