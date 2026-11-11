package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etTelefono, etPassword;
    private Button btnRegistrar;
    private TextView tvVolverLogin, tvIrRegistroOrganizacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombre = findViewById(R.id.etRegisterNombre);
        etCorreo = findViewById(R.id.etRegisterCorreo);
        etTelefono = findViewById(R.id.etRegisterTelefono);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegistrar = findViewById(R.id.btnRegistrarCuenta);
        tvVolverLogin = findViewById(R.id.tvVolverLogin);
        tvIrRegistroOrganizacion = findViewById(R.id.tvIrRegistroOrganizacion);

        // 1. ACCIÓN AL REGISTRARSE COMO USUARIO NORMAL
        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos requeridos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "¡Cuenta de usuario creada con éxito!", Toast.LENGTH_SHORT).show();

                // AQUÍ ESTABA EL ERROR: Ahora redirige correctamente a la interfaz simple de usuario
                Intent intent = new Intent(RegisterActivity.this, UserMainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 2. ENLACE PARA IR AL REGISTRO DE ORGANIZACIÓN / BRIGADA
        if (tvIrRegistroOrganizacion != null) {
            tvIrRegistroOrganizacion.setOnClickListener(v -> {
                Intent intent = new Intent(RegisterActivity.this, RegistrarOrganizacionActivity.class);
                startActivity(intent);
            });
        }

        // 3. VOLVER A LOGIN
        tvVolverLogin.setOnClickListener(v -> {
            finish();
        });
    }
}