package com.example.appecolim;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appecolim.data.local.EmpleadoDAO;
import com.example.appecolim.network.ApiService;
import com.example.appecolim.network.RetrofitClient;
import com.example.appecolim.network.dto.ErrorResponse;
import com.example.appecolim.network.dto.LoginRequest;
import com.example.appecolim.network.dto.LoginResponse;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etCodigo, etPassword;
    private Button btnIniciarSesion;
    private TextView txtError;
    private EmpleadoDAO empleadoDAO;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etCodigo = findViewById(R.id.etCodigo);
        etPassword = findViewById(R.id.etPassword);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        txtError = findViewById(R.id.txtError);

        empleadoDAO = new EmpleadoDAO(this);
        apiService = RetrofitClient.getApiService();

        btnIniciarSesion.setOnClickListener(v -> intentarLogin());
    }

    private void intentarLogin() {
        String codigo = etCodigo.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (codigo.isEmpty() || password.isEmpty()) {
            mostrarError("Ingresa tu código y contraseña");
            return;
        }

        txtError.setVisibility(View.GONE);
        btnIniciarSesion.setEnabled(false);
        Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show();

        LoginRequest request = new LoginRequest(codigo, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnIniciarSesion.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    guardarSesionYNavegar(loginResponse);
                } else {
                    String mensaje = "Código o contraseña incorrectos";
                    try {
                        if (response.errorBody() != null) {
                            ErrorResponse error = new Gson().fromJson(
                                    response.errorBody().string(), ErrorResponse.class);
                            if (error != null && error.mensaje != null) {
                                mensaje = error.mensaje;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    mostrarError(mensaje);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnIniciarSesion.setEnabled(true);
                // Sin conexión: intenta login offline con lo último guardado en SQLite
                intentarLoginOffline(codigo);
            }
        });
    }

    private void guardarSesionYNavegar(LoginResponse loginResponse) {
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .format(new java.util.Date());

        empleadoDAO.guardarEmpleado(
                loginResponse.empleado.idEmpleado,
                loginResponse.empleado.nombre,
                loginResponse.empleado.cargo,
                loginResponse.empleado.turno,
                loginResponse.empleado.codigo,
                loginResponse.token,
                fechaActual
        );

        irAMenuPrincipal();
    }

    private void intentarLoginOffline(String codigoIngresado) {
        try (Cursor cursor = empleadoDAO.obtenerEmpleadoGuardado()) {
            if (cursor != null && cursor.moveToFirst()) {
                int idxCodigo = cursor.getColumnIndex("codigo");
                String codigoGuardado = idxCodigo >= 0 ? cursor.getString(idxCodigo) : null;

                if (codigoGuardado != null && codigoGuardado.equals(codigoIngresado)) {
                    Toast.makeText(this, "Sin conexión, usando datos guardados", Toast.LENGTH_SHORT).show();
                    irAMenuPrincipal();
                    return;
                }
            }
        }
        mostrarError("Sin conexión a internet y no hay una sesión guardada para este código");
    }

    private void irAMenuPrincipal() {
        Intent intent = new Intent(LoginActivity.this, MenuPrincipalActivity.class);
        startActivity(intent);
        finish();
    }

    private void mostrarError(String mensaje) {
        txtError.setText(mensaje);
        txtError.setVisibility(View.VISIBLE);
    }
}