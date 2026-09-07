package com.example.appecolim.ui.reporte;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.view.View;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import com.example.appecolim.R;
import com.example.appecolim.data.local.EmpleadoDAO;
import com.example.appecolim.network.ApiService;
import com.example.appecolim.network.RetrofitClient;
import com.example.appecolim.network.dto.ReporteResponse;
import com.example.appecolim.network.dto.TipoTotalDto;
import com.example.appecolim.ui.historial.HistorialActivity;
import com.example.appecolim.ui.registro.RegistroActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReporteActivity extends AppCompatActivity {

    private TextView txtMesReporte, txtTotalKg;
    private LinearLayout containerMateriales, containerLeyenda;
    private MaterialButton btnExportPdf, btnExportCsv;
    private EmpleadoDAO empleadoDAO;
    private ApiService apiService;
    private final Map<String, Double> datosExportacion = new LinkedHashMap<>();
    private boolean reporteDisponible = false;
    private File archivoExportacion;

    private final ActivityResultLauncher<Intent> selectorExportacion =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() != RESULT_OK
                                || resultado.getData() == null
                                || resultado.getData().getData() == null) {
                            limpiarTemporalExportacion();
                            return;
                        }

                        Uri destino = resultado.getData().getData();
                        File origen = archivoExportacion;

                        if (origen == null || !origen.isFile()) {
                            limpiarTemporalExportacion();
                            Toast.makeText(this,
                                    "Vuelve a exportar el reporte",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        new Thread(() -> {
                            String mensaje;

                            try (FileInputStream entrada = new FileInputStream(origen);
                                 OutputStream salida = getContentResolver()
                                         .openOutputStream(destino, "wt")) {

                                if (salida == null) {
                                    throw new IOException(
                                            "No se pudo abrir el archivo de destino");
                                }

                                byte[] buffer = new byte[8192];
                                int leidos;

                                while ((leidos = entrada.read(buffer)) != -1) {
                                    salida.write(buffer, 0, leidos);
                                }

                                salida.flush();
                                mensaje = "Archivo guardado correctamente";

                            } catch (IOException | RuntimeException e) {
                                android.util.Log.e(
                                        "EXPORTAR_REPORTE", "Error guardando", e);
                                mensaje = "No se pudo guardar el archivo completo. "
                                        + "Intenta exportarlo nuevamente.";
                            }

                            final String aviso = mensaje;

                            runOnUiThread(() -> {
                                limpiarTemporalExportacion();

                                if (!isFinishing() && !isDestroyed()) {
                                    Toast.makeText(this,
                                            aviso,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }, "guardar-reporte").start();
                    }
            );

    // Traduce el "tipo" técnico de la API a la etiqueta que se muestra en pantalla
    private static final Map<String, String> ETIQUETAS_TIPO = new HashMap<>();
    static {
        ETIQUETAS_TIPO.put("papel_carton", "PAPEL / CARTÓN");
        ETIQUETAS_TIPO.put("plastico", "PLÁSTICO");
        ETIQUETAS_TIPO.put("metal", "METAL");
        ETIQUETAS_TIPO.put("restos_de_comida", "RESTOS DE COMIDA");
        ETIQUETAS_TIPO.put("residuos_de_jardin", "RESIDUOS DE JARDÍN");
        ETIQUETAS_TIPO.put("pilas_baterias", "PILAS Y BATERÍAS");
        ETIQUETAS_TIPO.put("material_punzocortante", "MATERIAL PUNZOCORTANTE");
    }

    private static final String[] NOMBRES_MES = {
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reporte);
        if (savedInstanceState != null) {
            String ruta = savedInstanceState.getString("archivo_exportacion");
            if (ruta != null) {
                archivoExportacion = new File(ruta);
            }

        }

        txtMesReporte = findViewById(R.id.txt_mes_reporte);
        txtTotalKg = findViewById(R.id.txt_reporte_total_kg);
        containerMateriales = findViewById(R.id.container_resumen_materiales);
        containerLeyenda = findViewById(R.id.container_leyenda);
        btnExportPdf = findViewById(R.id.btn_export_pdf);
        btnExportCsv = findViewById(R.id.btn_export_csv);

        empleadoDAO = new EmpleadoDAO(this);
        apiService = RetrofitClient.getApiService();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnExportPdf.setOnClickListener(v -> exportarReporte("PDF"));
        btnExportCsv.setOnClickListener(v -> exportarReporte("CSV"));

        findViewById(R.id.tab_hoy).setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroActivity.class));
            finish();
        });
        findViewById(R.id.tab_historial).setOnClickListener(v -> {
            startActivity(new Intent(this, HistorialActivity.class));
            finish();
        });
        reporteDisponible = false;
        cargarDatosReporte();
    }

    private void exportarReporte(String formato) {
        if (!reporteDisponible) {
            Toast.makeText(this,
                    "Espera a que se cargue el reporte",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (archivoExportacion != null) {
            Toast.makeText(this,
                    "Termina la exportación actual",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean esPdf = "PDF".equals(formato);
        String extension = esPdf ? ".pdf" : ".csv";
        String mime = esPdf ? "application/pdf" : "text/csv";

        try {
            archivoExportacion = File.createTempFile(
                    "reporte_", extension, getCacheDir());

            if (esPdf) {
                crearPdf(archivoExportacion);
            } else {
                crearCsv(archivoExportacion);
            }

            String fecha = new java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.US
            ).format(new java.util.Date());

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mime);
            intent.putExtra(
                    Intent.EXTRA_TITLE,
                    "ECOLIM_reporte_" + fecha + extension
            );

            selectorExportacion.launch(intent);

        } catch (IOException | RuntimeException e) {
            android.util.Log.e(
                    "EXPORTAR_REPORTE", "Error preparando archivo", e);
            limpiarTemporalExportacion();

            Toast.makeText(this,
                    "No se pudo preparar el archivo",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void crearPdf(File destino) throws IOException {
        // Es la tarjeta que contiene resumen, total, donut y leyenda.
        View tarjeta = (View) containerMateriales.getParent();

        if (tarjeta.getWidth() <= 0 || tarjeta.getHeight() <= 0) {
            throw new IOException("El reporte aún no terminó de dibujarse");
        }

        PdfDocument documento = new PdfDocument();

        try {
            // Página A4 aproximada en puntos.
            PdfDocument.PageInfo informacion =
                    new PdfDocument.PageInfo.Builder(595, 842, 1).create();

            PdfDocument.Page pagina = documento.startPage(informacion);

            try {
                Canvas canvas = pagina.getCanvas();
                canvas.drawColor(Color.WHITE);

                Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
                texto.setColor(Color.BLACK);
                texto.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD));
                texto.setTextSize(18);

                canvas.drawText("ECOLIM - REPORTE DE RESIDUOS", 40, 45, texto);

                texto.setTypeface(android.graphics.Typeface.DEFAULT);
                texto.setTextSize(12);
                canvas.drawText(
                        txtMesReporte.getText().toString(), 40, 68, texto);

                float anchoDisponible = 515f;
                float altoDisponible = 690f;

                float escala = Math.min(
                        anchoDisponible / tarjeta.getWidth(),
                        altoDisponible / tarjeta.getHeight()
                );

                float izquierda =
                        40f + (anchoDisponible
                                - tarjeta.getWidth() * escala) / 2f;

                int estadoCanvas = canvas.save();
                canvas.translate(izquierda, 90f);
                canvas.scale(escala, escala);

                // Dibuja el reporte completo, aunque haya que desplazarse
                // para verlo entero en el teléfono.
                tarjeta.draw(canvas);
                canvas.restoreToCount(estadoCanvas);

                texto.setTextSize(9);
                canvas.drawText(
                        "Resumen mensual de los datos cargados en la aplicación",
                        40, 812, texto);

            } finally {
                documento.finishPage(pagina);
            }

            try (FileOutputStream salida = new FileOutputStream(destino)) {
                documento.writeTo(salida);
            }
        } finally {
            documento.close();
        }
    }

    private void crearCsv(File destino) throws IOException {
        double total = 0;
        for (double kg : datosExportacion.values()) {
            total += kg;
        }

        // BOM UTF-8 para facilitar la lectura de tildes en Excel.
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Periodo,Categoria,Kilogramos,Porcentaje\r\n");

        String periodo = txtMesReporte.getText().toString();

        for (Map.Entry<String, Double> entrada
                : datosExportacion.entrySet()) {

            String codigo = entrada.getKey();
            double kg = entrada.getValue();
            double porcentaje = total > 0 ? kg / total * 100.0 : 0;

            String etiqueta = ETIQUETAS_TIPO.getOrDefault(
                    codigo,
                    codigo.replace('_', ' ').toUpperCase(Locale.ROOT)
            );

            csv.append(celdaCsv(periodo)).append(',')
                    .append(celdaCsv(etiqueta)).append(',')
                    .append(String.format(Locale.US, "%.2f", kg)).append(',')
                    .append(String.format(Locale.US, "%.2f", porcentaje))
                    .append("\r\n");
        }

        csv.append(celdaCsv(periodo)).append(',')
                .append(celdaCsv("TOTAL")).append(',')
                .append(String.format(Locale.US, "%.2f", total)).append(',')
                .append(total > 0 ? "100.00" : "0.00")
                .append("\r\n");

        try (FileOutputStream salida = new FileOutputStream(destino)) {
            salida.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String celdaCsv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private void limpiarTemporalExportacion() {
        if (archivoExportacion != null) {
            if (archivoExportacion.exists() && !archivoExportacion.delete()) {
                android.util.Log.w(
                        "EXPORTAR_REPORTE", "No se pudo limpiar el temporal");
            }
            archivoExportacion = null;
        }
    }

    private String obtenerIdEmpleadoGuardado() {
        try (Cursor cursor = empleadoDAO.obtenerEmpleadoGuardado()) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex("id_empleado");
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        }
        return null;
    }

    private void cargarDatosReporte() {
        String idEmpleado = null;
        String token = null;

        // Lee el identificador y el token de la misma sesión.
        try (Cursor cursor = empleadoDAO.obtenerEmpleadoGuardado()) {
            if (cursor != null && cursor.moveToFirst()) {
                idEmpleado = cursor.getString(
                        cursor.getColumnIndexOrThrow("id_empleado"));
                token = cursor.getString(
                        cursor.getColumnIndexOrThrow("token_sesion"));
            }
        } catch (RuntimeException e) {
            android.util.Log.e("REPORTE_API", "Error leyendo la sesión", e);
            Toast.makeText(this,
                    "No se pudo leer la sesión",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (idEmpleado == null || idEmpleado.trim().isEmpty()
                || token == null || token.trim().isEmpty()) {
            Toast.makeText(this,
                    "Inicia sesión con internet para consultar el reporte",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Calendar calendario = Calendar.getInstance();
        int mes = calendario.get(Calendar.MONTH) + 1;
        int anio = calendario.get(Calendar.YEAR);

        txtMesReporte.setText(
                "MES: " + NOMBRES_MES[mes - 1] + " " + anio);

        apiService.obtenerReporte(
                "Bearer " + token.trim(), mes, anio, idEmpleado
        ).enqueue(new Callback<ReporteResponse>() {
            @Override
            public void onResponse(
                    Call<ReporteResponse> call,
                    Response<ReporteResponse> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    mostrarReporte(response.body());

                    android.util.Log.d("REPORTE_API",
                            "Reporte recibido. Total kg: "
                                    + response.body().totalKg);
                    return;
                }

                android.util.Log.e("REPORTE_API",
                        "No se pudo cargar. HTTP " + response.code());

                String mensaje = response.code() == 401
                        ? "Tu sesión no es válida o expiró. Inicia sesión otra vez."
                        : "No se pudo cargar el reporte. HTTP " + response.code();

                Toast.makeText(
                        ReporteActivity.this,
                        mensaje,
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onFailure(
                    Call<ReporteResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;

                android.util.Log.e(
                        "REPORTE_API", "Error consultando el reporte", t);

                Toast.makeText(
                        ReporteActivity.this,
                        "No se pudo conectar o procesar la respuesta del servidor",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void mostrarReporte(ReporteResponse reporte) {
        containerMateriales.removeAllViews();
        containerLeyenda.removeAllViews();

        // Agrupa nombres antiguos y códigos actuales de una misma categoría.
        Map<String, Double> agrupados = new LinkedHashMap<>();

        if (reporte.porTipo != null) {
            for (TipoTotalDto item : reporte.porTipo) {
                if (item == null || !Double.isFinite(item.totalKg)
                        || item.totalKg <= 0) {
                    continue;
                }

                String codigo = normalizarTipo(item.tipo);

                agrupados.put(
                        codigo,
                        agrupados.getOrDefault(codigo, 0.0) + item.totalKg
                );
            }
        }

        // Usa los mismos valores para texto, porcentajes y gráfico.
        double total = 0;
        for (double kg : agrupados.values()) {
            total += kg;
        }

        txtTotalKg.setText("Total: " + formatoPeso(total) + " kg");

        datosExportacion.clear();
        datosExportacion.putAll(agrupados);
        reporteDisponible = true;

        double[] valores = new double[agrupados.size()];
        int[] colores = new int[agrupados.size()];

        int indice = 0;

        for (Map.Entry<String, Double> entrada : agrupados.entrySet()) {
            String codigo = entrada.getKey();
            double kg = entrada.getValue();

            String etiqueta = ETIQUETAS_TIPO.getOrDefault(
                    codigo,
                    codigo.replace('_', ' ').toUpperCase(Locale.ROOT)
            );

            int color = colorDelTipo(codigo);
            double porcentaje = total > 0 ? kg / total * 100.0 : 0;

            valores[indice] = kg;
            colores[indice] = color;
            indice++;

            String porcentajeTexto = String.format(
                    Locale.getDefault(), "%.1f", porcentaje);

            // Resumen superior.
            TextView fila = new TextView(this);
            fila.setText(
                    etiqueta + ": " + formatoPeso(kg)
                            + " kg (" + porcentajeTexto + "%)"
            );
            fila.setTextColor(Color.BLACK);
            fila.setTextSize(15);
            fila.setTypeface(
                    fila.getTypeface(), android.graphics.Typeface.BOLD);
            fila.setPadding(0, dpReporte(4), 0, 0);
            containerMateriales.addView(fila);

            // Leyenda: mismo color que el segmento correspondiente.
            LinearLayout itemLeyenda = new LinearLayout(this);
            itemLeyenda.setOrientation(LinearLayout.HORIZONTAL);
            itemLeyenda.setGravity(android.view.Gravity.CENTER_VERTICAL);

            View muestra = new View(this);
            muestra.setBackgroundColor(color);
            muestra.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            LinearLayout.LayoutParams cuadro =
                    new LinearLayout.LayoutParams(
                            dpReporte(10), dpReporte(10));
            cuadro.setMargins(0, 0, dpReporte(6), 0);
            itemLeyenda.addView(muestra, cuadro);

            TextView texto = new TextView(this);
            texto.setText(etiqueta + "\n" + porcentajeTexto + "%");
            texto.setTextSize(10);
            texto.setTextColor(Color.BLACK);

            itemLeyenda.addView(
                    texto,
                    new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)
            );

            LinearLayout.LayoutParams filaLeyenda =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            filaLeyenda.setMargins(0, 0, 0, dpReporte(8));
            containerLeyenda.addView(itemLeyenda, filaLeyenda);
        }

        View donut = findViewById(R.id.donutPlaceholder);
        donut.setBackground(new DonutDrawable(valores, colores));

        if (agrupados.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("No hay residuos registrados este mes");
            vacio.setTextColor(Color.BLACK);
            containerMateriales.addView(vacio);
        }
    }

    private String formatearKg(double valor) {
        if (valor == Math.floor(valor)) {
            return String.valueOf((int) valor);
        }
        return String.valueOf(valor);
    }
    private int dpReporte(int valor) {
        return Math.round(
                valor * getResources().getDisplayMetrics().density);
    }

    private String formatoPeso(double valor) {
        return String.format(Locale.getDefault(), "%.2f", valor);
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) return "otros";

        String valor = Normalizer.normalize(tipo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        switch (valor) {
            case "papel_carton":
            case "papel_y_carton":
                return "papel_carton";

            case "plastico":
            case "plasticos":
                return "plastico";

            case "metal":
            case "metales":
                return "metal";

            case "restos_de_comida":
            case "restos_comida":
                return "restos_de_comida";

            case "residuos_de_jardin":
            case "residuos_jardin":
                return "residuos_de_jardin";

            case "pilas_baterias":
            case "pilas_y_baterias":
                return "pilas_baterias";

            case "material_punzocortante":
            case "materiales_punzocortantes":
                return "material_punzocortante";

            default:
                return valor.isEmpty() ? "otros" : valor;
        }
    }

    private int colorDelTipo(String codigo) {
        switch (codigo) {
            case "plastico":
                return Color.parseColor("#66BB6A");
            case "metal":
                return Color.parseColor("#FF7043");
            case "papel_carton":
                return Color.parseColor("#3949AB");
            case "pilas_baterias":
                return Color.parseColor("#2E7D32");
            case "material_punzocortante":
                return Color.parseColor("#81C784");
            case "restos_de_comida":
                return Color.parseColor("#FFB74D");
            case "residuos_de_jardin":
                return Color.parseColor("#AED581");
            default:
                return Color.parseColor("#757575");
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (archivoExportacion != null) {
            outState.putString(
                    "archivo_exportacion",
                    archivoExportacion.getAbsolutePath()
            );
        }
        super.onSaveInstanceState(outState);
    }
}