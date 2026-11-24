package com.example.brigadauruapanclient;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    public static final String BASE_URL = "http://192.168.1.7:8080";
    public static final String WS_URL = "ws://10.0.2.2:8080/ws";
    private static final String PREFS_NAME = "UrbTrackPrefs";

    private static final String[] ARCHIVOS_PREFS_SESION = {
            "UrbTrackPrefs",
            "usuario_sesion_prefs",
            "organizaciones_uruapan_prefs",
            "perfil_operador_cache",
            "brigada_estado_prefs"
    };

    public static String getToken(Context context) {
        if (context == null) return "";

        String[] archivosPrefs = {"UrbTrackPrefs", "usuario_sesion_prefs", "organizaciones_uruapan_prefs"};
        String[] clavesToken = {"accessToken", "token", "jwt"};

        for (String archivo : archivosPrefs) {
            SharedPreferences prefs = context.getSharedPreferences(archivo, Context.MODE_PRIVATE);
            for (String clave : clavesToken) {
                String val = prefs.getString(clave, "");
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    public static String getRole(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("role", "");
    }

    public static void cerrarSesion(Context context) {
        if (context == null) return;
        for (String archivo : ARCHIVOS_PREFS_SESION) {
            context.getSharedPreferences(archivo, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }

    public static ApiResponse request(Context context, String method, String endpoint, String jsonBody) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        boolean esRutaAuth = endpoint != null && endpoint.startsWith("/api/auth/");
        if (!esRutaAuth) {
            String token = getToken(context);
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
        }

        if (jsonBody != null && !jsonBody.isEmpty()) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }
        conn.disconnect();
        return new ApiResponse(code, sb.toString());
    }

    public static class ApiResponse {
        public final int code;
        public final String body;

        public ApiResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean isSuccessful() {
            return code >= 200 && code < 300;
        }
    }
}