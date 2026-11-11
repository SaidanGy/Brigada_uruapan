package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrarOrganizacionActivity extends AppCompatActivity {

    private EditText etNombre, etResponsable, etTelefono, etDireccion, etPassword;
    private Button btnRegistrar;
    private TextView tvVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_organizacion);

        etNombre = findViewById(R.id.etOrgNombre);
        etResponsable = findViewById(R.id.etOrgResponsable);
        etTelefono = findViewById(R.id.etOrgTelefono);
        etDireccion = findViewById(R.id.etOrgDireccion);
        etPassword = findViewById(R.id.etOrgPassword);
        btnRegistrar = findViewById(R.id.btnRegistrarOrganizacion);
        tvVolver = findViewById(R.id.tvVolverRegistroUsuario);

        // Lógica de registro de la organización
        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String responsable = etResponsable.getText().toString().trim();
            String telefono = etTelefono.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || responsable.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "¡Organización registrada con éxito!", Toast.LENGTH_LONG).show();
                // Redirigir al Centro de Mando (MainActivity)
                Intent intent = new Intent(RegistrarOrganizacionActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Volver a la pantalla de usuario
        tvVolver.setOnClickListener(v -> {
            finish();
        });
    }
}