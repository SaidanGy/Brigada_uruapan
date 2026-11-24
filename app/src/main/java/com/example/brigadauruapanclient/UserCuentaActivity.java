package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class UserCuentaActivity extends AppCompatActivity {

    private static final String PREFS_USUARIO = "usuario_sesion_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";
    private static final String PREFS_PERFIL_CACHE = "perfil_ciudadano_cache";

    private EditText etUserNombre, etUserCorreo, etUserTelefono, etUserNuevaPassword;
    private Button btnGuardarCambiosUser, btnCerrarSesionUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_cuenta);

        etUserNombre = findViewById(R.id.etUserNombre);
        etUserCorreo = findViewById(R.id.etUserCorreo);
        etUserTelefono = findViewById(R.id.etUserTelefono);
        etUserNuevaPassword = findViewById(R.id.etUserNuevaPassword);
        btnGuardarCambiosUser = findViewById(R.id.btnGuardarCambiosUser);
        btnCerrarSesionUser = findViewById(R.id.btnCerrarSesionUser);

        cargarDatosUsuario();

        if (btnGuardarCambiosUser != null) {
            btnGuardarCambiosUser.setOnClickListener(v -> guardarCambiosUsuario());
        }

        if (btnCerrarSesionUser != null) {
            btnCerrarSesionUser.setOnClickListener(v -> cerrarSesion());
        }

        LinearLayout tabInicio = findViewById(R.id.tabUserMapa);
        if (tabInicio != null) {
            tabInicio.setOnClickListener(v -> {
                startActivity(new Intent(UserCuentaActivity.this, UserMainActivity.class));
                finish();
            });
        }

        LinearLayout tabHistorial = findViewById(R.id.tabUserHistorial);
        if (tabHistorial != null) {
            tabHistorial.setOnClickListener(v -> {
                startActivity(new Intent(UserCuentaActivity.this, UserHistorialActivity.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuario();
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private void cargarDatosUsuario() {
        SharedPreferences prefsUsuario = getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE);
        SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        SharedPreferences prefsCache = getSharedPreferences(PREFS_PERFIL_CACHE, MODE_PRIVATE);

        String correoGuardado = prefsUsuario.getString("email", "");
        if (correoGuardado.isEmpty()) {
            correoGuardado = prefsLogin.getString("email", "");
        }
        String correoKey = normalizarCorreo(correoGuardado);

        String nombreGuardado = prefsUsuario.getString("full_name", "");
        if (nombreGuardado.isEmpty() && !correoKey.isEmpty()) {
            nombreGuardado = prefsCache.getString("nombre_" + correoKey, "");
        }

        String telefonoGuardado = prefsUsuario.getString("phone", "");
        if (telefonoGuardado.isEmpty() && !correoKey.isEmpty()) {
            telefonoGuardado = prefsCache.getString("telefono_" + correoKey, "");
        }

        if (etUserNombre != null) {
            etUserNombre.setText(nombreGuardado);
        }
        if (etUserCorreo != null) {
            etUserCorreo.setText(correoGuardado);
        }
        if (etUserTelefono != null) {
            etUserTelefono.setText(telefonoGuardado);
        }
    }

    private void guardarCambiosUsuario() {
        String nombre = etUserNombre != null ? etUserNombre.getText().toString().trim() : "";
        String correo = etUserCorreo != null ? etUserCorreo.getText().toString().trim() : "";
        String telefono = etUserTelefono != null ? etUserTelefono.getText().toString().trim() : "";
        String nuevaPassword = etUserNuevaPassword != null ? etUserNuevaPassword.getText().toString().trim() : "";

        if (nombre.isEmpty() || correo.isEmpty()) {
            Toast.makeText(this, "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nuevaPassword.isEmpty() && nuevaPassword.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        String correoKey = normalizarCorreo(correo);

        getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE)
                .edit()
                .putString("full_name", nombre)
                .putString("email", correo)
                .putString("phone", telefono)
                .apply();

        getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE)
                .edit()
                .putString("email", correo)
                .apply();

        SharedPreferences.Editor cacheEditor = getSharedPreferences(PREFS_PERFIL_CACHE, MODE_PRIVATE).edit();
        cacheEditor.putString("nombre_" + correoKey, nombre);
        cacheEditor.putString("telefono_" + correoKey, telefono);
        if (!nuevaPassword.isEmpty()) {
            cacheEditor.putString("password_" + correoKey, nuevaPassword);
        }
        cacheEditor.apply();

        if (etUserNuevaPassword != null) {
            etUserNuevaPassword.setText("");
        }

        String mensaje = nuevaPassword.isEmpty()
                ? "Datos de cuenta guardados"
                : "Datos y nueva contraseña guardados";
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private void cerrarSesion() {
        getSharedPreferences(PREFS_USUARIO, MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE).edit().clear().apply();

        Intent intent = new Intent(UserCuentaActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}