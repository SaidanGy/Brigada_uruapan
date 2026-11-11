package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegistrarUnidadActivity extends AppCompatActivity {

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
        ///LOGICA
        // BOTONES DE LA VISTA DE REGISTRO ---

        // Botón para refrescar las coordenadas GPS simuladas
        Button btnActualizarGps = findViewById(R.id.btnActualizarGps);
        if (btnActualizarGps != null) {
            btnActualizarGps.setOnClickListener(v -> {
                Toast.makeText(this, "Posición GPS actualizada vía API nativa", Toast.LENGTH_SHORT).show();
            });
        }

        // Botón principal para transmitir la ubicación
        Button btnTransmitir = findViewById(R.id.btnTransmitir);
        if (btnTransmitir != null) {
            btnTransmitir.setOnClickListener(v -> {
                Toast.makeText(this, "Paquete JSON transmitido al Centro de Mando con éxito", Toast.LENGTH_SHORT).show();
            });
        }

        // NAVEGACIÓN INFERIOR

        // Ir al Mapa (Home)
        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                Intent intent = new Intent(RegistrarUnidadActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
        // Ir a Organizaciones
        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                Intent intent = new Intent(RegistrarUnidadActivity.this, OrganizacionesActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Ir a Cuenta
        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                Intent intent = new Intent(RegistrarUnidadActivity.this, CuentaActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}