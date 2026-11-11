package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class UserMainActivity extends AppCompatActivity {

    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        // Configuración de la tarjeta deslizable (BottomSheet)
        View bottomSheetCard = findViewById(R.id.bottomSheetUserCard);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN); // Oculta al iniciar

        // Al tocar el área del mapa, se despliega la información de la brigada seleccionada
        View mapContainer = findViewById(R.id.mapUserContainer);
        if (mapContainer != null) {
            mapContainer.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN ||
                        bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });
        }

        // Botón de Llamar en la tarjeta
        Button btnCallBrigade = findViewById(R.id.btnCallBrigade);
        if (btnCallBrigade != null) {
            btnCallBrigade.setOnClickListener(v -> {
                Toast.makeText(this, "Iniciando llamada con la brigada...", Toast.LENGTH_SHORT).show();
            });
        }

        // Botón de Solicitar Brigada en la tarjeta
        Button btnRequestBrigade = findViewById(R.id.btnRequestBrigade);
        if (btnRequestBrigade != null) {
            btnRequestBrigade.setOnClickListener(v -> {
                Intent intent = new Intent(UserMainActivity.this, SolicitarBrigadaActivity.class);
                startActivity(intent);
            });
        }

        // Navegación Inferior
        LinearLayout tabMapa = findViewById(R.id.tabUserMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });
        }

        LinearLayout tabHistorial = findViewById(R.id.tabUserHistorial);
        if (tabHistorial != null) {
            tabHistorial.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), UserHistorialActivity.class);
                startActivity(intent);
                finish();
            });
        }

        LinearLayout tabCuenta = findViewById(R.id.tabUserCuenta);
        if (tabCuenta != null) {
            tabCuenta.setOnClickListener(v -> {
                Intent intent = new Intent(UserMainActivity.this, UserCuentaActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
}