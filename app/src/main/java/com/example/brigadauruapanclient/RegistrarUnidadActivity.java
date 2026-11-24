package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class RegistrarUnidadActivity extends AppCompatActivity {

    private static final String PREFS_URBTRACK = "UrbTrackPrefs";

    private TextView tvCoordenadas;
    private RadioGroup rgEstados;
    private Button btnActualizarGps, btnTransmitir;

    // Coordenadas por defecto en Uruapan, Michoacán
    private double currentLat = 19.4064492;
    private double currentLon = -102.0430476;
    private double currentHeading = 0.0;
    private double currentSpeedKmh = 12.5;
    private String unidadReferenciaId = "";
    private String unidadReferenciaTipo = "";
    private String estadoInicialUnidad = "AVAILABLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registrar_unidad);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCoordenadas = findViewById(R.id.tvCoordenadas);
        rgEstados = findViewById(R.id.rgEstados);
        btnActualizarGps = findViewById(R.id.btnActualizarGps);
        btnTransmitir = findViewById(R.id.btnTransmitir);

        // Recibir coordenadas y datos del marcador tocado en MainActivity
        Intent intentRecibido = getIntent();
        if (intentRecibido != null) {
            double extraLat = intentRecibido.getDoubleExtra("lat", Double.NaN);
            double extraLon = intentRecibido.getDoubleExtra("lon", Double.NaN);
            String extraId = intentRecibido.getStringExtra("unit_id");
            String extraTipo = intentRecibido.getStringExtra("unit_type");
            String extraStatus = intentRecibido.getStringExtra("status");

            if (!Double.isNaN(extraLat) && !Double.isNaN(extraLon)) {
                currentLat = extraLat;
                currentLon = extraLon;
            }
            if (extraId != null && !extraId.isEmpty()) {
                unidadReferenciaId = extraId;
            }
            if (extraTipo != null && !extraTipo.isEmpty()) {
                unidadReferenciaTipo = extraTipo;
            }
            if (extraStatus != null && !extraStatus.isEmpty()) {
                estadoInicialUnidad = extraStatus;
            }
        }

        actualizarTextoCoordenadas();
        actualizarColorBotonEstado();

        if (rgEstados != null) {
            rgEstados.setOnCheckedChangeListener((group, checkedId) -> actualizarColorBotonEstado());
        }
        actualizarColorBotonEstado();

        if (!unidadReferenciaId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Punto sincronizado: " + unidadReferenciaId + (unidadReferenciaTipo.isEmpty() ? "" : " (" + unidadReferenciaTipo + ")"),
                    Toast.LENGTH_SHORT
            ).show();
        }

        // Refrescar / simular pequeño desplazamiento GPS en Uruapan
        if (btnActualizarGps != null) {
            btnActualizarGps.setOnClickListener(v -> {
                currentLat += 0.0004;
                currentLon -= 0.0003;
                currentHeading = (currentHeading + 15.0) % 360.0;
                actualizarTextoCoordenadas();
                Toast.makeText(this, "Posición GPS actualizada", Toast.LENGTH_SHORT).show();
            });
        }

        // Transmitir ubicación y estado al backend
        if (btnTransmitir != null) {
            btnTransmitir.setOnClickListener(v -> {
                actualizarColorBotonEstado();
                enviarUbicacionYEstado();
            });
        }

        // NAVEGACIÓN INFERIOR
        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                startActivity(new Intent(RegistrarUnidadActivity.this, MainActivity.class));
                finish();
            });
        }

        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                startActivity(new Intent(RegistrarUnidadActivity.this, OrganizacionesActivity.class));
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                startActivity(new Intent(RegistrarUnidadActivity.this, CuentaActivity.class));
                finish();
            });
        }
    }

    private void actualizarTextoCoordenadas() {
        if (tvCoordenadas != null) {
            String prefijo = unidadReferenciaId.isEmpty() ? "" : unidadReferenciaId + " · ";
            String texto = String.format(Locale.US, "%sLat: %.4f, Lon: %.4f", prefijo, currentLat, currentLon);
            tvCoordenadas.setText(texto);
        }
    }

    private void actualizarColorBotonEstado() {
        if (btnTransmitir != null) {
            btnTransmitir.setBackgroundTintList(ColorStateList.valueOf(obtenerColorEstado()));
        }
    }

    private int obtenerColorEstado() {
        int checkedId = rgEstados != null ? rgEstados.getCheckedRadioButtonId() : -1;
        if (checkedId == R.id.rbDisponible) {
            return 0xFF16A34A; // Verde (Disponible)
        } else if (checkedId == R.id.rbEnRuta) {
            return 0xFF0284C7; // Azul claro (En ruta)
        } else if (checkedId == R.id.rbEnPunto) {
            return 0xFF2563EB; // Azul (En el punto)
        } else if (checkedId == R.id.rbIncidente) {
            return 0xFFDC2626; // Rojo (Incidente)
        }
        return 0xFF16A34A;
    }

    private String obtenerEtiquetaEstadoUI() {
        int checkedId = rgEstados != null ? rgEstados.getCheckedRadioButtonId() : -1;
        if (checkedId == R.id.rbDisponible) {
            return "Disponible / En servicio";
        } else if (checkedId == R.id.rbEnRuta) {
            return "En ruta";
        } else if (checkedId == R.id.rbEnPunto) {
            return "En el punto";
        } else if (checkedId == R.id.rbIncidente) {
            return "Incidente / Emergencia";
        }
        return "Disponible / En servicio";
    }

    private String obtenerEstadoBackendSeleccionado() {
        int checkedId = rgEstados != null ? rgEstados.getCheckedRadioButtonId() : -1;
        if (checkedId == R.id.rbEnRuta || checkedId == R.id.rbEnPunto || checkedId == R.id.rbIncidente) {
            return "BUSY";
        }
        return "AVAILABLE";
    }
    private String ahoraIsoUtc() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private void guardarEstadoLocalParaMapa(String estadoBackend, String etiquetaEstado) {
        getSharedPreferences("brigada_estado_prefs", MODE_PRIVATE)
                .edit()
                .putString("ultimo_unit_id", unidadReferenciaId)
                .putString("ultimo_estado", estadoBackend)
                .putString("ultimo_estado_texto", etiquetaEstado)
                .putInt("ultimo_color", obtenerColorEstado())
                .putString("ultima_lat", String.valueOf(currentLat))
                .putString("ultima_lon", String.valueOf(currentLon))
                .apply();
    }

    private boolean tieneSesionActiva() {
        String token = ApiClient.getToken(this);
        if (token != null && !token.trim().isEmpty()) {
            return true;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        String email = prefs.getString("email", "");
        String role = prefs.getString("role", "");
        return !email.trim().isEmpty()
                || "UNIT".equalsIgnoreCase(role)
                || "OPERATOR".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role);
    }

    private void enviarUbicacionYEstado() {
        if (!tieneSesionActiva()) {
            Toast.makeText(this, "Inicia sesión primero con una cuenta de unidad", Toast.LENGTH_LONG).show();
            return;
        }

        if (btnTransmitir != null) {
            btnTransmitir.setEnabled(false);
        }
        Toast.makeText(this, "Transmitiendo ubicación...", Toast.LENGTH_SHORT).show();

        String estadoBackend = obtenerEstadoBackendSeleccionado();
        String etiquetaEstado = obtenerEtiquetaEstadoUI();

        new Thread(() -> {
            try {
                // 1. Si el usuario eligió un estado en el RadioGroup, actualizar /api/units/me/status
                if (estadoBackend != null) {
                    JSONObject statusBody = new JSONObject();
                    statusBody.put("status", estadoBackend);
                    ApiClient.request(this, "PUT", "/api/units/me/status", statusBody.toString());
                }

                // 2. Enviar ubicación a POST /api/units/me/location
                JSONObject locationBody = new JSONObject();
                locationBody.put("eventId", UUID.randomUUID().toString());
                locationBody.put("latitude", currentLat);
                locationBody.put("longitude", currentLon);
                locationBody.put("heading", currentHeading);
                locationBody.put("speedKmh", currentSpeedKmh);
                locationBody.put("recordedAt", ahoraIsoUtc());

                ApiClient.ApiResponse response = ApiClient.request(
                        this,
                        "POST",
                        "/api/units/me/location",
                        locationBody.toString()
                );

                runOnUiThread(() -> {
                    if (btnTransmitir != null) {
                        btnTransmitir.setEnabled(true);
                    }
                    if (response.isSuccessful()) {
                        guardarEstadoLocalParaMapa(estadoBackend, etiquetaEstado);

                        Toast.makeText(
                                RegistrarUnidadActivity.this,
                                String.format(Locale.US, "Guardado (%s) en %.4f, %.4f", etiquetaEstado, currentLat, currentLon),
                                Toast.LENGTH_LONG
                        ).show();

                        Intent volverMapa = new Intent(RegistrarUnidadActivity.this, MainActivity.class);
                        if (!unidadReferenciaId.isEmpty()) {
                            volverMapa.putExtra("unit_id", unidadReferenciaId);
                        }
                        if (!unidadReferenciaTipo.isEmpty()) {
                            volverMapa.putExtra("unit_type", unidadReferenciaTipo);
                        }
                        volverMapa.putExtra("status", estadoBackend);
                        volverMapa.putExtra("lat", currentLat);
                        volverMapa.putExtra("lon", currentLon);
                        startActivity(volverMapa);
                        finish();
                    } else {
                        Toast.makeText(
                                RegistrarUnidadActivity.this,
                                "Error al transmitir (" + response.code + "): " + response.body,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (btnTransmitir != null) {
                        btnTransmitir.setEnabled(true);
                    }
                    Toast.makeText(
                            RegistrarUnidadActivity.this,
                            "Fallo de red: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }
}