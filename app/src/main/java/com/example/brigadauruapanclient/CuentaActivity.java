package com.example.brigadauruapanclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class CuentaActivity extends AppCompatActivity {

    private static final String PREFS_ORG = "organizaciones_uruapan_prefs";
    private static final String PREFS_URBTRACK = "UrbTrackPrefs";
    private static final String PREFS_OPERADOR_CACHE = "perfil_operador_cache";

    private EditText etNombreOperador, etMatriculaOperador, etCorreoOperador, etTelefonoOperador;
    private EditText etCorporacionAsignada, etDireccionBase, etTurnoGuardia;
    private Button btnGuardarCambios, btnCerrarSesion;

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

        etNombreOperador = findViewById(R.id.etNombreOperador);
        etMatriculaOperador = findViewById(R.id.etMatriculaOperador);
        etCorreoOperador = findViewById(R.id.etCorreoOperador);
        etTelefonoOperador = findViewById(R.id.etTelefonoOperador);
        etCorporacionAsignada = findViewById(R.id.etCorporacionAsignada);
        etDireccionBase = findViewById(R.id.etDireccionBase);


        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        cargarDatosOperador();

        if (btnGuardarCambios != null) {
            btnGuardarCambios.setOnClickListener(v -> guardarCambiosOperador());
        }

        if (btnCerrarSesion != null) {
            btnCerrarSesion.setOnClickListener(v -> {
                getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE).edit().clear().apply();
                getSharedPreferences(PREFS_ORG, MODE_PRIVATE).edit().clear().apply();

                Intent intent = new Intent(CuentaActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        LinearLayout tabMapa = findViewById(R.id.tabMapa);
        if (tabMapa != null) {
            tabMapa.setOnClickListener(v -> {
                startActivity(new Intent(CuentaActivity.this, MainActivity.class));
                finish();
            });
        }

        LinearLayout tabRegistrar = findViewById(R.id.tabRegistrarUnidad);
        if (tabRegistrar != null) {
            tabRegistrar.setOnClickListener(v -> {
                startActivity(new Intent(CuentaActivity.this, RegistrarUnidadActivity.class));
                finish();
            });
        }

        LinearLayout tabOrganizaciones = findViewById(R.id.tabOrganizaciones);
        if (tabOrganizaciones != null) {
            tabOrganizaciones.setOnClickListener(v -> {
                startActivity(new Intent(CuentaActivity.this, OrganizacionesActivity.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosOperador();
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private void cargarDatosOperador() {
        SharedPreferences prefsOrg = getSharedPreferences(PREFS_ORG, MODE_PRIVATE);
        SharedPreferences prefsLogin = getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE);
        SharedPreferences prefsCache = getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE);

        String correo = prefsLogin.getString("email", "");
        if (correo.isEmpty()) {
            correo = prefsOrg.getString("email_org", "");
        }
        String key = normalizarCorreo(correo);

        String responsable = !key.isEmpty() ? prefsCache.getString("responsable_" + key, "") : "";
        if (responsable.isEmpty()) {
            responsable = prefsOrg.getString("responsable_org", "");
        }

        String matricula = !key.isEmpty() ? prefsCache.getString("matricula_" + key, "") : "";
        if (matricula.isEmpty()) {
            matricula = prefsOrg.getString("unit_id", "");
        }
        if (matricula.isEmpty()) {
            matricula = prefsOrg.getString("user_id", "");
        }

        String telefono = !key.isEmpty() ? prefsCache.getString("telefono_" + key, "") : "";
        if (telefono.isEmpty()) {
            telefono = prefsOrg.getString("telefono_org", "");
        }

        String corporacion = !key.isEmpty() ? prefsCache.getString("corporacion_" + key, "") : "";
        if (corporacion.isEmpty()) {
            corporacion = prefsOrg.getString("nombre_org", "");
        }

        String direccion = !key.isEmpty() ? prefsCache.getString("direccion_" + key, "") : "";
        if (direccion.isEmpty()) {
            direccion = prefsOrg.getString("direccion_org", "");
        }

        String turno = !key.isEmpty() ? prefsCache.getString("turno_" + key, "") : "";
        String unitType = !key.isEmpty() ? prefsCache.getString("unit_type_" + key, prefsOrg.getString("unit_type", "")) : prefsOrg.getString("unit_type", "");

        if (!key.isEmpty()) {
            prefsOrg.edit()
                    .putString("email_org", correo)
                    .putString("nombre_org", corporacion)
                    .putString("responsable_org", responsable)
                    .putString("telefono_org", telefono)
                    .putString("direccion_org", direccion)
                    .putString("unit_id", matricula)
                    .putString("unit_type", unitType)
                    .apply();
        }

        if (etNombreOperador != null) etNombreOperador.setText(responsable);
        if (etMatriculaOperador != null) etMatriculaOperador.setText(matricula);
        if (etCorreoOperador != null) etCorreoOperador.setText(correo);
        if (etTelefonoOperador != null) etTelefonoOperador.setText(telefono);
        if (etCorporacionAsignada != null) etCorporacionAsignada.setText(corporacion);
        if (etDireccionBase != null) etDireccionBase.setText(direccion);
        if (etTurnoGuardia != null) etTurnoGuardia.setText(turno);
    }

    private void guardarCambiosOperador() {
        String responsable = etNombreOperador != null ? etNombreOperador.getText().toString().trim() : "";
        String matricula = etMatriculaOperador != null ? etMatriculaOperador.getText().toString().trim() : "";
        String correo = etCorreoOperador != null ? etCorreoOperador.getText().toString().trim() : "";
        String telefono = etTelefonoOperador != null ? etTelefonoOperador.getText().toString().trim() : "";
        String corporacion = etCorporacionAsignada != null ? etCorporacionAsignada.getText().toString().trim() : "";
        String direccion = etDireccionBase != null ? etDireccionBase.getText().toString().trim() : "";
        String turno = etTurnoGuardia != null ? etTurnoGuardia.getText().toString().trim() : "";

        if (correo.isEmpty()) {
            Toast.makeText(this, "El correo es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = normalizarCorreo(correo);

        getSharedPreferences(PREFS_OPERADOR_CACHE, MODE_PRIVATE)
                .edit()
                .putString("responsable_" + key, responsable)
                .putString("matricula_" + key, matricula)
                .putString("telefono_" + key, telefono)
                .putString("corporacion_" + key, corporacion)
                .putString("direccion_" + key, direccion)
                .putString("turno_" + key, turno)
                .apply();

        getSharedPreferences(PREFS_ORG, MODE_PRIVATE)
                .edit()
                .putString("responsable_org", responsable)
                .putString("unit_id", matricula)
                .putString("email_org", correo)
                .putString("telefono_org", telefono)
                .putString("nombre_org", corporacion)
                .putString("direccion_org", direccion)
                .apply();

        getSharedPreferences(PREFS_URBTRACK, MODE_PRIVATE)
                .edit()
                .putString("email", correo)
                .apply();

        Toast.makeText(this, "Datos de cuenta actualizados con éxito", Toast.LENGTH_SHORT).show();
    }
}