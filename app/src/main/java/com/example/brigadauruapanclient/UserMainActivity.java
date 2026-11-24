package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserMainActivity extends AppCompatActivity implements SimuladorCompartidoUruapan.Listener {

    private static final double URUAPAN_LAT = 19.4064492;
    private static final double URUAPAN_LON = -102.0430476;
    private static final double DEFAULT_RADIUS_KM = 10.0;

    private WebView webUserMap;
    private boolean mapaListo = false;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvUserBrigadeInitials, tvUserBrigadeName, tvUserBrigadeUnit, tvUserBrigadeStatus;

    private final List<UnidadUsuario> unidadesCercanas = new ArrayList<>();
    private UnidadUsuario unidadSeleccionada;
    private boolean usandoDatosSimulados = true;

    public static class UnidadUsuario {
        public final String id;
        public final String tipo;
        public String estado;
        public double lat;
        public double lon;
        public final boolean enMovimiento;

        public UnidadUsuario(String id, String tipo, String estado, double lat, double lon, boolean enMovimiento) {
            this.id = id;
            this.tipo = tipo;
            this.estado = estado;
            this.lat = lat;
            this.lon = lon;
            this.enMovimiento = enMovimiento;
        }

        public String getEtiquetaEstado() {
            if ("BUSY".equalsIgnoreCase(estado)) return "En ruta / Ocupada";
            if ("ON_SCENE".equalsIgnoreCase(estado)) return "En el punto";
            return "Disponible";
        }
    }

    public class BridgeUsuario {
        @JavascriptInterface
        public void onMarkerClicked(String id, String tipo, String estado, double lat, double lon) {
            boolean mov = false;
            for (UnidadUsuario u : unidadesCercanas) {
                if (u.id.equals(id)) {
                    mov = u.enMovimiento;
                    break;
                }
            }
            final boolean enMov = mov;
            runOnUiThread(() -> mostrarDetalleBrigada(new UnidadUsuario(id, tipo, estado, lat, lon, enMov)));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        tvUserBrigadeInitials = findViewById(R.id.tvUserBrigadeInitials);
        tvUserBrigadeName = findViewById(R.id.tvUserBrigadeName);
        tvUserBrigadeUnit = findViewById(R.id.tvUserBrigadeUnit);
        tvUserBrigadeStatus = findViewById(R.id.tvUserBrigadeStatus);

        LinearLayout layoutAuxilioRapido = findViewById(R.id.layoutAuxilioRapido);
        if (layoutAuxilioRapido != null) {
            layoutAuxilioRapido.setOnClickListener(v -> abrirSolicitarBrigada());
        }

        TextView tvSolicitarDesdeBarra = findViewById(R.id.tvSolicitarDesdeBarra);
        if (tvSolicitarDesdeBarra != null) {
            tvSolicitarDesdeBarra.setOnClickListener(v -> abrirSolicitarBrigada());
        }

        View bottomSheetCard = findViewById(R.id.bottomSheetUserCard);
        if (bottomSheetCard != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        configurarWebViewLeaflet();
        cargarUnidadesCercanas();

        Button btnCallBrigade = findViewById(R.id.btnCallBrigade);
        if (btnCallBrigade != null) {
            btnCallBrigade.setOnClickListener(v ->
                    Toast.makeText(this, "Contactando con la brigada de asistencia...", Toast.LENGTH_SHORT).show()
            );
        }

        Button btnRequestBrigade = findViewById(R.id.btnRequestBrigade);
        if (btnRequestBrigade != null) {
            btnRequestBrigade.setOnClickListener(v -> abrirSolicitarBrigada());
        }

        LinearLayout tabInicio = findViewById(R.id.tabUserMapa);
        if (tabInicio != null) {
            tabInicio.setOnClickListener(v -> {
                if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
                cargarUnidadesCercanas();
            });
        }

        LinearLayout tabHistorial = findViewById(R.id.tabUserHistorial);
        if (tabHistorial != null) {
            tabHistorial.setOnClickListener(v -> {
                startActivity(new Intent(UserMainActivity.this, UserHistorialActivity.class));
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabUserCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                startActivity(new Intent(UserMainActivity.this, UserCuentaActivity.class));
                finish();
            });
        }
    }

    private void abrirSolicitarBrigada() {
        Intent intent = new Intent(UserMainActivity.this, SolicitarBrigadaActivity.class);
        if (unidadSeleccionada != null) {
            intent.putExtra("unit_id", unidadSeleccionada.id);
            intent.putExtra("unit_type", unidadSeleccionada.tipo);
            intent.putExtra("lat", unidadSeleccionada.lat);
            intent.putExtra("lon", unidadSeleccionada.lon);
        }
        startActivity(intent);
    }

    @Override
    public void onActualizado(List<SimuladorCompartidoUruapan.UnidadSimulada> unidades) {
        if (!usandoDatosSimulados) return;
        unidadesCercanas.clear();
        for (SimuladorCompartidoUruapan.UnidadSimulada s : unidades) {
            unidadesCercanas.add(new UnidadUsuario(s.id, s.tipo, s.estado, s.lat, s.lon, s.enMovimiento));
        }
        enviarMarcadoresAlMapa();
    }

    private void configurarWebViewLeaflet() {
        webUserMap = findViewById(R.id.mapUserContainer);
        if (webUserMap == null) return;

        WebSettings settings = webUserMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webUserMap.addJavascriptInterface(new BridgeUsuario(), "AndroidBridge");
        webUserMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mapaListo = true;
                enviarMarcadoresAlMapa();
            }
        });

        String html = construirHtmlLeaflet(URUAPAN_LAT, URUAPAN_LON);
        webUserMap.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
    }

    private String construirHtmlLeaflet(double lat, double lon) {
        return "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=2.0, user-scalable=yes'/>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body,#map{width:100%;height:100%;margin:0;padding:0;background:#131C31;}" +
                ".leyenda-estados{position:fixed;bottom:16px;left:14px;z-index:1000;background:rgba(15,23,42,0.92);" +
                "color:#F8FAFC;padding:10px 12px;border-radius:12px;font-family:sans-serif;font-size:12px;" +
                "box-shadow:0 4px 12px rgba(0,0,0,0.35);line-height:1.5;}" +
                ".leyenda-item{display:flex;align-items:center;gap:8px;}" +
                ".dot{width:10px;height:10px;border-radius:50%;display:inline-block;border:1.5px solid #fff;}" +
                "</style>" +
                "</head><body><div id='map'></div>" +
                "<div class='leyenda-estados'>" +
                "<div class='leyenda-item'><span class='dot' style='background:#10B981;'></span><span>Disponible</span></div>" +
                "<div class='leyenda-item'><span class='dot' style='background:#2563EB;'></span><span>En el punto</span></div>" +
                "<div class='leyenda-item'><span class='dot' style='background:#EF4444;'></span><span>En ruta / Incidente</span></div>" +
                "</div><script>" +
                "var map = L.map('map', {zoomControl:true}).setView([" + lat + ", " + lon + "], 14);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "  maxZoom: 19," +
                "  attribution: '&copy; OpenStreetMap'" +
                "}).addTo(map);" +
                "var markersLayer = L.layerGroup().addTo(map);" +
                "var pinsById = {};" +
                "function colorPorEstado(estado) {" +
                "  var e = (estado || '').toUpperCase();" +
                "  if (e === 'BUSY' || e.indexOf('INCIDENT') !== -1) return '#EF4444';" +
                "  if (e === 'ON_SCENE' || e.indexOf('PUNTO') !== -1) return '#2563EB';" +
                "  return '#10B981';" +
                "}" +
                "window.centrarEnPunto = function(targetLat, targetLon, zoomLevel) {" +
                "  map.setView([targetLat, targetLon], zoomLevel || map.getZoom(), {animate: true});" +
                "};" +
                "window.actualizarUnidades = function(jsonStr) {" +
                "  var lista = JSON.parse(jsonStr);" +
                "  var idsActivos = {};" +
                "  lista.forEach(function(u) {" +
                "    idsActivos[u.id] = true;" +
                "    var color = colorPorEstado(u.estado);" +
                "    if (pinsById[u.id]) {" +
                "      pinsById[u.id].setLatLng([u.lat, u.lon]);" +
                "      pinsById[u.id].setStyle({fillColor: color, color: '#FFFFFF', weight: 2.5});" +
                "    } else {" +
                "      var pin = L.circleMarker([u.lat, u.lon], {" +
                "        radius: 10, fillColor: color, color: '#FFFFFF', weight: 2.5, opacity: 1, fillOpacity: 0.95" +
                "      }).addTo(markersLayer);" +
                "      pin.on('click', function() {" +
                "        if (window.AndroidBridge) {" +
                "          window.AndroidBridge.onMarkerClicked(u.id, u.tipo, u.estado, u.lat, u.lon);" +
                "        }" +
                "      });" +
                "      pinsById[u.id] = pin;" +
                "    }" +
                "  });" +
                "  Object.keys(pinsById).forEach(function(id) {" +
                "    if (!idsActivos[id]) {" +
                "      markersLayer.removeLayer(pinsById[id]);" +
                "      delete pinsById[id];" +
                "    }" +
                "  });" +
                "};" +
                "</script></body></html>";
    }

    private void cargarUnidadesCercanas() {
        String endpoint = String.format(
                Locale.US,
                "/api/units/nearby?lat=%.6f&lon=%.6f&radiusKm=%.1f",
                URUAPAN_LAT, URUAPAN_LON, DEFAULT_RADIUS_KM
        );

        new Thread(() -> {
            List<UnidadUsuario> cargadas = new ArrayList<>();
            boolean desdeBackend = false;

            try {
                ApiClient.ApiResponse response = ApiClient.request(this, "GET", endpoint, null);
                if (response.isSuccessful()) {
                    JSONArray unitsArray = new JSONArray(response.body);
                    for (int i = 0; i < unitsArray.length(); i++) {
                        JSONObject unit = unitsArray.optJSONObject(i);
                        if (unit == null) continue;

                        double lat = unit.optDouble("latitude", Double.NaN);
                        double lon = unit.optDouble("longitude", Double.NaN);
                        if (Double.isNaN(lat) || Double.isNaN(lon)) continue;

                        String id = unit.optString("unitId", "UN-" + (i + 1));
                        String tipo = unit.optString("unitType", "UNIT");
                        String estado = unit.optString("status", "AVAILABLE");
                        cargadas.add(new UnidadUsuario(id, tipo, estado, lat, lon, false));
                    }
                    desdeBackend = !cargadas.isEmpty();
                }
            } catch (Exception ignored) {
            }

            final boolean esSimulado = !desdeBackend;
            if (esSimulado) {
                for (SimuladorCompartidoUruapan.UnidadSimulada s : SimuladorCompartidoUruapan.getInstance().getUnidades()) {
                    cargadas.add(new UnidadUsuario(s.id, s.tipo, s.estado, s.lat, s.lon, s.enMovimiento));
                }
            }

            final List<UnidadUsuario> resultadoFinal = cargadas;
            runOnUiThread(() -> {
                usandoDatosSimulados = esSimulado;
                unidadesCercanas.clear();
                unidadesCercanas.addAll(resultadoFinal);
                enviarMarcadoresAlMapa();
                if (usandoDatosSimulados) {
                    SimuladorCompartidoUruapan.getInstance().suscribir(this);
                } else {
                    SimuladorCompartidoUruapan.getInstance().desuscribir(this);
                }
            });
        }).start();
    }

    private void enviarMarcadoresAlMapa() {
        if (!mapaListo || webUserMap == null) return;

        JSONArray array = new JSONArray();
        for (UnidadUsuario u : unidadesCercanas) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", u.id);
                obj.put("tipo", u.tipo);
                obj.put("estado", u.estado);
                obj.put("lat", u.lat);
                obj.put("lon", u.lon);
                array.put(obj);
            } catch (Exception ignored) {
            }
        }

        String jsonEscapado = JSONObject.quote(array.toString());
        webUserMap.evaluateJavascript("window.actualizarUnidades(" + jsonEscapado + ");", null);
    }

    private void mostrarDetalleBrigada(UnidadUsuario u) {
        unidadSeleccionada = u;

        String iniciales = u.id.length() >= 2 ? u.id.substring(0, 2).toUpperCase(Locale.ROOT) : "UN";
        if (tvUserBrigadeInitials != null) {
            tvUserBrigadeInitials.setText(iniciales);
        }
        if (tvUserBrigadeName != null) {
            tvUserBrigadeName.setText("Unidad " + u.id);
        }
        if (tvUserBrigadeUnit != null) {
            tvUserBrigadeUnit.setText(String.format(Locale.US, "%s · %.4f, %.4f", u.tipo, u.lat, u.lon));
        }
        if (tvUserBrigadeStatus != null) {
            tvUserBrigadeStatus.setText("● " + u.getEtiquetaEstado());
        }
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webUserMap != null) {
            webUserMap.onResume();
        }
        if (usandoDatosSimulados) {
            SimuladorCompartidoUruapan.getInstance().suscribir(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SimuladorCompartidoUruapan.getInstance().desuscribir(this);
        if (webUserMap != null) {
            webUserMap.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        SimuladorCompartidoUruapan.getInstance().desuscribir(this);
        super.onDestroy();
    }
}