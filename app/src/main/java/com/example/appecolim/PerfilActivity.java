package com.example.appecolim;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appecolim.data.local.EmpleadoDAO;

public class PerfilActivity extends AppCompatActivity {

    private TextView txtNombre, txtCodigo, txtCargo, txtTurno;
    private ImageButton btnVolver;
    private EmpleadoDAO empleadoDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        txtNombre = findViewById(R.id.txtNombre);
        txtCodigo = findViewById(R.id.txtCodigo);
        txtCargo = findViewById(R.id.txtCargo);
        txtTurno = findViewById(R.id.txtTurno);
        btnVolver = findViewById(R.id.btnVolver);

        empleadoDAO = new EmpleadoDAO(this);
        cargarDatosDelEmpleado();

        // Al tocar la flecha, regresa a la pantalla anterior
        btnVolver.setOnClickListener(v -> finish());
    }

    // Lee el empleado guardado en SQLite y llena los campos reales
    private void cargarDatosDelEmpleado() {
        Cursor cursor = empleadoDAO.obtenerEmpleadoGuardado();

        if (cursor.moveToFirst()) {
            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            String codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo"));
            String cargo = cursor.getString(cursor.getColumnIndexOrThrow("cargo"));
            String turno = cursor.getString(cursor.getColumnIndexOrThrow("turno"));

            txtNombre.setText(nombre);
            txtCodigo.setText(codigo);
            txtCargo.setText(cargo);
            txtTurno.setText(turno);
        }
        cursor.close();
    }
}