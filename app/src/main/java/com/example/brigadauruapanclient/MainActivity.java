package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Cambiar si es necesario id.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //  CONFIGURACIÓN DE TARJETA DESPLEGABLE
        View bottomSheetCard = findViewById(R.id.bottomSheetCard);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard);

        // Ocultar la tarjeta al iniciar la actividad para que no tape
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Al hacer clic en el área del mapa, se despliega la información
        View mapContainer = findViewById(R.id.mapContainer);
        if (mapContainer != null) {
            mapContainer.setOnClickListener(v -> {
                // Usamos STATE_COLLAPSED o STATE_EXPANDED según prefieras la altura
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN ||
                        bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });
        }

        //  BOTONES DE ACCIÓN DE LA TARJETA
        Button btnCallBrigade = findViewById(R.id.btnCallBrigade);
        if (btnCallBrigade != null) {
            btnCallBrigade.setOnClickListener(v -> {
                Toast.makeText(this, "Iniciando llamada con la brigada...", Toast.LENGTH_SHORT).show();
            });
        }

        /////////////////////boton en zona de mapa para solicitar brigada
        Button btnRequestBrigade = findViewById(R.id.btnRequestBrigade);
        if (btnRequestBrigade != null) {
            btnRequestBrigade.setOnClickListener(v -> {
                // Abre formulario de solicitud de brigada
                Intent intent = new Intent(MainActivity.this, SolicitarBrigadaActivity.class);
                startActivity(intent);
            });
        }

        // NAVEGACIÓN INFERIOR

        // Tab 1: Mapa
        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                // puedes cerrar la tarjeta o refrescar el mapa
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });
        }

        // Tab 2: Registrar Unidad
        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RegistrarUnidadActivity.class);
                startActivity(intent);
                finish(); // Cierra la actual para evitar acumular actividades en memoria
            });
        }

        // Tab 3: Organizaciones
        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, OrganizacionesActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // Tab 4: Cuenta
        LinearLayout tabCuenta = findViewById(R.id.tabCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CuentaActivity.class);
                startActivity(intent);
                finish();
            });
        }

    }
}