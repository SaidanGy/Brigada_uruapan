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

public class OrganizacionesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizaciones);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //  BOTÓN DE GUARDAR ORGANIZACIÓN
        Button btnGuardarOrg = findViewById(R.id.btnGuardarOrg);
        if (btnGuardarOrg != null) {
            btnGuardarOrg.setOnClickListener(v -> {
                Toast.makeText(this, "Organización y corporación vinculadas con éxito", Toast.LENGTH_SHORT).show();
            });
        }

        //  INFERIOR RESPONSIVA

        // Ir al Mapa (Home)
        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                Intent intent = new Intent(OrganizacionesActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Ir a Registrar Unidad
        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> {
                Intent intent = new Intent(OrganizacionesActivity.this, RegistrarUnidadActivity.class);
                startActivity(intent);
                finish();
            });
        }
        // Ir a Cuenta
        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                Intent intent = new Intent(OrganizacionesActivity.this, CuentaActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}