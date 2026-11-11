package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class UserHistorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_historial);

        // Navegación Inferior - Ir a Inicio (Mapa)
        LinearLayout tabMapa = findViewById(R.id.tabUserMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                Intent intent = new Intent(UserHistorialActivity.this, UserMainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Navegación Inferior - Ir a Mi Cuenta
        LinearLayout tabCuenta = findViewById(R.id.tabUserCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                Intent intent = new Intent(UserHistorialActivity.this, UserCuentaActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}