package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String LOGIN_URL = "http://192.168.1.7:8080/api/auth/login";
    private static final String PREFS_NAME = "UrbTrackPrefs";
    private static final String PREFS_USUARIO = "usuario_sesion_prefs";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvIrARegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvIrARegistro = findViewById(R.id.tvIrARegistro);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            iniciarSesionEnBackend(email, password);
        });

        if (tvIrARegistro != null) {
            tvIrARegistro.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }

    private void iniciarSesionEnBackend(String email, String password) {
        btnLogin.setEnabled(false);
        Toast.makeText(this, "Conectando con el servidor...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(LOGIN_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                InputStream stream = (responseCode >= 200 && responseCode < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder responseBuilder = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                    }
                }

                String responseBody = responseBuilder.toString();

                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    if (responseCode >= 200 && responseCode < 300) {
                        manejarLoginExitoso(responseBody, email);
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                "Error de acceso (" + responseCode + "): verifica correo o contraseña",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                Log.e("URBTRACK_LOGIN", "Fallo al conectar: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(
                            LoginActivity.this,
                            "No se pudo conectar a " + LOGIN_URL + " -> " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void manejarLoginExitoso(String responseBody, String fallbackEmail) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONObject payload = (json.has("data") && json.optJSONObject("data") != null)
                    ? json.optJSONObject("data")
                    : json;

            String accessToken = json.optString("accessToken", json.optString("token", json.optString("jwt", "")));
            if (accessToken.isEmpty() && payload != null) {
                accessToken = payload.optString("accessToken", payload.optString("token", payload.optString("jwt", "")));
            }

            String role = payload != null
                    ? payload.optString("role", json.optString("role", "CITIZEN")).toUpperCase(Locale.ROOT)
                    : "CITIZEN";
            String userId = payload != null ? payload.optString("userId", json.optString("userId", "")) : "";
            String email = payload != null ? payload.optString("email", json.optString("email", fallbackEmail)) : fallbackEmail;
            if (email == null || email.trim().isEmpty()) {
                email = fallbackEmail;
            }
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("accessToken", accessToken)
                    .putString("role", role)
                    .putString("userId", userId)
                    .putString("email", email)
                    .apply();

            SharedPreferences prefsUsuario = getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE);
            String emailAnteriorUsuario = prefsUsuario.getString("email", "");
            SharedPreferences.Editor userEditor = prefsUsuario.edit();
            userEditor.putString("email", email);
            if (!emailNormalizado.equalsIgnoreCase(emailAnteriorUsuario)) {
                userEditor.remove("full_name");
                userEditor.remove("phone");
            }
            if (payload != null) {
                String fullName = payload.optString("fullName", "");
                String phone = payload.optString("phone", "");
                if (!fullName.isEmpty()) userEditor.putString("full_name", fullName);
                if (!phone.isEmpty()) userEditor.putString("phone", phone);
            }
            userEditor.apply();

            // Limpia el archivo plano de organización anterior para no arrastrar datos de otra corporación
            getSharedPreferences("organizaciones_uruapan_prefs", MODE_PRIVATE).edit().clear().apply();

            // Guarda en caché por correo los datos de organización/unidad si el backend los envía
            if (payload != null && !emailNormalizado.isEmpty()) {
                SharedPreferences.Editor opCacheEditor = getSharedPreferences("perfil_operador_cache", MODE_PRIVATE).edit();
                String fullName = payload.optString("fullName", "");
                String phone = payload.optString("phone", "");
                String orgName = payload.optString("organizationName", payload.optString("identifier", ""));
                String orgAddress = payload.optString("organizationAddress", "");
                String unitType = payload.optString("unitType", "");
                String unitId = payload.optString("unitId", "");

                if (!fullName.isEmpty()) opCacheEditor.putString("responsable_" + emailNormalizado, fullName);
                if (!phone.isEmpty()) opCacheEditor.putString("telefono_" + emailNormalizado, phone);
                if (!orgName.isEmpty()) opCacheEditor.putString("corporacion_" + emailNormalizado, orgName);
                if (!orgAddress.isEmpty()) opCacheEditor.putString("direccion_" + emailNormalizado, orgAddress);
                if (!unitType.isEmpty()) opCacheEditor.putString("unit_type_" + emailNormalizado, unitType);
                if (!unitId.isEmpty()) opCacheEditor.putString("matricula_" + emailNormalizado, unitId);
                opCacheEditor.apply();
            }

            Toast.makeText(this, "Sesión iniciada (" + role + ")", Toast.LENGTH_SHORT).show();

            Intent intent;
            if ("UNIT".equals(role) || "ADMIN".equals(role) || "OPERATOR".equals(role)) {
                intent = new Intent(LoginActivity.this, MainActivity.class);
            } else {
                intent = new Intent(LoginActivity.this, UserMainActivity.class);
            }
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Respuesta recibida pero error al leer JSON", Toast.LENGTH_LONG).show();
        }
    }
}