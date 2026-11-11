package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UserCuentaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_cuenta);

        Button btnGuardar = findViewById(R.id.btnGuardarCambiosUser);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesionUser);

        btnGuardar.setOnClickListener(v -> {
            Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
        });

        // Botón para cerrar sesión y volver al Login
        btnCerrarSesion.setOnClickListener(v -> {
            Intent intent = new Intent(UserCuentaActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Navegación Inferior en la pestaña Cuenta - Ir a Mapa/Inicio
        LinearLayout tabMapa = findViewById(R.id.tabUserMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                Intent intent = new Intent(UserCuentaActivity.this, UserMainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Navegación Inferior en la pestaña Cuenta - Ir a Mis Solicitudes
        LinearLayout tabHistorial = findViewById(R.id.tabUserHistorial);
        if (tabHistorial != null) {
            tabHistorial.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), UserHistorialActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}