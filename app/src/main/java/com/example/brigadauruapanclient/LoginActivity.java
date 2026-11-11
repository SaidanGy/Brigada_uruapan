package com.example.brigadauruapanclient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvIrARegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ajuste Edge-To-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Conectar el texto de registro
        tvIrARegistro = findViewById(R.id.tvIrARegistro);

        // Lógica del Botón Iniciar Sesión
        // Lógica del Botón Iniciar Sesión
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim().toLowerCase();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                // PRUEBAS INTERFAZ USUARIO Y ORGANIZACIÓN
                // Si el correo contiene la palabra "admin", entra como Organización/Centro de Mando
                // Si escribes cualquier otro correo (ejemplo: "usuario@gmail.com"), entra como Usuario Normal

                if (email.contains("admin")) {
                    Toast.makeText(this, "Iniciando sesión como Organización / Operador", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Iniciando sesión como Usuario", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, UserMainActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        });

        // Lógica para ir a la pantalla de Registro
        if (tvIrARegistro != null) {
            tvIrARegistro.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }
}