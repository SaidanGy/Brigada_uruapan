package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SimuladorCompartidoUruapan.Listener {

    // Coordenadas de referencia de Uruapan, Michoacán
    public static final double URUAPAN_LAT = 19.4064492;
    public static final double URUAPAN_LON = -102.0430476;
    private static final double DEFAULT_RADIUS_KM = 10.0;

    private WebView webMap;
    private boolean mapaListo = false;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvBrigadeInitials, tvBrigadeName, tvBrigadeUnit, tvBrigadeStatus;
    private EditText etSearch;

    private final List<UnidadMapa> unidadesActuales = new ArrayList<>();
    private UnidadMapa unidadSeleccionada;
    private boolean usandoDatosSimulados = false;

    public static class UnidadMapa {
        public final String id;
        public final String tipo;
        public String estado;
        public double lat;
        public double lon;
        public final boolean enMovimiento;

        public UnidadMapa(String id, String tipo, String estado, double lat, double lon, boolean enMovimiento) {
            this.id = id;
            this.tipo = tipo;
            this.estado = estado;
            this.lat = lat;
            this.lon = lon;
            this.enMovimiento = enMovimiento;
        }
    }

    public class BridgeAndroid {
        @JavascriptInterface
        public void onMarkerClicked(String id, String tipo, String estado, double lat, double lon) {
            boolean mov = false;
            for (UnidadMapa u : unidadesActuales) {
                if (u.id.equals(id)) {
                    mov = u.enMovimiento;
                    break;
                }
            }
            final boolean enMov = mov;
            runOnUiThread(() -> mostrarDetalleEnTarjeta(new UnidadMapa(id, tipo, estado, lat, lon, enMov)));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvBrigadeInitials = findViewById(R.id.tvBrigadeInitials);
        tvBrigadeName = findViewById(R.id.tvBrigadeName);
        tvBrigadeUnit = findViewById(R.id.tvBrigadeUnit);
        tvBrigadeStatus = findViewById(R.id.tvBrigadeStatus);

        View bottomSheetCard = findViewById(R.id.bottomSheetCard);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        TextView tvEstadoUnidadMapa = findViewById(R.id.tvEstadoUnidadMapa);
        if (tvEstadoUnidadMapa != null) {
            SharedPreferences prefsEstado = getSharedPreferences("brigada_estado_prefs", MODE_PRIVATE);
            String ultimoEstado = prefsEstado.getString("ultimo_estado", "");
            int ultimoColor = prefsEstado.getInt("ultimo_color", 0xFF38BDF8);

            if (!ultimoEstado.isEmpty()) {
                tvEstadoUnidadMapa.setText("● " + ultimoEstado + " · Uruapan");
            }
            tvEstadoUnidadMapa.setTextColor(ultimoColor);
        }

        configurarWebViewLeaflet();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    enviarMarcadoresAlMapa(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }

        cargarUnidadesCercanas();

        Button btnCallBrigade = findViewById(R.id.btnCallBrigade);
        if (btnCallBrigade != null) {
            btnCallBrigade.setOnClickListener(v ->
                    Toast.makeText(this, "Iniciando llamada con la brigada...", Toast.LENGTH_SHORT).show()
            );
        }

        Button btnRequestBrigade = findViewById(R.id.btnRequestBrigade);
        if (btnRequestBrigade != null) {
            btnRequestBrigade.setText("Usar en Unidad");
            btnRequestBrigade.setOnClickListener(v -> abrirRegistrarUnidad());
        }

        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
                cargarUnidadesCercanas();
            });
        }

        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> abrirRegistrarUnidad());
        }

        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, OrganizacionesActivity.class));
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, CuentaActivity.class));
                finish();
            });
        }
    }

    @Override
    public void onActualizado(List<SimuladorCompartidoUruapan.UnidadSimulada> unidades) {
        if (!usandoDatosSimulados) return;
        unidadesActuales.clear();
        for (SimuladorCompartidoUruapan.UnidadSimulada s : unidades) {
            unidadesActuales.add(new UnidadMapa(s.id, s.tipo, s.estado, s.lat, s.lon, s.enMovimiento));
        }
        refrescarMarcadoresEnMapa();
    }

    private void abrirRegistrarUnidad() {
        Intent intent = new Intent(MainActivity.this, RegistrarUnidadActivity.class);
        if (unidadSeleccionada != null) {
            intent.putExtra("unit_id", unidadSeleccionada.id);
            intent.putExtra("unit_type", unidadSeleccionada.tipo);
            intent.putExtra("status", unidadSeleccionada.estado);
            intent.putExtra("lat", unidadSeleccionada.lat);
            intent.putExtra("lon", unidadSeleccionada.lon);
        }
        startActivity(intent);
        finish();
    }

    private void configurarWebViewLeaflet() {
        webMap = findViewById(R.id.mapContainer);
        if (webMap == null) return;

        WebSettings settings = webMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webMap.addJavascriptInterface(new BridgeAndroid(), "AndroidBridge");
        webMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mapaListo = true;
                enviarMarcadoresAlMapa(etSearch != null ? etSearch.getText().toString() : "");
            }
        });

        String html = construirHtmlLeaflet(URUAPAN_LAT, URUAPAN_LON);
        webMap.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
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

    private void aplicarActualizacionDesdeIntent(List<UnidadMapa> lista) {
        Intent intent = getIntent();
        if (intent == null) return;

        String estadoRecibido = intent.getStringExtra("status");
        if (estadoRecibido == null || estadoRecibido.isEmpty()) return;

        double latRecibida = intent.getDoubleExtra("lat", Double.NaN);
        double lonRecibida = intent.getDoubleExtra("lon", Double.NaN);
        String idRecibido = intent.getStringExtra("unit_id");
        String tipoRecibido = intent.getStringExtra("unit_type");

        boolean encontrada = false;
        if (idRecibido != null && !idRecibido.isEmpty()) {
            for (UnidadMapa u : lista) {
                if (u.id.equalsIgnoreCase(idRecibido)) {
                    u.estado = estadoRecibido;
                    if (!Double.isNaN(latRecibida) && !Double.isNaN(lonRecibida)) {
                        u.lat = latRecibida;
                        u.lon = lonRecibida;
                    }
                    unidadSeleccionada = u;
                    encontrada = true;
                    break;
                }
            }
        }

        if (!encontrada && !Double.isNaN(latRecibida) && !Double.isNaN(lonRecibida) && !lista.isEmpty()) {
            UnidadMapa primera = lista.get(0);
            primera.estado = estadoRecibido;
            primera.lat = latRecibida;
            primera.lon = lonRecibida;
            if (tipoRecibido != null && !tipoRecibido.isEmpty()) {
                unidadSeleccionada = new UnidadMapa(primera.id, tipoRecibido, estadoRecibido, latRecibida, lonRecibida, primera.enMovimiento);
            } else {
                unidadSeleccionada = primera;
            }
        }
    }

    private void cargarUnidadesCercanas() {
        String endpoint = String.format(
                Locale.US,
                "/api/units/nearby?lat=%.6f&lon=%.6f&radiusKm=%.1f",
                URUAPAN_LAT, URUAPAN_LON, DEFAULT_RADIUS_KM
        );

        new Thread(() -> {
            List<UnidadMapa> cargadas = new ArrayList<>();
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
                        cargadas.add(new UnidadMapa(id, tipo, estado, lat, lon, false));
                    }
                    desdeBackend = !cargadas.isEmpty();
                }
            } catch (Exception ignored) {
            }

            final boolean esSimulado = !desdeBackend;
            if (esSimulado) {
                for (SimuladorCompartidoUruapan.UnidadSimulada s : SimuladorCompartidoUruapan.getInstance().getUnidades()) {
                    cargadas.add(new UnidadMapa(s.id, s.tipo, s.estado, s.lat, s.lon, s.enMovimiento));
                }
            }

            aplicarActualizacionDesdeIntent(cargadas);

            final List<UnidadMapa> resultadoFinal = cargadas;
            runOnUiThread(() -> {
                usandoDatosSimulados = esSimulado;
                unidadesActuales.clear();
                unidadesActuales.addAll(resultadoFinal);
                refrescarMarcadoresEnMapa();
                if (usandoDatosSimulados) {
                    SimuladorCompartidoUruapan.getInstance().suscribir(this);
                } else {
                    SimuladorCompartidoUruapan.getInstance().desuscribir(this);
                }
            });
        }).start();
    }

    private void refrescarMarcadoresEnMapa() {
        enviarMarcadoresAlMapa(etSearch != null ? etSearch.getText().toString() : "");
    }

    private void enviarMarcadoresAlMapa(String filtro) {
        if (!mapaListo || webMap == null) return;

        String textoFiltro = filtro.trim().toLowerCase(Locale.ROOT);
        JSONArray array = new JSONArray();

        for (UnidadMapa u : unidadesActuales) {
            String coincidencia = (u.id + " " + u.tipo + " " + u.estado).toLowerCase(Locale.ROOT);
            if (!textoFiltro.isEmpty() && !coincidencia.contains(textoFiltro)) {
                continue;
            }
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
        webMap.evaluateJavascript("window.actualizarUnidades(" + jsonEscapado + ");", null);
    }

    private void mostrarDetalleEnTarjeta(UnidadMapa u) {
        unidadSeleccionada = u;

        String iniciales = u.id.length() >= 2 ? u.id.substring(0, 2).toUpperCase(Locale.ROOT) : "UN";
        if (tvBrigadeInitials != null) {
            tvBrigadeInitials.setText(iniciales);
        }
        if (tvBrigadeName != null) {
            tvBrigadeName.setText("Unidad " + u.id);
        }
        if (tvBrigadeUnit != null) {
            String modo = u.enMovimiento ? "En movimiento" : "En base";
            tvBrigadeUnit.setText(String.format(Locale.US, "%s · %s · %.4f, %.4f", u.tipo, modo, u.lat, u.lon));
        }
        if (tvBrigadeStatus != null) {
            tvBrigadeStatus.setText("● " + u.estado);
        }
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webMap != null) {
            webMap.onResume();
        }
        if (usandoDatosSimulados) {
            SimuladorCompartidoUruapan.getInstance().suscribir(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SimuladorCompartidoUruapan.getInstance().desuscribir(this);
        if (webMap != null) {
            webMap.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        SimuladorCompartidoUruapan.getInstance().desuscribir(this);
        super.onDestroy();
    }
}