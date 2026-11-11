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

public class CuentaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cuenta);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // MANEJO DE BOTONES DE LA VISTA DE CUENTA
        Button btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        if (btnGuardarCambios != null) {
            btnGuardarCambios.setOnClickListener(v -> {
                Toast.makeText(this, "Datos de cuenta actualizados con éxito", Toast.LENGTH_SHORT).show();
            });
        }

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> {

                Intent intent = new Intent(CuentaActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish();
            });
        }

        // NAVEGACIÓN INFERIOR

        // Ir al Mapa (Home)
        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                Intent intent = new Intent(CuentaActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Ir a Registrar Unidad
        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> {
                Intent intent = new Intent(CuentaActivity.this, RegistrarUnidadActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Ir a Organizaciones
        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                Intent intent = new Intent(CuentaActivity.this, OrganizacionesActivity.class);
                startActivity(intent);
                finish();
            });
        }


    }
}