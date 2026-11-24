package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class OrganizacionesActivity extends AppCompatActivity {

    private static final String PREFS_SYNC_OPERADOR = "alerta_despacho_sync_prefs";
    private static final String PREFS_SOLICITUDES = "solicitudes_ciudadano_prefs";
    private static final String PREFS_ORG = "organizaciones_uruapan_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";
    private static final String PREFS_OPERADOR_CACHE = "perfil_operador_cache";
    private static final String PREFS_HISTORIAL_UNIDAD = "historial_unidad_prefs";
    private static final String PREFS_ESTADO_BRIGADA = "brigada_estado_prefs";

    private ImageButton btnRefreshAlerts;
    private TextView tvEmptyPending;
    private LinearLayout layoutPendingDispatchesContainer;

    private TextView tvNoActiveAlert;
    private LinearLayout layoutActiveAlertDetails;
    private TextView tvActiveAlertType, tvActiveAlertStatus, tvActiveAlertDescription, tvActiveAlertCoords, tvActiveAlertTimes;
    private Button btnArriveAlert, btnResolveAlert;

    private TextView tvUnitCurrentStatus, tvEmptyUnitHistory;
    private LinearLayout layoutUnitHistoryContainer;

    private String alertaActivaId = "";
    private String alertaActivaFolio = "";
    private String alertaActivaTipo = "-";
    private String alertaActivaDesc = "-";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean bucleActualizacionActivo;
    private final Runnable bucleRefresco = new Runnable() {
        @Override
        public void run() {
            if (!bucleActualizacionActivo) return;
            refrescarTodo();
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRefreshAlerts = findViewById(R.id.btnRefreshAlerts);
        tvEmptyPending = findViewById(R.id.tvEmptyPending);
        layoutPendingDispatchesContainer = findViewById(R.id.layoutPendingDispatchesContainer);

        tvNoActiveAlert = findViewById(R.id.tvNoActiveAlert);
        layoutActiveAlertDetails = findViewById(R.id.layoutActiveAlertDetails);
        tvActiveAlertType = findViewById(R.id.tvActiveAlertType);
        tvActiveAlertStatus = findViewById(R.id.tvActiveAlertStatus);
        tvActiveAlertDescription = findViewById(R.id.tvActiveAlertDescription);
        tvActiveAlertCoords = findViewById(R.id.tvActiveAlertCoords);
        tvActiveAlertTimes = findViewById(R.id.tvActiveAlertTimes);
        btnArriveAlert = findViewById(R.id.btnArriveAlert);
        btnResolveAlert = findViewById(R.id.btnResolveAlert);

        tvUnitCurrentStatus = findViewById(R.id.tvUnitCurrentStatus);
        tvEmptyUnitHistory = findViewById(R.id.tvEmptyUnitHistory);
        layoutUnitHistoryContainer = findViewById(R.id.layoutUnitHistoryContainer);

        if (btnRefreshAlerts != null) {
            btnRefreshAlerts.setOnClickListener(v -> refrescarTodo());
        }

        if (btnArriveAlert != null) {
            btnArriveAlert.setOnClickListener(v -> accionAlertaActiva("arrive", "Llegada registrada"));
        }

        if (btnResolveAlert != null) {
            btnResolveAlert.setOnClickListener(v -> accionAlertaActiva("resolve", "Alerta marcada como resuelta"));
        }

        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                startActivity(new Intent(OrganizacionesActivity.this, MainActivity.class));
                finish();
            });
        }

        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> {
                startActivity(new Intent(OrganizacionesActivity.this, RegistrarUnidadActivity.class));
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                startActivity(new Intent(OrganizacionesActivity.this, CuentaActivity.class));
                finish();
            });
        }

        refrescarTodo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescarTodo();
        bucleActualizacionActivo = true;
        handler.removeCallbacks(bucleRefresco);
        handler.postDelayed(bucleRefresco, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bucleActualizacionActivo = false;
        handler.removeCallbacks(bucleRefresco);
    }

    private String ahoraIsoUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private String valorLimpio(JSONObject obj, String key, String fallback) {
        if (obj == null || obj.isNull(key)) return fallback;
        String val = obj.optString(key, fallback);
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return fallback;
        }
        return val.trim();
    }

    private String obtenerTipoCorporacionActiva() {
        SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        SharedPreferences prefsOrg = getSharedPreferences(PREFS_ORG, MODE_PRIVATE);
        SharedPreferences prefsCache = getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE);

        String correo = prefsLogin.getString("email", "");
        if (correo.isEmpty()) {
            correo = prefsOrg.getString("email_org", "");
        }
        String key = correo.trim().toLowerCase(Locale.ROOT);

        String corporation = !key.isEmpty() ? prefsCache.getString("unit_type_" + key, "") : "";
        if (corporation.isEmpty()) {
            corporation = prefsOrg.getString("unit_type", "");
        }
        corporation = normalizarCorporation(corporation);

        if (corporation.isEmpty()) {
            corporation = normalizarCorporation(detectarUnitTypeDeNombre(prefsOrg.getString("nombre_org", "")));
        }
        if (corporation.isEmpty()) {
            consultarCorporationDesdeBackend();
        }
        return corporation;
    }

    private String normalizarCorporation(String corporation) {
        if (corporation == null) return "";
        String u = corporation.trim().toUpperCase(Locale.ROOT);
        if (u.isEmpty() || u.equals("UNIT") || u.equals("OPERADOR") || u.equals("OPERATOR")
                || u.equals("ADMIN") || u.equals("CIUDADANO") || u.equals("CITIZEN")) {
            return "";
        }
        return corporation.trim();
    }

    private String detectarUnitTypeDeNombre(String nombre) {
        String lower = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (lower.contains("bomber") || lower.contains("fire") || lower.contains("fuego")) return "FIRE";
        if (lower.contains("ambulanc") || lower.contains("medic") || lower.contains("cruz") || lower.contains("salud")) return "AMBULANCE";
        if (lower.contains("polic") || lower.contains("seguridad") || lower.contains("tránsito") || lower.contains("transito") || lower.contains("delito")) return "POLICE";
        if (lower.contains("accident") || lower.contains("civil") || lower.contains("protecc") || lower.contains("guardia") || lower.contains("riesgo") || lower.contains("risk")) return "CIVIL_PROTECTION";
        return "";
    }

    private void consultarCorporationDesdeBackend() {
        SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        SharedPreferences prefsOrg = getSharedPreferences(PREFS_ORG, MODE_PRIVATE);
        SharedPreferences prefsCache = getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE);

        String correo = prefsLogin.getString("email", "");
        if (correo.isEmpty()) {
            correo = prefsOrg.getString("email_org", "");
        }
        final String key = correo.trim().toLowerCase(Locale.ROOT);

        String unitIdTemp = prefsOrg.getString("unit_id", "");
        if (unitIdTemp.isEmpty()) {
            unitIdTemp = !key.isEmpty() ? prefsCache.getString("matricula_" + key, "") : "";
        }
        final String miUnitId = unitIdTemp;
        if (miUnitId.isEmpty()) return;

        new Thread(() -> {
            try {
                String endpoint = String.format(
                        Locale.US,
                        "/api/units/nearby?lat=%.6f&lon=%.6f&radiusKm=%.1f",
                        MainActivity.URUAPAN_LAT, MainActivity.URUAPAN_LON, 10.0);
                ApiClient.ApiResponse response = ApiClient.request(OrganizacionesActivity.this, "GET", endpoint, null);
                if (!response.isSuccessful()) return;

                JSONArray arr = extraerArregloJson(response.body);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject u = arr.optJSONObject(i);
                    if (u == null) continue;
                    String id = u.optString("unitId", "");
                    if (id.isEmpty() || !id.equals(miUnitId)) continue;
                    final String corporation = normalizarCorporation(u.optString("unitType", ""));
                    if (!corporation.isEmpty()) {
                        getSharedPreferences(PREFS_ORG, MODE_PRIVATE)
                                .edit()
                                .putString("unit_type", corporation)
                                .apply();
                        if (!key.isEmpty()) {
                            getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE)
                                    .edit()
                                    .putString("unit_type_" + key, corporation)
                                    .apply();
                        }
                        runOnUiThread(() -> {
                            pintarDespachosPendientes(combinarConAlertasSincronizadas(new JSONArray()));
                            cargarDespachosPendientes();
                        });
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private boolean correspondeACorporacion(String tipoAlertaRaw, String tipoCorporacion) {
        if (tipoCorporacion == null || tipoCorporacion.isEmpty()) return true;
        String alertaUpper = (tipoAlertaRaw == null ? "" : tipoAlertaRaw).toUpperCase(Locale.ROOT);

        if (alertaUpper.equals("OTHER") || alertaUpper.contains("APOYO") || alertaUpper.contains("OTRA")) {
            return true;
        }

        switch (tipoCorporacion) {
            case "FIRE":
                return alertaUpper.contains("FIRE") || alertaUpper.contains("BOMBER") || alertaUpper.contains("INCENDIO")
                        || alertaUpper.contains("FUEGO") || alertaUpper.contains("RESCATE");
            case "AMBULANCE":
                return alertaUpper.contains("MEDICAL") || alertaUpper.contains("MEDIC") || alertaUpper.contains("AMBULANC")
                        || alertaUpper.contains("SCAPH") || alertaUpper.contains("SALUD");
            case "POLICE":
                return alertaUpper.contains("ROBBERY") || alertaUpper.contains("POLIC") || alertaUpper.contains("SEGURIDAD")
                        || alertaUpper.contains("DELITO");
            case "CIVIL_PROTECTION":
                return alertaUpper.contains("ACCIDENT") || alertaUpper.contains("CIVIL") || alertaUpper.contains("PROTECC")
                        || alertaUpper.contains("RISK") || alertaUpper.contains("RIESGO") || alertaUpper.contains("GUARDIA");
            default:
                return true;
        }
    }

    private void refrescarTodo() {
        cargarDespachosPendientes();
        cargarAlertaActiva();
        pintarEstadoYHistorialUnidad();
    }

    private void actualizarEstadoLocalUnidad(String estadoBackend, String etiquetaTexto) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_ESTADO_BRIGADA, MODE_PRIVATE).edit();
        if (estadoBackend != null && !estadoBackend.isEmpty()) {
            editor.putString("estado_backend", estadoBackend);
            editor.putString("ultimo_estado", estadoBackend);
        }
        if (etiquetaTexto != null && !etiquetaTexto.isEmpty()) {
            editor.putString("ultimo_estado_texto", etiquetaTexto);
        }
        editor.apply();
        pintarEstadoYHistorialUnidad();
    }

    private void registrarEnHistorialUnidad(String id, String folio, String tipo, String desc, String estado, String campoHora) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_HISTORIAL_UNIDAD, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("alertas_aceptadas_json", "[]"));
            String ahora = ahoraIsoUtc();

            boolean encontrada = false;
            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (item == null) continue;
                boolean mismoId = id != null && !id.isEmpty() && id.equals(item.optString("id", ""));
                boolean mismoFolio = folio != null && !folio.isEmpty() && folio.equals(item.optString("folio", ""));
                if (mismoId || mismoFolio) {
                    if (estado != null && !estado.isEmpty()) item.put("status", estado);
                    if (campoHora != null && !campoHora.isEmpty()) item.put(campoHora, ahora);
                    lista.put(i, item);
                    encontrada = true;
                    break;
                }
            }

            if (!encontrada) {
                JSONObject nueva = new JSONObject();
                nueva.put("id", id == null ? "" : id);
                nueva.put("folio", folio == null ? "" : folio);
                nueva.put("type", (tipo == null || tipo.isEmpty()) ? "-" : tipo);
                nueva.put("description", (desc == null || desc.isEmpty()) ? "-" : desc);
                nueva.put("status", (estado == null || estado.isEmpty()) ? "EN_ROUTE" : estado);
                nueva.put("acceptedAt", ahora);
                nueva.put("arrivedAt", "");
                nueva.put("resolvedAt", "");
                lista.put(0, nueva);
            }

            prefs.edit().putString("alertas_aceptadas_json", lista.toString()).apply();
            pintarEstadoYHistorialUnidad();
        } catch (Exception ignored) {
        }
    }

    private void pintarEstadoYHistorialUnidad() {
        if (tvUnitCurrentStatus != null) {
            SharedPreferences prefsEstado = getSharedPreferences(PREFS_ESTADO_BRIGADA, MODE_PRIVATE);
            String estadoBackend = prefsEstado.getString("estado_backend", "");
            String ultimoEstado = prefsEstado.getString("ultimo_estado", "");
            String textoEstado = prefsEstado.getString("ultimo_estado_texto", "");

            String estadoMostrar = !estadoBackend.isEmpty() ? estadoBackend : ultimoEstado;
            if (!estadoMostrar.isEmpty() || !textoEstado.isEmpty()) {
                String detalle = estadoMostrar.isEmpty()
                        ? textoEstado
                        : (textoEstado.isEmpty() ? estadoMostrar : estadoMostrar + " · " + textoEstado);
                tvUnitCurrentStatus.setText("Mi estado: " + detalle);
            } else {
                tvUnitCurrentStatus.setText("Mi estado: Sin confirmar (usa Registrar para transmitir)");
            }
        }

        if (layoutUnitHistoryContainer == null || tvEmptyUnitHistory == null) return;
        layoutUnitHistoryContainer.removeAllViews();

        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_HISTORIAL_UNIDAD, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("alertas_aceptadas_json", "[]"));
            if (lista.length() == 0) {
                tvEmptyUnitHistory.setVisibility(View.VISIBLE);
                return;
            }
            tvEmptyUnitHistory.setVisibility(View.GONE);

            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (item == null) continue;

                String folio = item.optString("folio", "");
                String tipo = item.optString("type", "ALERTA");
                String titulo = folio.isEmpty() ? tipo : "Folio " + folio + " · " + tipo;
                String estado = item.optString("status", "-");
                String desc = item.optString("description", "-");

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.VERTICAL);
                fila.setPadding(18, 14, 18, 14);
                fila.setBackgroundColor(0xFF0F172A);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 12);
                fila.setLayoutParams(params);

                TextView tvTitulo = new TextView(this);
                tvTitulo.setText(titulo + " [" + estado + "]");
                tvTitulo.setTextColor(0xFFFFFFFF);
                tvTitulo.setTextSize(13f);

                TextView tvSub = new TextView(this);
                tvSub.setText(desc);
                tvSub.setTextColor(0xFF94A3B8);
                tvSub.setTextSize(11f);

                fila.addView(tvTitulo);
                fila.addView(tvSub);
                layoutUnitHistoryContainer.addView(fila);
            }
        } catch (Exception ignored) {
            tvEmptyUnitHistory.setVisibility(View.VISIBLE);
        }
    }

    private JSONArray extraerArregloJson(String rawBody) {
        JSONArray items = new JSONArray();
        if (rawBody == null || rawBody.trim().isEmpty()) return items;

        try {
            String trimmed = rawBody.trim();
            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                String[] posiblesClaves = {"items", "data", "content", "dispatches", "offers"};
                for (String clave : posiblesClaves) {
                    if (obj.has(clave) && obj.optJSONArray(clave) != null) {
                        return obj.optJSONArray(clave);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return items;
    }

    private JSONArray combinarConAlertasSincronizadas(JSONArray desdeApi) {
        String tipoCorpActiva = obtenerTipoCorporacionActiva();
        JSONArray combinada = new JSONArray();

        if (desdeApi != null) {
            for (int i = 0; i < desdeApi.length(); i++) {
                JSONObject item = desdeApi.optJSONObject(i);
                if (item == null) continue;
                JSONObject alertObj = item.optJSONObject("alert");
                String tipoItem = valorLimpio(item, "type",
                        valorLimpio(item, "requiredUnitType",
                                valorLimpio(alertObj, "type", valorLimpio(alertObj, "requiredUnitType", ""))));
                if (correspondeACorporacion(tipoItem, tipoCorpActiva)) {
                    combinada.put(item);
                }
            }
        }

        try {
            SharedPreferences prefsOp = getSharedPreferences(PREFS_SYNC_OPERADOR, MODE_PRIVATE);
            JSONArray syncArray = new JSONArray(prefsOp.getString("alertas_entrantes_json", "[]"));
            for (int i = 0; i < syncArray.length(); i++) {
                JSONObject s = syncArray.optJSONObject(i);
                if (s == null) continue;

                String estadoSync = valorLimpio(s, "status", "PENDIENTE");
                if ("CANCELADA".equalsIgnoreCase(estadoSync)
                        || "ACEPTADA".equalsIgnoreCase(estadoSync)
                        || "RECHAZADA".equalsIgnoreCase(estadoSync)
                        || "RESUELTA".equalsIgnoreCase(estadoSync)
                        || "EXPIRED".equalsIgnoreCase(estadoSync)) {
                    continue;
                }

                String unitTypeAlert = valorLimpio(s, "unitType", valorLimpio(s, "type", "OTHER"));
                if (!correspondeACorporacion(unitTypeAlert, tipoCorpActiva)) {
                    continue;
                }

                String folio = valorLimpio(s, "folio", valorLimpio(s, "id", "SOL-" + (i + 1)));
                String tipo = valorLimpio(s, "type", "ALERTA");
                String desc = valorLimpio(s, "description", "-");
                String fecha = valorLimpio(s, "date", "-");

                JSONObject adaptada = new JSONObject();
                adaptada.put("dispatchId", "SYNC:" + folio);
                adaptada.put("folio", folio);
                adaptada.put("type", "Folio " + folio + " · " + tipo);
                adaptada.put("description", desc);
                adaptada.put("distance_m", 0.0);
                adaptada.put("expires_at", fecha);
                adaptada.put("esSincronizada", true);
                combinada.put(adaptada);
            }
        } catch (Exception ignored) {
        }

        return combinada;
    }

    private void cargarDespachosPendientes() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.request(this, "GET", "/api/dispatches/pending", null);
                JSONArray items = new JSONArray();
                final String errorApi;

                if (response.isSuccessful()) {
                    items = extraerArregloJson(response.body);
                    errorApi = null;
                } else {
                    errorApi = "Despachos (" + response.code + "): " + response.body;
                }

                final JSONArray listaFinal = combinarConAlertasSincronizadas(items);
                runOnUiThread(() -> {
                    if (errorApi != null && listaFinal.length() == 0) {
                        Toast.makeText(OrganizacionesActivity.this, errorApi, Toast.LENGTH_LONG).show();
                    }
                    pintarDespachosPendientes(listaFinal);
                });
            } catch (Exception e) {
                final JSONArray listaFallback = combinarConAlertasSincronizadas(new JSONArray());
                runOnUiThread(() -> {
                    if (listaFallback.length() == 0) {
                        Toast.makeText(OrganizacionesActivity.this, "Sin conexión al consultar despachos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    pintarDespachosPendientes(listaFallback);
                });
            }
        }).start();
    }

    private void pintarDespachosPendientes(JSONArray lista) {
        if (layoutPendingDispatchesContainer == null || tvEmptyPending == null) return;
        layoutPendingDispatchesContainer.removeAllViews();

        if (lista == null || lista.length() == 0) {
            tvEmptyPending.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyPending.setVisibility(View.GONE);

        for (int i = 0; i < lista.length(); i++) {
            JSONObject item = lista.optJSONObject(i);
            if (item == null) continue;

            JSONObject alertObj = item.optJSONObject("alert");
            final String dispatchId = valorLimpio(item, "dispatchId", valorLimpio(item, "id", ""));
            final String folio = valorLimpio(item, "folio", valorLimpio(alertObj, "folio", ""));
            final boolean esSincronizada = item.optBoolean("esSincronizada", false);
            final String type = valorLimpio(item, "type", valorLimpio(alertObj, "type", "ALERTA"));
            final String description = valorLimpio(item, "description", valorLimpio(alertObj, "description", "-"));
            double distanceM = item.optDouble("distance_m", item.optDouble("distanceMeters", 0.0));
            String expiresAt = valorLimpio(item, "expires_at", valorLimpio(item, "expiresAt", "-"));

            LinearLayout tarjeta = new LinearLayout(this);
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setPadding(24, 24, 24, 24);
            tarjeta.setBackgroundColor(0xFF0F172A);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 20);
            tarjeta.setLayoutParams(params);

            TextView tvTipo = new TextView(this);
            tvTipo.setText(type);
            tvTipo.setTextColor(0xFFFFFFFF);
            tvTipo.setTextSize(14f);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(description);
            tvDesc.setTextColor(0xFFE2E8F0);
            tvDesc.setTextSize(12f);

            TextView tvMeta = new TextView(this);
            if (esSincronizada) {
                tvMeta.setText("Solicitud entrante · Registrada: " + expiresAt);
            } else {
                tvMeta.setText(String.format(Locale.US, "Distancia: %.0f m · Expira: %s", distanceM, expiresAt));
            }
            tvMeta.setTextColor(0xFF94A3B8);
            tvMeta.setTextSize(11f);

            LinearLayout filaBotones = new LinearLayout(this);
            filaBotones.setOrientation(LinearLayout.HORIZONTAL);
            filaBotones.setPadding(0, 14, 0, 0);

            Button btnAceptar = new Button(this);
            btnAceptar.setText("Aceptar");
            btnAceptar.setBackgroundColor(0xFF16A34A);
            btnAceptar.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            btnParams.setMarginEnd(12);
            btnAceptar.setLayoutParams(btnParams);
            btnAceptar.setOnClickListener(v -> responderDespacho(dispatchId, folio, "accept", esSincronizada, type, description));

            Button btnRechazar = new Button(this);
            btnRechazar.setText("Rechazar");
            btnRechazar.setBackgroundColor(0xFFDC2626);
            btnRechazar.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams btnRejectParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            btnRejectParams.setMarginStart(12);
            btnRechazar.setLayoutParams(btnRejectParams);
            btnRechazar.setOnClickListener(v -> responderDespacho(dispatchId, folio, "reject", esSincronizada, type, description));

            filaBotones.addView(btnAceptar);
            filaBotones.addView(btnRechazar);

            tarjeta.addView(tvTipo);
            tarjeta.addView(tvDesc);
            tarjeta.addView(tvMeta);
            tarjeta.addView(filaBotones);

            layoutPendingDispatchesContainer.addView(tarjeta);
        }
    }

    private void actualizarEstadoSync(String folio, String estadoOperador, String estadoUsuario) {
        try {
            SharedPreferences prefsOp = getSharedPreferences(PREFS_SYNC_OPERADOR, MODE_PRIVATE);
            JSONArray listaOp = new JSONArray(prefsOp.getString("alertas_entrantes_json", "[]"));
            for (int i = 0; i < listaOp.length(); i++) {
                JSONObject op = listaOp.optJSONObject(i);
                if (op != null && folio.equals(op.optString("folio", ""))) {
                    op.put("status", estadoOperador);
                    listaOp.put(i, op);
                }
            }
            prefsOp.edit().putString("alertas_entrantes_json", listaOp.toString()).commit();

            SharedPreferences prefsSol = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
            JSONArray listaSol = new JSONArray(prefsSol.getString("historial_json", "[]"));
            for (int i = 0; i < listaSol.length(); i++) {
                JSONObject sol = listaSol.optJSONObject(i);
                if (sol != null && folio.equals(sol.optString("folio", ""))) {
                    sol.put("status", estadoUsuario);
                    listaSol.put(i, sol);
                }
            }
            prefsSol.edit().putString("historial_json", listaSol.toString()).commit();
        } catch (Exception ignored) {
        }
    }

    private void responderDespacho(String dispatchId, String folio, String accion,
                                   boolean esSincronizada, String tipo, String descripcion) {
        final boolean aceptada = "accept".equals(accion);

        if (esSincronizada) {
            actualizarEstadoSync(
                    folio,
                    aceptada ? "ACEPTADA" : "RECHAZADA",
                    aceptada ? "ACEPTADA (EN RUTA)" : "RECHAZADA"
            );
            if (aceptada) {
                alertaActivaId = "SYNC:" + folio;
                alertaActivaFolio = folio;
                alertaActivaTipo = tipo;
                alertaActivaDesc = descripcion;

                registrarEnHistorialUnidad(alertaActivaId, folio, tipo, descripcion, "EN_ROUTE", "acceptedAt");
                actualizarEstadoLocalUnidad("BUSY", "En ruta / Atendiendo");

                JSONObject activa = new JSONObject();
                try {
                    activa.put("id", alertaActivaId);
                    activa.put("folio", folio);
                    activa.put("type", tipo);
                    activa.put("status", "EN_RUTA");
                    activa.put("description", descripcion);
                } catch (Exception ignored) {
                }
                pintarAlertaActiva(activa);
            }
            Toast.makeText(this, aceptada ? "Solicitud " + folio + " aceptada" : "Solicitud " + folio + " rechazada", Toast.LENGTH_SHORT).show();
            refrescarTodo();
            return;
        }

        if (dispatchId == null || dispatchId.isEmpty()) return;
        new Thread(() -> {
            try {
                String endpoint = "/api/dispatches/" + dispatchId + "/" + accion;
                ApiClient.ApiResponse response = ApiClient.request(this, "POST", endpoint, "{}");
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        if (aceptada) {
                            String alertIdResp = "";
                            String folioResp = folio;
                            String estadoResp = "EN_ROUTE";
                            String descResp = descripcion;
                            try {
                                if (response.body != null && response.body.trim().startsWith("{")) {
                                    JSONObject bodyObj = new JSONObject(response.body.trim());
                                    JSONObject alertObj = bodyObj.optJSONObject("alert");
                                    alertIdResp = valorLimpio(bodyObj, "alertId",
                                            valorLimpio(alertObj, "id", valorLimpio(bodyObj, "id", dispatchId)));
                                    folioResp = valorLimpio(bodyObj, "folio", valorLimpio(alertObj, "folio", folio));
                                    estadoResp = valorLimpio(bodyObj, "status", valorLimpio(alertObj, "status", "EN_ROUTE"));
                                    descResp = valorLimpio(bodyObj, "description", valorLimpio(alertObj, "description", descripcion));
                                } else {
                                    alertIdResp = dispatchId;
                                }
                            } catch (Exception ignored) {
                                alertIdResp = dispatchId;
                            }

                            alertaActivaId = alertIdResp;
                            alertaActivaFolio = folioResp;
                            alertaActivaTipo = tipo;
                            alertaActivaDesc = descResp;

                            registrarEnHistorialUnidad(alertIdResp, folioResp, tipo, descResp, estadoResp, "acceptedAt");
                            actualizarEstadoLocalUnidad("BUSY", "En ruta / Atendiendo");

                            JSONObject activa = new JSONObject();
                            try {
                                activa.put("id", alertIdResp);
                                activa.put("folio", folioResp);
                                activa.put("type", tipo);
                                activa.put("status", estadoResp);
                                activa.put("description", descResp);
                            } catch (Exception ignored) {
                            }
                            pintarAlertaActiva(activa);
                        }
                        Toast.makeText(this, "Despacho actualizado (" + accion + ")", Toast.LENGTH_SHORT).show();
                        refrescarTodo();
                    } else {
                        Toast.makeText(this, "Error (" + response.code + "): " + response.body, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Fallo de red: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void cargarAlertaActiva() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.request(this, "GET", "/api/alerts/active", null);
                JSONObject alerta = null;
                final String errorActiva;

                if (response.isSuccessful() && response.body != null && !response.body.trim().isEmpty()) {
                    errorActiva = null;
                    String trimmed = response.body.trim();
                    if (trimmed.startsWith("{")) {
                        JSONObject obj = new JSONObject(trimmed);
                        if (obj.has("id") && !obj.isNull("id")) {
                            alerta = obj;
                        } else if (obj.has("alert") && !obj.isNull("alert")) {
                            alerta = obj.optJSONObject("alert");
                        }
                    } else if (trimmed.startsWith("[")) {
                        JSONArray arr = new JSONArray(trimmed);
                        if (arr.length() > 0) {
                            alerta = arr.optJSONObject(0);
                        }
                    }
                } else if (response.code == 404 || response.code == 403) {
                    errorActiva = null;
                } else {
                    errorActiva = "Alerta activa (" + response.code + "): " + response.body;
                }

                final JSONObject alertaFinal = alerta;
                runOnUiThread(() -> {
                    if (errorActiva != null && alertaFinal == null && alertaActivaId.isEmpty()) {
                        Toast.makeText(OrganizacionesActivity.this, errorActiva, Toast.LENGTH_LONG).show();
                    }
                    if (alertaFinal != null) {
                        pintarAlertaActiva(alertaFinal);
                    } else if (alertaActivaId.isEmpty()) {
                        pintarAlertaActiva(null);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (alertaActivaId.isEmpty()) {
                        pintarAlertaActiva(null);
                    }
                });
            }
        }).start();
    }

    private void pintarAlertaActiva(JSONObject alerta) {
        if (tvNoActiveAlert == null || layoutActiveAlertDetails == null) return;

        if (alerta == null) {
            alertaActivaId = "";
            alertaActivaFolio = "";
            alertaActivaTipo = "-";
            alertaActivaDesc = "-";
            tvNoActiveAlert.setVisibility(View.VISIBLE);
            layoutActiveAlertDetails.setVisibility(View.GONE);
            return;
        }

        alertaActivaId = valorLimpio(alerta, "id", "");
        alertaActivaFolio = valorLimpio(alerta, "folio", "");
        String tipo = valorLimpio(alerta, "type", "-");
        String estado = valorLimpio(alerta, "status", "-");
        String desc = valorLimpio(alerta, "description", "-");
        alertaActivaTipo = tipo;
        alertaActivaDesc = desc;

        double lat = alerta.optDouble("latitude", 0.0);
        double lon = alerta.optDouble("longitude", 0.0);
        String arrivedAt = valorLimpio(alerta, "arrivedAt", "-");
        String resolvedAt = valorLimpio(alerta, "resolvedAt", "-");

        tvNoActiveAlert.setVisibility(View.GONE);
        layoutActiveAlertDetails.setVisibility(View.VISIBLE);

        if (tvActiveAlertType != null) tvActiveAlertType.setText("Tipo: " + tipo);
        if (tvActiveAlertStatus != null) tvActiveAlertStatus.setText("Estado: " + estado);
        if (tvActiveAlertDescription != null) tvActiveAlertDescription.setText("Descripción: " + desc);
        if (tvActiveAlertCoords != null) {
            tvActiveAlertCoords.setText(String.format(Locale.US, "Ubicación: %.6f, %.6f", lat, lon));
        }
        if (tvActiveAlertTimes != null) {
            tvActiveAlertTimes.setText("Llegada: " + arrivedAt + " | Resuelta: " + resolvedAt);
        }
    }

    private void accionAlertaActiva(String accion, String mensajeOk) {
        if (alertaActivaId == null || alertaActivaId.isEmpty()) {
            Toast.makeText(this, "No hay alerta activa para actualizar", Toast.LENGTH_SHORT).show();
            return;
        }

        final String idActual = alertaActivaId;
        final String folioActual = alertaActivaFolio;
        final String tipoActual = alertaActivaTipo;
        final String descActual = alertaActivaDesc;

        if (idActual.startsWith("SYNC:")) {
            if ("arrive".equals(accion)) {
                actualizarEstadoSync(folioActual, "EN_SITIO", "ATENDIENDO EN SITIO");
                registrarEnHistorialUnidad(idActual, folioActual, tipoActual, descActual, "ON_SCENE", "arrivedAt");
                if (tvActiveAlertStatus != null) tvActiveAlertStatus.setText("Estado: EN_SITIO");
            } else if ("resolve".equals(accion)) {
                actualizarEstadoSync(folioActual, "RESUELTA", "RESUELTA");
                registrarEnHistorialUnidad(idActual, folioActual, tipoActual, descActual, "RESOLVED", "resolvedAt");
                actualizarEstadoLocalUnidad("AVAILABLE", "Disponible / En servicio");
                pintarAlertaActiva(null);
            }
            Toast.makeText(this, mensajeOk, Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnArriveAlert != null) btnArriveAlert.setEnabled(false);
        if (btnResolveAlert != null) btnResolveAlert.setEnabled(false);

        new Thread(() -> {
            try {
                String endpoint = "/api/alerts/" + idActual + "/" + accion;
                ApiClient.ApiResponse response = ApiClient.request(this, "POST", endpoint, "{}");
                runOnUiThread(() -> {
                    if (btnArriveAlert != null) btnArriveAlert.setEnabled(true);
                    if (btnResolveAlert != null) btnResolveAlert.setEnabled(true);

                    if (response.isSuccessful()) {
                        if ("arrive".equals(accion)) {
                            registrarEnHistorialUnidad(idActual, folioActual, tipoActual, descActual, "ON_SCENE", "arrivedAt");
                        } else if ("resolve".equals(accion)) {
                            registrarEnHistorialUnidad(idActual, folioActual, tipoActual, descActual, "RESOLVED", "resolvedAt");
                            actualizarEstadoLocalUnidad("AVAILABLE", "Disponible / En servicio");
                        }
                        Toast.makeText(this, mensajeOk, Toast.LENGTH_SHORT).show();
                        refrescarTodo();
                    } else {
                        Toast.makeText(this, "Error (" + response.code + "): " + response.body, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (btnArriveAlert != null) btnArriveAlert.setEnabled(true);
                    if (btnResolveAlert != null) btnResolveAlert.setEnabled(true);
                    Toast.makeText(this, "Fallo de red: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}