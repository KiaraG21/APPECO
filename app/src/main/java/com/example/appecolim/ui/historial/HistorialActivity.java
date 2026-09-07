package com.example.appecolim.ui.historial;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appecolim.R;
import com.example.appecolim.adapter.ResiduoAdapter;
import com.example.appecolim.data.model.Residuo;
import com.example.appecolim.ui.registro.RegistroActivity;
import com.example.appecolim.ui.reporte.ReporteActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private TextView btnFiltroFecha, btnFiltroTipo;
    private RecyclerView rvHistorial;
    private ResiduoAdapter adapter;
    private List<Residuo> listaResiduos = new ArrayList<>();

    // Variables para guardar los filtros actuales
    private String filtroFechaSeleccionada = "";
    private String filtroTipoSeleccionado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historial);

        // Inicializar vistas
        btnFiltroFecha = findViewById(R.id.btn_filtro_fecha);
        btnFiltroTipo = findViewById(R.id.btn_filtro_tipo);
        rvHistorial = findViewById(R.id.rv_historial);

        // Configurar RecyclerView
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResiduoAdapter(listaResiduos, true);
        rvHistorial.setAdapter(adapter);

        // Click en Filtro de Fecha (Ventana Emergente por Día Exacto)
        btnFiltroFecha.setOnClickListener(v -> mostrarSelectorFecha());

        // Click en Filtro de Tipo (Ventana Emergente con lista de materiales)
        btnFiltroTipo.setOnClickListener(v -> mostrarSelectorTipo());

        // Lógica de navegación entre pestañas
        findViewById(R.id.tab_hoy).setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroActivity.class));
            finish();
        });

        findViewById(R.id.tab_reporte).setOnClickListener(v -> {
            startActivity(new Intent(this, ReporteActivity.class));
            finish();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void mostrarSelectorFecha() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Formateamos la fecha seleccionada (Ej: 2026-09-06) para la API
            filtroFechaSeleccionada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            // Actualizamos el botón con la fecha completa
            btnFiltroFecha.setText("FECHA: " + dayOfMonth + "/" + (month + 1) + "/" + year);
            cargarDatosDeAPI(); // Llamada a tu GET /historial con filtros
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePicker.setTitle("Seleccionar Fecha Exacta");
        datePicker.show();
    }

    private void mostrarSelectorTipo() {
        // Los 7 tipos que definimos antes
        String[] tipos = {
            "Papel y cartón",
            "Plástico",
            "Metal",
            "Restos de comida",
            "Residuos de jardín",
            "Pilas y baterías",
            "Material punzocortante",
            "TODOS"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Tipo de Residuo");
        builder.setItems(tipos, (dialog, which) -> {
            if (tipos[which].equals("TODOS")) {
                filtroTipoSeleccionado = "";
                btnFiltroTipo.setText("TIPO ▼");
            } else {
                filtroTipoSeleccionado = tipos[which];
                btnFiltroTipo.setText(filtroTipoSeleccionado.toUpperCase());
            }
            cargarDatosDeAPI(); // Llamada a tu GET /historial con filtros
        });
        builder.show();
    }

    private void cargarDatosDeAPI() {
        // AQUÍ es donde tus compañeros conectarán el GET /historial?fecha=...&tipo=...
        // que ya probaste en Postman.
        
        // Simulación: Imprimir los parámetros que se enviarían a la API
        android.util.Log.d("API_HISTORIAL", "Llamando a: /historial?fecha=" + filtroFechaSeleccionada + "&tipo=" + filtroTipoSeleccionado);

        // LÓGICA DE SUMA DINÁMICA (Para que salga en el XML el total actualizado)
        double sumaTotal = 0;
        for (Residuo r : listaResiduos) {
            sumaTotal += r.getCantidadKg();
        }

        // Actualizamos el TextView del TOTAL en el XML
        TextView txtTotal = findViewById(R.id.txt_total_historial);
        txtTotal.setText(getString(R.string.total_format, String.valueOf(sumaTotal)));
    }
}
