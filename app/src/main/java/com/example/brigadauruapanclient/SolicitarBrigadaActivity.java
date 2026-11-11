package com.example.brigadauruapanclient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SolicitarBrigadaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitar_brigada);

        EditText etNombre = findViewById(R.id.etNombreSolicitante);
        EditText etTelefono = findViewById(R.id.etTelefonoSolicitante);
        EditText etDireccion = findViewById(R.id.etDireccionDestino);
        EditText etReferencias = findViewById(R.id.etReferencias);
        Button btnConfirmar = findViewById(R.id.btnConfirmarSolicitud);

        btnConfirmar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String telefono = etTelefono.getText().toString().trim();
            String direccion = etDireccion.getText().toString().trim();

            if (nombre.isEmpty() || telefono.isEmpty() || direccion.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "¡Solicitud enviada con éxito!", Toast.LENGTH_LONG).show();
                finish(); // Regresa al mapa o pantalla anterior
            }
        });
    }
}