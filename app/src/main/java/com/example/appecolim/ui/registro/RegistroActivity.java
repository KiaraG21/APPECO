package com.example.appecolim.ui.registro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appecolim.R;
import com.example.appecolim.adapter.ResiduoAdapter;
import com.example.appecolim.data.model.Residuo;
import com.example.appecolim.ui.historial.HistorialActivity;
import com.example.appecolim.ui.reporte.ReporteActivity;
import com.example.appecolim.utils.FechaUtils;
import java.util.ArrayList;
import java.util.List;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import com.example.appecolim.data.local.DataBaseHelper;
import java.util.Locale;

public class RegistroActivity extends AppCompatActivity {

    private RecyclerView rvHoy;
    private ResiduoAdapter adapter;
    private List<Residuo> listaHoy = new ArrayList<>();
    private TextView txtFechaChip, txtTotalDia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro);

        // Inicializar vistas
        txtFechaChip = findViewById(R.id.txt_fecha_chip);
        txtTotalDia = findViewById(R.id.txt_total_dia);
        rvHoy = findViewById(R.id.rv_registros_hoy);

        // Mostrar fecha actual en el chip superior
        txtFechaChip.setText(FechaUtils.obtenerFechaHoraActual().split(" ")[0]);

        // Configurar RecyclerView
        rvHoy.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResiduoAdapter(listaHoy, false);
        adapter.setOnEditarListener(residuo ->
                EditorResiduo.mostrar(
                        RegistroActivity.this,
                        residuo,
                        this::cargarDatosDeHoy
                )
        );
        rvHoy.setAdapter(adapter);

        // Botón de Volver
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Lógica de navegación entre pestañas
        findViewById(R.id.tab_historial).setOnClickListener(v -> {
            startActivity(new Intent(this, HistorialActivity.class));
            finish();
        });

        findViewById(R.id.tab_reporte).setOnClickListener(v -> {
            startActivity(new Intent(this, ReporteActivity.class));
            finish();
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosDeHoy();
    }

    private void cargarDatosDeHoy() {
        String fechaHoy = FechaUtils.obtenerFechaHoraActual().split(" ")[0];
        txtFechaChip.setText(fechaHoy);

        // Consulta fuera del hilo de la interfaz.
        new Thread(() -> {
            List<Residuo> registros = new ArrayList<>();
            double suma = 0;

            try (DataBaseHelper helper =
                         new DataBaseHelper(getApplicationContext())) {
                SQLiteDatabase db = helper.getReadableDatabase();

                // Solo muestra registros del empleado que tiene la sesión.
                String consulta =
                        "SELECT id_local, tipo, cantidad_kg, fecha_hora "
                                + "FROM residuo_local "
                                + "WHERE id_empleado = "
                                + "(SELECT id_empleado FROM empleado_local LIMIT 1) "
                                + "AND fecha_hora LIKE ? "
                                + "ORDER BY fecha_hora DESC, id_local DESC";

                try (Cursor cursor = db.rawQuery(
                        consulta, new String[]{fechaHoy + "%"})) {
                    while (cursor.moveToNext()) {
                        int idLocal = cursor.getInt(
                                cursor.getColumnIndexOrThrow("id_local"));
                        String tipo = cursor.getString(
                                cursor.getColumnIndexOrThrow("tipo"));
                        double cantidadKg = cursor.getDouble(
                                cursor.getColumnIndexOrThrow("cantidad_kg"));
                        String fechaHora = cursor.getString(
                                cursor.getColumnIndexOrThrow("fecha_hora"));

                        registros.add(new Residuo(
                                idLocal, tipo, cantidadKg, fechaHora));
                        suma += cantidadKg;
                    }
                }

                final double total = suma;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    listaHoy.clear();
                    listaHoy.addAll(registros);
                    adapter.notifyDataSetChanged();

                    txtTotalDia.setText(getString(
                            R.string.total_format,
                            String.format(Locale.getDefault(), "%.2f", total)));

                    android.util.Log.d("REGISTROS_HOY",
                            "Registros encontrados: " + registros.size()
                                    + ", total kg: " + total);
                });
            } catch (RuntimeException e) {
                android.util.Log.e(
                        "REGISTROS_HOY", "Error leyendo SQLite", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(
                            RegistroActivity.this,
                            "No se pudieron cargar los registros del día",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }, "cargar-registros-hoy").start();
    }
}
