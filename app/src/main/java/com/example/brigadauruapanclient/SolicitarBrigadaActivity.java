package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SolicitarBrigadaActivity extends AppCompatActivity {

    private static final double DEFAULT_LAT = 19.4064492;
    private static final double DEFAULT_LON = -102.0430476;

    private static final String PREFS_USUARIO = "usuario_sesion_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";
    private static final String PREFS_PERFIL_CACHE = "perfil_ciudadano_cache";
    private static final String PREFS_SOLICITUDES = "solicitudes_ciudadano_prefs";
    private static final String PREFS_SYNC_OPERADOR = "alerta_despacho_sync_prefs";

    private static final String[] OPCIONES_UNIDAD = {
            "Bomberos (Incendio / Rescate)",
            "Ambulancia (Emergencia Médica)",
            "Policía (Seguridad / Robo)",
            "Protección Civil (Accidente / Riesgo)",
            "Otra Brigada / Apoyo General"
    };

    private Spinner spTipoUnidad;
    private EditText etNombre, etTelefono, etDireccion, etReferencias;
    private Button btnConfirmar;

    private double targetLat = DEFAULT_LAT;
    private double targetLon = DEFAULT_LON;
    private String unidadSeleccionadaId = "";
    private String unidadSeleccionadaTipo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitar_brigada);

        spTipoUnidad = findViewById(R.id.spTipoUnidad);
        etNombre = findViewById(R.id.etNombreSolicitante);
        etTelefono = findViewById(R.id.etTelefonoSolicitante);
        etDireccion = findViewById(R.id.etDireccionDestino);
        etReferencias = findViewById(R.id.etReferencias);
        btnConfirmar = findViewById(R.id.btnConfirmarSolicitud);

        leerDatosIntent();
        configurarSpinnerTipoUnidad();
        precargarDatosCiudadano();

        btnConfirmar.setOnClickListener(v -> {
            String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
            String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
            String direccion = etDireccion != null ? etDireccion.getText().toString().trim() : "";
            String referencias = etReferencias != null ? etReferencias.getText().toString().trim() : "";

            if (nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            enviarAlertaAlBackend(nombre, telefono, direccion, referencias);
        });
    }

    private void configurarSpinnerTipoUnidad() {
        if (spTipoUnidad == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                OPCIONES_UNIDAD
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                    ((TextView) view).setTextSize(14f);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#1E293B"));
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                    ((TextView) view).setTextSize(14f);
                    view.setPadding(32, 28, 32, 28);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipoUnidad.setAdapter(adapter);

        String tipoUpper = unidadSeleccionadaTipo.toUpperCase(Locale.ROOT);
        if (tipoUpper.contains("FIRE") || tipoUpper.contains("BOMBER")) {
            spTipoUnidad.setSelection(0);
        } else if (tipoUpper.contains("AMBULANC") || tipoUpper.contains("MEDIC")) {
            spTipoUnidad.setSelection(1);
        } else if (tipoUpper.contains("POLIC")) {
            spTipoUnidad.setSelection(2);
        } else if (tipoUpper.contains("CIVIL")) {
            spTipoUnidad.setSelection(3);
        } else {
            spTipoUnidad.setSelection(0);
        }
    }

    private void leerDatosIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double extraLat = intent.getDoubleExtra("lat", Double.NaN);
        double extraLon = intent.getDoubleExtra("lon", Double.NaN);
        if (!Double.isNaN(extraLat) && !Double.isNaN(extraLon)) {
            targetLat = extraLat;
            targetLon = extraLon;
        }

        String extraId = intent.getStringExtra("unit_id");
        String extraTipo = intent.getStringExtra("unit_type");
        if (extraId != null) unidadSeleccionadaId = extraId;
        if (extraTipo != null) unidadSeleccionadaTipo = extraTipo;
    }

    private void precargarDatosCiudadano() {
        SharedPreferences prefsUsuario = getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE);
        SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        SharedPreferences prefsCache = getSharedPreferences(PREFS_PERFIL_CACHE, MODE_PRIVATE);

        String correo = prefsUsuario.getString("email", "");
        if (correo.isEmpty()) correo = prefsLogin.getString("email", "");
        String correoKey = correo.trim().toLowerCase(Locale.ROOT);

        String nombre = prefsUsuario.getString("full_name", "");
        if (nombre.isEmpty() && !correoKey.isEmpty()) {
            nombre = prefsCache.getString("nombre_" + correoKey, "");
        }

        String telefono = prefsUsuario.getString("phone", "");
        if (telefono.isEmpty() && !correoKey.isEmpty()) {
            telefono = prefsCache.getString("telefono_" + correoKey, "");
        }

        if (etNombre != null && etNombre.getText().toString().trim().isEmpty() && !nombre.isEmpty()) {
            etNombre.setText(nombre);
        }
        if (etTelefono != null && etTelefono.getText().toString().trim().isEmpty() && !telefono.isEmpty()) {
            etTelefono.setText(telefono);
        }
    }

    private String obtenerTipoAlertaSeleccionado() {
        int pos = spTipoUnidad != null ? spTipoUnidad.getSelectedItemPosition() : 0;
        switch (pos) {
            case 0:
                return "FIRE";
            case 1:
                return "MEDICAL";
            case 2:
                return "ROBBERY";
            case 3:
                return "ACCIDENT";
            default:
                return "OTHER";
        }
    }

    private void guardarOActualizarSolicitud(String backendId, String folioCorto, String tipo, String etiquetaUnidad,
                                             String corporation, String nombre, String telefono, String direccion, String descripcion,
                                             String estado) {
        try {
            String fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            SharedPreferences prefsUsuario = getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE);
            SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
            String correoOwner = prefsUsuario.getString("email", "");
            if (correoOwner.isEmpty()) correoOwner = prefsLogin.getString("email", "");

            JSONObject item = new JSONObject();
            item.put("id", (backendId != null && !backendId.isEmpty()) ? backendId : folioCorto);
            item.put("folio", folioCorto);
            item.put("type", tipo + " · " + etiquetaUnidad);
            item.put("unitType", corporation);
            item.put("status", estado);
            item.put("applicantName", nombre);
            item.put("phone", telefono);
            item.put("address", direccion);
            item.put("description", descripcion);
            item.put("date", fechaHora);
            item.put("latitude", targetLat);
            item.put("longitude", targetLon);
            item.put("ownerEmail", correoOwner.trim().toLowerCase(Locale.ROOT));

            // Historial del ciudadano
            SharedPreferences prefs = getSharedPreferences(PREFS_SOLICITUDES, MODE_PRIVATE);
            JSONArray lista = new JSONArray(prefs.getString("historial_json", "[]"));
            JSONArray nuevaLista = new JSONArray();
            nuevaLista.put(item);
            for (int i = 0; i < lista.length() && nuevaLista.length() < 30; i++) {
                JSONObject prev = lista.optJSONObject(i);
                if (prev != null && !folioCorto.equals(prev.optString("folio", ""))) {
                    nuevaLista.put(prev);
                }
            }
            prefs.edit().putString("historial_json", nuevaLista.toString()).commit();

            // Bandeja de respaldo sincronizada para el operador
            SharedPreferences prefsOp = getSharedPreferences(PREFS_SYNC_OPERADOR, MODE_PRIVATE);
            JSONArray listaOp = new JSONArray(prefsOp.getString("alertas_entrantes_json", "[]"));
            JSONArray nuevaOp = new JSONArray();
            nuevaOp.put(item);
            for (int i = 0; i < listaOp.length() && nuevaOp.length() < 30; i++) {
                JSONObject prevOp = listaOp.optJSONObject(i);
                if (prevOp != null && !folioCorto.equals(prevOp.optString("folio", ""))) {
                    nuevaOp.put(prevOp);
                }
            }
            prefsOp.edit().putString("alertas_entrantes_json", nuevaOp.toString()).commit();
        } catch (Exception e) {
            Toast.makeText(this, "Error guardando historial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String obtenerCorporationParaAlerta() {
        int pos = spTipoUnidad != null ? spTipoUnidad.getSelectedItemPosition() : 0;
        switch (pos) {
            case 0: return "FIRE";             // Bombeeros (Incendio / Rescate)
            case 1: return "AMBULANCE";        // Ambulancia (Emergencia Médica)
            case 2: return "POLICE";           // Policía (Seguridad / Robo)
            case 3: return "CIVIL_PROTECTION"; // Protección Civil (Accidente / Riesgo)
            default: return "OTHER";           // Otra Brigada / Apoyo General
        }
    }

    private void enviarAlertaAlBackend(String nombre, String telefono, String direccion, String referencias) {
        String tipoAlerta = obtenerTipoAlertaSeleccionado();
        String corporationParaAlerta = obtenerCorporationParaAlerta();
        String folioCorto = SimuladorCompartidoUruapan.generarFolioCorto(tipoAlerta);
        String etiquetaUnidad = spTipoUnidad != null && spTipoUnidad.getSelectedItem() != null
                ? spTipoUnidad.getSelectedItem().toString()
                : tipoAlerta;

        StringBuilder descripcion = new StringBuilder();
        descripcion.append("Folio: ").append(folioCorto);
        descripcion.append(" | Unidad: ").append(etiquetaUnidad);
        descripcion.append(" | Solicitante: ").append(nombre);
        descripcion.append(" | Tel: ").append(telefono);
        descripcion.append(" | Dirección: ").append(direccion);
        if (!referencias.isEmpty()) {
            descripcion.append(" | Ref: ").append(referencias);
        }

        // Guarda de inmediato en historial y bandeja de operador
        guardarOActualizarSolicitud(
                "", folioCorto, tipoAlerta, etiquetaUnidad, corporationParaAlerta,
                nombre, telefono, direccion, descripcion.toString(),
                "PENDIENTE"
        );

        String token = ApiClient.getToken(this);
        if (token == null || token.isEmpty()) {
            Toast.makeText(
                    this,
                    "Guardada con folio " + folioCorto + " (sin sesión activa en servidor)",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        if (btnConfirmar != null) {
            btnConfirmar.setEnabled(false);
        }
        Toast.makeText(this, "Enviando solicitud...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("type", tipoAlerta);
                body.put("unitType", corporationParaAlerta);
                body.put("description", descripcion.toString());
                body.put("latitude", targetLat);
                body.put("longitude", targetLon);

                ApiClient.ApiResponse response = ApiClient.request(
                        this,
                        "POST",
                        "/api/alerts",
                        body.toString()
                );

                runOnUiThread(() -> {
                    if (btnConfirmar != null) {
                        btnConfirmar.setEnabled(true);
                    }
                    if (response.isSuccessful()) {
                        String alertId = "";
                        try {
                            if (response.body != null && !response.body.trim().isEmpty()) {
                                JSONObject jsonResp = new JSONObject(response.body);
                                alertId = jsonResp.optString("id", "");
                            }
                        } catch (Exception ignored) {
                        }

                        guardarOActualizarSolicitud(
                                alertId, folioCorto, tipoAlerta, etiquetaUnidad, corporationParaAlerta,
                                nombre, telefono, direccion, descripcion.toString(),
                                "PENDIENTE"
                        );

                        Toast.makeText(
                                SolicitarBrigadaActivity.this,
                                "Solicitud enviada · Folio: " + folioCorto,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        guardarOActualizarSolicitud(
                                "", folioCorto, tipoAlerta, etiquetaUnidad, corporationParaAlerta,
                                nombre, telefono, direccion, descripcion.toString(),
                                "PENDIENTE (LOCAL)"
                        );
                        Toast.makeText(
                                SolicitarBrigadaActivity.this,
                                "Guardada con folio " + folioCorto + " (Servidor " + response.code + ")",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (btnConfirmar != null) {
                        btnConfirmar.setEnabled(true);
                    }
                    guardarOActualizarSolicitud(
                            "", folioCorto, tipoAlerta, etiquetaUnidad, corporationParaAlerta,
                            nombre, telefono, direccion, descripcion.toString(),
                            "PENDIENTE (LOCAL)"
                    );
                    Toast.makeText(
                            SolicitarBrigadaActivity.this,
                            "Guardada con folio " + folioCorto + " (sin red al servidor)",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
            }
        }).start();
    }
}