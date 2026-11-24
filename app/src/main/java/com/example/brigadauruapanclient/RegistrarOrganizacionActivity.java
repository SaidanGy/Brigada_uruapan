package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.Locale;

public class RegistrarOrganizacionActivity extends AppCompatActivity {

    private static final String PREFS_ORG = "organizaciones_uruapan_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";
    private static final String PREFS_OPERADOR_CACHE = "perfil_operador_cache";

    private EditText etNombre, etResponsable, etTelefono, etCorreo, etDireccion, etPassword;
    private Button btnRegistrar;
    private TextView tvVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_organizacion);

        etNombre = findViewById(R.id.etOrgNombre);
        etResponsable = findViewById(R.id.etOrgResponsable);
        etTelefono = findViewById(R.id.etOrgTelefono);
        etCorreo = findViewById(R.id.etOrgCorreo);
        etDireccion = findViewById(R.id.etOrgDireccion);
        etPassword = findViewById(R.id.etOrgPassword);
        btnRegistrar = findViewById(R.id.btnRegistrarOrganizacion);
        tvVolver = findViewById(R.id.tvVolverRegistroUsuario);

        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
            String responsable = etResponsable != null ? etResponsable.getText().toString().trim() : "";
            String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
            String correoIngresado = etCorreo != null ? etCorreo.getText().toString().trim() : "";
            String direccion = etDireccion != null ? etDireccion.getText().toString().trim() : "";
            String password = etPassword != null ? etPassword.getText().toString().trim() : "";

            if (nombre.isEmpty() || responsable.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            registrarUnidadOrganizacion(nombre, responsable, telefono, correoIngresado, direccion, password);
        });

        if (tvVolver != null) {
            tvVolver.setOnClickListener(v -> finish());
        }
    }

    private String detectarUnitType(String nombre) {
        String lower = nombre.toLowerCase(Locale.ROOT);
        if (lower.contains("bomber") || lower.contains("fire")) {
            return "FIRE";
        }
        if (lower.contains("ambulanc") || lower.contains("medic") || lower.contains("cruz")) {
            return "AMBULANCE";
        }
        if (lower.contains("polic") || lower.contains("seguridad") || lower.contains("tránsito") || lower.contains("transito")) {
            return "POLICE";
        }
        return "CIVIL_PROTECTION";
    }

    private String resolverCorreoOrganizacion(String nombre, String correoIngresado, String direccion) {
        if (correoIngresado != null && !correoIngresado.isEmpty()) {
            return correoIngresado;
        }
        if (direccion != null && direccion.contains("@")) {
            return direccion.trim();
        }
        String baseSlug = nombre.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (baseSlug.isEmpty()) {
            baseSlug = "unidad";
        }
        return baseSlug + "@uruapan.org";
    }

    private void guardarPerfilPorCorreo(String correo, String nombre, String responsable, String telefono,
                                        String direccion, String unitType, String unitId, String userId) {
        String key = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return;

        getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE)
                .edit()
                .putString("corporacion_" + key, nombre)
                .putString("responsable_" + key, responsable)
                .putString("telefono_" + key, telefono)
                .putString("direccion_" + key, direccion)
                .putString("unit_type_" + key, unitType)
                .putString("matricula_" + key, unitId != null ? unitId : "")
                .putString("user_id_" + key, userId != null ? userId : "")
                .apply();
    }

    private void registrarUnidadOrganizacion(
            String nombre,
            String responsable,
            String telefono,
            String correoIngresado,
            String direccion,
            String password
    ) {
        if (btnRegistrar != null) {
            btnRegistrar.setEnabled(false);
        }

        String correoParaApi = resolverCorreoOrganizacion(nombre, correoIngresado, direccion);
        String unitType = detectarUnitType(nombre);

        Toast.makeText(this, "Registrando con correo: " + correoParaApi, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", correoParaApi);
                body.put("password", password);
                body.put("fullName", responsable);
                body.put("phone", telefono);
                body.put("unitType", unitType);
                body.put("identifier", nombre);
                body.put("organizationName", nombre);
                body.put("organizationPhone", telefono);
                body.put("organizationAddress", direccion);

                ApiClient.ApiResponse response = ApiClient.request(
                        this,
                        "POST",
                        "/api/auth/register/unit",
                        body.toString()
                );

                runOnUiThread(() -> {
                    if (btnRegistrar != null) {
                        btnRegistrar.setEnabled(true);
                    }
                    if (response.isSuccessful()) {
                        String userId = "";
                        String unitId = "";
                        String unitStatus = "AVAILABLE";
                        String role = "UNIT";
                        String token = "";

                        try {
                            if (response.body != null && !response.body.trim().isEmpty()) {
                                JSONObject respJson = new JSONObject(response.body);
                                userId = respJson.optString("userId", "");
                                unitId = respJson.optString("unitId", "");
                                unitStatus = respJson.optString("unitStatus", "AVAILABLE");
                                role = respJson.optString("role", "UNIT");
                                token = respJson.optString("accessToken", respJson.optString("token", ""));
                            }
                        } catch (Exception ignored) {
                        }

                        guardarPerfilPorCorreo(correoParaApi, nombre, responsable, telefono, direccion, unitType, unitId, userId);

                        SharedPreferences.Editor orgEditor = getSharedPreferences(PREFS_ORG, MODE_PRIVATE).edit();
                        orgEditor.putString("nombre_org", nombre);
                        orgEditor.putString("responsable_org", responsable);
                        orgEditor.putString("telefono_org", telefono);
                        orgEditor.putString("direccion_org", direccion);
                        orgEditor.putString("email_org", correoParaApi);
                        orgEditor.putString("unit_type", unitType);
                        orgEditor.putString("user_id", userId);
                        orgEditor.putString("unit_id", unitId);
                        orgEditor.putString("unit_status", unitStatus);
                        orgEditor.putString("role", role);
                        orgEditor.apply();

                        SharedPreferences.Editor sessionEditor = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE).edit();
                        sessionEditor.putString("email", correoParaApi);
                        sessionEditor.putString("role", role);
                        if (!userId.isEmpty()) sessionEditor.putString("userId", userId);
                        if (!token.isEmpty()) sessionEditor.putString("accessToken", token);
                        sessionEditor.apply();

                        Toast.makeText(
                                RegistrarOrganizacionActivity.this,
                                "¡Organización registrada! Correo: " + correoParaApi,
                                Toast.LENGTH_LONG
                        ).show();

                        Intent intent = new Intent(RegistrarOrganizacionActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(
                                RegistrarOrganizacionActivity.this,
                                "Error al registrar (" + response.code + "): " + response.body,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (btnRegistrar != null) {
                        btnRegistrar.setEnabled(true);
                    }
                    Toast.makeText(
                            RegistrarOrganizacionActivity.this,
                            "Fallo de red: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }
}