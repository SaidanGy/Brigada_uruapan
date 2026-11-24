package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class UserHistorialActivity extends AppCompatActivity {

    private static final String PREFS_SOLICITUDES = "solicitudes_ciudadano_prefs";
    private static final String PREFS_SYNC_OPERADOR = "alerta_despacho_sync_prefs";
    private static final String PREFS_USUARIO = "usuario_sesion_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";

    private ImageButton btnRefreshHistorial;
    private LinearLayout layoutSolicitudesGuardadas;
    private View layoutEstadoVacioHistorial;
    private View scrollSolicitudes;

    // Bucle que consulta al backend el estado de las alertas activas
    // para reflejar en el historial si una unidad las atendió.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean bucleActualizacionActivo;
    private final Runnable bucleHistorial = new Runnable() {
        @Override
        public void run() {
            if (!bucleActualizacionActivo) return;
            cargarAlertaActivaParaHistorial(false);
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_historial);

        btnRefreshHistorial = findViewById(R.id.btnRefreshHistorial);
        layoutSolicitudesGuardadas = findViewById(R.id.layoutSolicitudesGuardadas);
        layoutEstadoVacioHistorial = findViewById(R.id.layoutEstadoVacioHistorial);
        scrollSolicitudes = findViewById(R.id.scrollSolicitudes);

        if (btnRefreshHistorial != null) {
            btnRefreshHistorial.setOnClickListener(v -> refrescarSolicitudesManual());
        }

        cargarSolicitudesGuardadas();

        LinearLayout tabMapa = findViewById(R.id.tabUserMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                startActivity(new Intent(UserHistorialActivity.this, UserMainActivity.class));
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabUserCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                startActivity(new Intent(UserHistorialActivity.this, UserCuentaActivity.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarSolicitudesGuardadas();
        cargarAlertaActivaParaHistorial(false);
        bucleActualizacionActivo = true;
        handler.removeCallbacks(bucleHistorial);
        handler.postDelayed(bucleHistorial, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bucleActualizacionActivo = false;
        handler.removeCallbacks(bucleHistorial);
    }

    @Override
    protected void onDestroy() {
        bucleActualizacionActivo = false;
        handler.removeCallbacks(bucleHistorial);
        super.onDestroy();
    }

    private void refrescarSolicitudesManual() {
        cargarSolicitudesGuardadas();
        cargarAlertaActivaParaHistorial(true);
    }

    private void cargarAlertaActivaParaHistorial(boolean mostrarAvisoManual) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.request(this, "GET", "/api/alerts/active", null);
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        cargarSolicitudesGuardadas();
                        if (mostrarAvisoManual) {
                            Toast.makeText(UserHistorialActivity.this, "Solicitudes actualizadas", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                String body = response.body == null ? "" : response.body.trim();
                JSONObject alerta = null;
                if (body.startsWith("{")) {
                    JSONObject obj = new JSONObject(body);
                    alerta = obj.optJSONObject("alert");
                    if (alerta == null && !obj.isNull("id")) alerta = obj;
                } else if (body.startsWith("[")) {
                    JSONArray arr = new JSONArray(body);
                    if (arr.length() > 0) alerta = arr.optJSONObject(0);
                }

                if (alerta == null) {
                    runOnUiThread(() -> {
                        cargarSolicitudesGuardadas();
                        if (mostrarAvisoManual) {
                            Toast.makeText(UserHistorialActivity.this, "Sin alerta activa en curso; historial actualizado", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                final String folio = valorLimpio(alerta, "folio", "");
                final String id = valorLimpio(alerta, "id", "");
                final String estadoAlerta = valorLimpio(alerta, "status", "");
                final String arrivedAt = valorLimpio(alerta, "arrivedAt", "");
                final String resolvedAt = valorLimpio(alerta, "resolvedAt", "");
                final String nuevoEstado = estadoDesdeAlerta(estadoAlerta, arrivedAt, resolvedAt);

                runOnUiThread(() -> {
                    try {
                        if (!folio.isEmpty() || !id.isEmpty()) {
                            SharedPreferences prefs = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
                            JSONArray lista = new JSONArray(prefs.getString("historial_json", "[]"));
                            boolean cambio = false;
                            for (int i = 0; i < lista.length(); i++) {
                                JSONObject item = lista.optJSONObject(i);
                                if (item == null) continue;
                                boolean coincide = (!folio.isEmpty() && folio.equals(item.optString("folio", "").trim()))
                                        || (!id.isEmpty() && id.equals(item.optString("id", "").trim()));
                                if (!coincide) continue;
                                if (!nuevoEstado.equals(valorLimpio(item, "status", ""))) {
                                    item.put("status", nuevoEstado);
                                    lista.put(i, item);
                                    cambio = true;
                                }
                            }
                            if (cambio) {
                                prefs.edit().putString("historial_json", lista.toString()).apply();
                            }
                        }
                        cargarSolicitudesGuardadas();
                        if (mostrarAvisoManual) {
                            Toast.makeText(UserHistorialActivity.this, "Solicitudes actualizadas", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    cargarSolicitudesGuardadas();
                    if (mostrarAvisoManual) {
                        Toast.makeText(UserHistorialActivity.this, "Historial local actualizado", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private String valorLimpio(JSONObject obj, String key, String fallback) {
        if (obj == null || obj.isNull(key)) return fallback;
        String val = obj.optString(key, fallback);
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return fallback;
        }
        return val.trim();
    }

    private String estadoDesdeAlerta(String estadoAlerta, String arrivedAt, String resolvedAt) {
        if (!resolvedAt.isEmpty() || "RESOLVED".equalsIgnoreCase(estadoAlerta) || "RESUELTA".equalsIgnoreCase(estadoAlerta)) {
            return "RESUELTA";
        }
        if ("EXPIRED".equalsIgnoreCase(estadoAlerta) || "EXPIRADA".equalsIgnoreCase(estadoAlerta)) {
            return "EXPIRADA";
        }
        if (!arrivedAt.isEmpty()
                || "ARRIVED".equalsIgnoreCase(estadoAlerta)
                || "EN_SITIO".equalsIgnoreCase(estadoAlerta)
                || "ON_SCENE".equalsIgnoreCase(estadoAlerta)
                || "ATENDIENDO".equalsIgnoreCase(estadoAlerta)) {
            return "ATENDIENDO EN SITIO";
        }
        if ("ACCEPTED".equalsIgnoreCase(estadoAlerta) || "ACEPTADO".equalsIgnoreCase(estadoAlerta)
                || "ACCEPT".equalsIgnoreCase(estadoAlerta) || "EN_ROUTE".equalsIgnoreCase(estadoAlerta)
                || "EN_RUTA".equalsIgnoreCase(estadoAlerta) || "BUSY".equalsIgnoreCase(estadoAlerta)) {
            return "ACEPTADA (EN RUTA)";
        }
        if ("REJECTED".equalsIgnoreCase(estadoAlerta) || "RECHAZADA".equalsIgnoreCase(estadoAlerta)
                || "REJECT".equalsIgnoreCase(estadoAlerta)) {
            return "RECHAZADA";
        }
        if ("CANCELLED".equalsIgnoreCase(estadoAlerta) || "CANCELED".equalsIgnoreCase(estadoAlerta)
                || "CANCELADA".equalsIgnoreCase(estadoAlerta)) {
            return "CANCELADA";
        }
        return "PENDIENTE";
    }

    private String obtenerCorreoActivo() {
        String correo = getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE).getString("email", "");
        if (correo == null || correo.trim().isEmpty()) {
            correo = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE).getString("email", "");
        }
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private String obtenerFolioLegible(JSONObject item, int index) {
        String folio = item.optString("folio", "").trim();
        if (!folio.isEmpty()) return folio;

        String rawId = item.optString("id", "").trim();
        if (rawId.isEmpty()) return "SOL-" + (1000 + index);
        if (rawId.length() > 10) {
            return "SOL-" + rawId.substring(0, 4).toUpperCase(Locale.ROOT);
        }
        return rawId;
    }

    private boolean perteneceAUsuario(JSONObject item, String correoActivo) {
        if (item == null) return false;
        String owner = item.optString("ownerEmail", "").trim().toLowerCase(Locale.ROOT);
        if (correoActivo.isEmpty()) {
            return owner.isEmpty();
        }
        return correoActivo.equals(owner);
    }

    private void cargarSolicitudesGuardadas() {
        if (layoutSolicitudesGuardadas == null) return;
        layoutSolicitudesGuardadas.removeAllViews();

        int tarjetasMostradas = 0;
        String correoActivo = obtenerCorreoActivo();

        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("historial_json", "[]"));

            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (!perteneceAUsuario(item, correoActivo)) {
                    continue;
                }

                tarjetasMostradas++;
                String id = item.optString("id", "");
                String folio = obtenerFolioLegible(item, i);
                String tipo = item.optString("type", "OTHER");
                String estado = item.optString("status", "PENDIENTE");
                String direccion = item.optString("address", "Sin dirección");
                String fecha = item.optString("date", "");

                CardView card = new CardView(this);
                card.setCardBackgroundColor(Color.parseColor("#0F172A"));
                card.setRadius(dpToPx(16));
                card.setCardElevation(dpToPx(4));

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, dpToPx(16));
                card.setLayoutParams(cardParams);

                LinearLayout contenido = new LinearLayout(this);
                contenido.setOrientation(LinearLayout.VERTICAL);
                contenido.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

                TextView tvFolio = new TextView(this);
                tvFolio.setText("Folio " + folio + "  ·  ● " + estado);
                if ("CANCELADA".equalsIgnoreCase(estado) || "RECHAZADA".equalsIgnoreCase(estado) || "EXPIRADA".equalsIgnoreCase(estado)) {
                    tvFolio.setTextColor(Color.parseColor("#F87171"));
                } else if ("ACEPTADA".equalsIgnoreCase(estado) || "ACEPTADA (EN RUTA)".equalsIgnoreCase(estado)
                        || "ATENDIENDO EN SITIO".equalsIgnoreCase(estado) || "RESUELTA".equalsIgnoreCase(estado)) {
                    tvFolio.setTextColor(Color.parseColor("#34D399"));
                } else {
                    tvFolio.setTextColor(Color.parseColor("#38BDF8"));
                }
                tvFolio.setTextSize(12f);

                TextView tvTitulo = new TextView(this);
                tvTitulo.setText("Solicitud de Brigada (" + tipo + ")");
                tvTitulo.setTextColor(Color.WHITE);
                tvTitulo.setTextSize(15f);
                tvTitulo.setPadding(0, dpToPx(6), 0, 0);

                TextView tvDir = new TextView(this);
                tvDir.setText("Dirección: " + direccion);
                tvDir.setTextColor(Color.parseColor("#94A3B8"));
                tvDir.setTextSize(13f);
                tvDir.setPadding(0, dpToPx(4), 0, 0);

                TextView tvFecha = new TextView(this);
                tvFecha.setText("Fecha: " + fecha);
                tvFecha.setTextColor(Color.parseColor("#64748B"));
                tvFecha.setTextSize(11f);
                tvFecha.setPadding(0, dpToPx(8), 0, 0);

                LinearLayout filaBotones = new LinearLayout(this);
                filaBotones.setOrientation(LinearLayout.HORIZONTAL);
                filaBotones.setPadding(0, dpToPx(12), 0, 0);

                Button btnCancelar = new Button(this);
                btnCancelar.setText("Cancelar");
                btnCancelar.setAllCaps(false);
                btnCancelar.setTextSize(12f);
                btnCancelar.setTextColor(Color.WHITE);
                btnCancelar.setBackgroundColor(Color.parseColor("#D97706"));
                LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                        0,
                        dpToPx(38),
                        1f
                );
                cancelParams.setMarginEnd(dpToPx(8));
                btnCancelar.setLayoutParams(cancelParams);

                boolean yaCancelada = "CANCELADA".equalsIgnoreCase(estado)
                        || "RESUELTA".equalsIgnoreCase(estado)
                        || "EXPIRADA".equalsIgnoreCase(estado);
                btnCancelar.setEnabled(!yaCancelada);
                btnCancelar.setAlpha(yaCancelada ? 0.45f : 1f);
                btnCancelar.setOnClickListener(v -> cancelarSolicitud(id, folio, correoActivo));

                Button btnEliminar = new Button(this);
                btnEliminar.setText("Eliminar");
                btnEliminar.setAllCaps(false);
                btnEliminar.setTextSize(12f);
                btnEliminar.setTextColor(Color.WHITE);
                btnEliminar.setBackgroundColor(Color.parseColor("#DC2626"));
                LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(
                        0,
                        dpToPx(38),
                        1f
                );
                delParams.setMarginStart(dpToPx(8));
                btnEliminar.setLayoutParams(delParams);
                btnEliminar.setOnClickListener(v -> eliminarSolicitud(id, folio, correoActivo));

                filaBotones.addView(btnCancelar);
                filaBotones.addView(btnEliminar);

                contenido.addView(tvFolio);
                contenido.addView(tvTitulo);
                contenido.addView(tvDir);
                contenido.addView(tvFecha);
                contenido.addView(filaBotones);
                card.addView(contenido);

                layoutSolicitudesGuardadas.addView(card);
            }
        } catch (Exception ignored) {
        }

        boolean estaVacia = tarjetasMostradas == 0;
        if (layoutEstadoVacioHistorial != null) {
            layoutEstadoVacioHistorial.setVisibility(estaVacia ? View.VISIBLE : View.GONE);
        }
        if (scrollSolicitudes != null) {
            scrollSolicitudes.setVisibility(estaVacia ? View.GONE : View.VISIBLE);
        }
    }

    private boolean coincideSolicitudDeUsuario(JSONObject item, String id, String folio, String correoActivo) {
        if (item == null) return false;
        if (!perteneceAUsuario(item, correoActivo)) return false;

        String itemFolio = item.optString("folio", "").trim();
        String itemId = item.optString("id", "").trim();
        boolean coincideFolio = !folio.isEmpty() && folio.equals(itemFolio);
        boolean coincideId = !id.isEmpty() && id.equals(itemId);
        return coincideFolio || coincideId;
    }

    private void cancelarSolicitud(String id, String folio, String correoActivo) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("historial_json", "[]"));
            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (coincideSolicitudDeUsuario(item, id, folio, correoActivo)) {
                    item.put("status", "CANCELADA");
                    lista.put(i, item);
                }
            }
            prefs.edit().putString("historial_json", lista.toString()).commit();

            actualizarEstadoEnOperador(id, folio, correoActivo, "CANCELADA");

            if (id != null && !id.isEmpty() && !id.equals(folio)) {
                new Thread(() -> {
                    try {
                        ApiClient.request(this, "POST", "/api/alerts/" + id + "/cancel", "{}");
                    } catch (Exception ignored) {
                    }
                }).start();
            }

            Toast.makeText(this, "Solicitud " + folio + " cancelada", Toast.LENGTH_SHORT).show();
            cargarSolicitudesGuardadas();
        } catch (Exception ignored) {
        }
    }

    private void eliminarSolicitud(String id, String folio, String correoActivo) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("historial_json", "[]"));
            JSONArray nueva = new JSONArray();
            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.optJSONObject(i);
                if (!coincideSolicitudDeUsuario(item, id, folio, correoActivo)) {
                    nueva.put(item);
                }
            }
            prefs.edit().putString("historial_json", nueva.toString()).commit();

            SharedPreferences prefsOp = getSharedPreferences(PREFS_SYNC_OPERADOR, MODE_PRIVATE);
            JSONArray listaOp = new JSONArray(prefsOp.getString("alertas_entrantes_json", "[]"));
            JSONArray nuevaOp = new JSONArray();
            for (int i = 0; i < listaOp.length(); i++) {
                JSONObject op = listaOp.optJSONObject(i);
                if (!coincideSolicitudDeUsuario(op, id, folio, correoActivo)) {
                    nuevaOp.put(op);
                }
            }
            prefsOp.edit().putString("alertas_entrantes_json", nuevaOp.toString()).commit();

            Toast.makeText(this, "Solicitud " + folio + " eliminada", Toast.LENGTH_SHORT).show();
            cargarSolicitudesGuardadas();
        } catch (Exception ignored) {
        }
    }

    private void actualizarEstadoEnOperador(String id, String folio, String correoActivo, String nuevoEstado) {
        try {
            SharedPreferences prefsOp = getSharedPreferences(PREFS_SYNC_OPERADOR, MODE_PRIVATE);
            JSONArray listaOp = new JSONArray(prefsOp.getString("alertas_entrantes_json", "[]"));
            for (int i = 0; i < listaOp.length(); i++) {
                JSONObject op = listaOp.optJSONObject(i);
                if (coincideSolicitudDeUsuario(op, id, folio, correoActivo)) {
                    op.put("status", nuevoEstado);
                    listaOp.put(i, op);
                }
            }
            prefsOp.edit().putString("alertas_entrantes_json", listaOp.toString()).commit();
        } catch (Exception ignored) {
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}