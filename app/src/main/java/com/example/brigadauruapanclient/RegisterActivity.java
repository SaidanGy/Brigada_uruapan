package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private static final String PREFS_USUARIO = "usuario_sesion_prefs";

    private EditText etNombre, etCorreo, etTelefono, etPassword;
    private Button btnRegistrar;
    private TextView tvVolverLogin, tvIrRegistroOrganizacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etRegisterNombre);
        etCorreo = findViewById(R.id.etRegisterCorreo);
        etTelefono = findViewById(R.id.etRegisterTelefono);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegistrar = findViewById(R.id.btnRegistrarCuenta);
        tvVolverLogin = findViewById(R.id.tvVolverLogin);
        tvIrRegistroOrganizacion = findViewById(R.id.tvIrRegistroOrganizacion);

        // 1. ACCIÓN AL REGISTRARSE COMO USUARIO CIUDADANO
        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String correo = etCorreo.getText().toString().trim();
            String telefono = etTelefono.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa nombre, correo y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            registrarCiudadanoEnBackend(nombre, correo, telefono, password);
        });

        // 2. ENLACE PARA IR AL REGISTRO DE ORGANIZACIÓN / BRIGADA
        if (tvIrRegistroOrganizacion != null) {
            tvIrRegistroOrganizacion.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, RegistrarOrganizacionActivity.class);
                startActivity(intent);
            });
        }

        // 3. VOLVER A LOGIN
        tvVolverLogin.setOnClickListener(v -> finish());
    }

    private void registrarCiudadanoEnBackend(String nombre, String correo, String telefono, String password) {
        btnRegistrar.setEnabled(false);
        Toast.makeText(this, "Creando cuenta de usuario...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", correo);
                body.put("password", password);
                body.put("fullName", nombre);
                body.put("phone", telefono);

                ApiClient.ApiResponse response = ApiClient.request(
                        this,
                        "POST",
                        "/api/auth/register/citizen",
                        body.toString()
                );

                runOnUiThread(() -> {
                    btnRegistrar.setEnabled(true);
                    if (response.isSuccessful()) {
                        String userId = "";
                        String role = "CITIZEN";
                        try {
                            JSONObject respJson = new JSONObject(response.body);
                            userId = respJson.optString("userId", "");
                            role = respJson.optString("role", "CITIZEN");
                        } catch (Exception ignored) {
                        }

                        getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE)
                                .edit()
                                .putString("user_id", userId)
                                .putString("full_name", nombre)
                                .putString("email", correo)
                                .putString("phone", telefono)
                                .putString("role", role)
                                .apply();

                        Toast.makeText(
                                RegisterActivity.this,
                                "¡Cuenta de usuario creada con éxito!",
                                Toast.LENGTH_LONG
                        ).show();

                        Intent intent = new Intent(RegisterActivity.this, UserMainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Error al registrar (" + response.code + "): " + response.body,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(
                            RegisterActivity.this,
                            "Fallo de red: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }
}