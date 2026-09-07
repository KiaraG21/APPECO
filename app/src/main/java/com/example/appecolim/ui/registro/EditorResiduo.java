package com.example.appecolim.ui.registro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appecolim.data.local.DataBaseHelper;
import com.example.appecolim.data.model.Residuo;
import com.example.appecolim.network.RetrofitClient;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.Locale;

import retrofit2.Response;

public final class EditorResiduo {

    private EditorResiduo() {}

    private static final String[] NOMBRES = {
            "Restos de comida",
            "Papel/Cartón",
            "Pilas y Baterías",
            "Residuos de jardín",
            "Metales",
            "Material Punzocortante",
            "Plásticos"
    };

    private static final String[] CODIGOS = {
            "restos_de_comida",
            "papel_carton",
            "pilas_baterias",
            "residuos_de_jardin",
            "metal",
            "material_punzocortante",
            "plastico"
    };

    public static void mostrar(
            AppCompatActivity activity,
            Residuo residuo,
            Runnable alGuardar) {

        LinearLayout contenido = new LinearLayout(activity);
        contenido.setOrientation(LinearLayout.VERTICAL);

        int margen = Math.round(
                24 * activity.getResources().getDisplayMetrics().density);
        contenido.setPadding(margen, margen, margen, margen);

        TextView etiquetaTipo = new TextView(activity);
        etiquetaTipo.setText("Tipo de residuo");
        contenido.addView(etiquetaTipo);

        Spinner tipo = new Spinner(activity);
        ArrayAdapter<String> opciones = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_item,
                NOMBRES);
        opciones.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        tipo.setAdapter(opciones);

        int seleccion = -1;
        for (int i = 0; i < NOMBRES.length; i++) {
            if (NOMBRES[i].equalsIgnoreCase(residuo.getTipo())
                    || CODIGOS[i].equalsIgnoreCase(residuo.getTipo())) {
                seleccion = i;
                break;
            }
        }

        if (seleccion < 0) {
            Toast.makeText(activity,
                    "No se reconoce la categoría de este registro",
                    Toast.LENGTH_LONG).show();
            return;
        }

        tipo.setSelection(seleccion);
        contenido.addView(tipo);

        TextView etiquetaPeso = new TextView(activity);
        etiquetaPeso.setText("Peso en kilogramos");
        contenido.addView(etiquetaPeso);

        EditText peso = new EditText(activity);
        peso.setSingleLine(true);
        peso.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        peso.setText(String.format(
                Locale.US, "%.2f", residuo.getCantidadKg()));
        peso.setSelectAllOnFocus(true);
        contenido.addView(peso);

        AlertDialog dialogo = new AlertDialog.Builder(activity)
                .setTitle("Editar residuo")
                .setView(contenido)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialogo.setOnShowListener(ignored ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            final double cantidad;

                            try {
                                BigDecimal valor = new BigDecimal(
                                        peso.getText().toString()
                                                .trim().replace(',', '.'));

                                if (valor.signum() <= 0
                                        || valor.compareTo(
                                        new BigDecimal("999999.99")) > 0
                                        || valor.stripTrailingZeros().scale() > 2) {
                                    throw new NumberFormatException();
                                }

                                cantidad = valor.doubleValue();
                            } catch (NumberFormatException e) {
                                peso.setError(
                                        "Ingresa un peso mayor que 0, "
                                                + "con hasta dos decimales");
                                return;
                            }

                            int indice = tipo.getSelectedItemPosition();
                            String nombre = NOMBRES[indice];
                            String codigo = CODIGOS[indice];

                            dialogo.setCancelable(false);
                            tipo.setEnabled(false);
                            peso.setEnabled(false);
                            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(false);
                            dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setEnabled(false);

                            new Thread(() -> {
                                String error = actualizar(
                                        activity, residuo,
                                        nombre, codigo, cantidad);

                                activity.runOnUiThread(() -> {
                                    if (activity.isFinishing()
                                            || activity.isDestroyed()) return;

                                    if (error == null) {
                                        dialogo.dismiss();
                                        alGuardar.run();
                                        Toast.makeText(activity,
                                                "Registro actualizado",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        dialogo.setCancelable(true);
                                        tipo.setEnabled(true);
                                        peso.setEnabled(true);
                                        dialogo.getButton(
                                                        AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(true);
                                        dialogo.getButton(
                                                        AlertDialog.BUTTON_NEGATIVE)
                                                .setEnabled(true);

                                        Toast.makeText(activity,
                                                error,
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }, "editar-residuo").start();
                        }));

        dialogo.show();
    }

    private static String actualizar(
            AppCompatActivity activity,
            Residuo residuo,
            String nombre,
            String codigo,
            double cantidad) {

        boolean servidorConfirmado = false;

        try (DataBaseHelper helper =
                     new DataBaseHelper(activity.getApplicationContext())) {
            SQLiteDatabase db = helper.getWritableDatabase();

            String empleado;
            String token;
            String idMongo;
            String estado;
            String idLocal = String.valueOf(residuo.getIdLocal());

            String consulta =
                    "SELECT r.id_empleado, r.id_mongo, r.estado_sync, "
                            + "e.token_sesion "
                            + "FROM residuo_local r "
                            + "JOIN empleado_local e "
                            + "ON e.id_empleado = r.id_empleado "
                            + "WHERE r.id_local = ?";

            try (Cursor cursor = db.rawQuery(
                    consulta, new String[]{idLocal})) {
                if (!cursor.moveToFirst()) {
                    return "No se encontró el registro para la sesión actual.";
                }

                empleado = cursor.getString(0);
                idMongo = cursor.getString(1);
                estado = cursor.getString(2);
                token = cursor.getString(3);
            }

            if (idMongo == null || idMongo.trim().isEmpty()
                    || !"sincronizado".equals(estado)) {
                return "Este registro está pendiente de sincronización. "
                        + "Primero debe sincronizarse para editarlo en la API.";
            }

            if (token == null || token.trim().isEmpty()) {
                return "Inicia sesión con internet antes de editar.";
            }

            JsonObject cambios = new JsonObject();
            cambios.addProperty("tipo", codigo);
            cambios.addProperty("cantidad_kg", cantidad);

            Response<JsonObject> respuesta =
                    RetrofitClient.getApiService()
                            .actualizarResiduo(
                                    "Bearer " + token.trim(),
                                    idLocal, idMongo, cambios)
                            .execute();

            if (!respuesta.isSuccessful()) {
                android.util.Log.e("EDITAR_RESIDUO",
                        "Error HTTP " + respuesta.code());

                if (respuesta.errorBody() != null) {
                    respuesta.errorBody().close();
                }

                if (respuesta.code() == 401) {
                    return "Tu sesión expiró o no es válida. "
                            + "Inicia sesión nuevamente.";
                }

                if (respuesta.code() == 404) {
                    return "El servidor no encontró este registro "
                            + "para tu empleado.";
                }

                return "No se pudo actualizar. HTTP " + respuesta.code();
            }

            servidorConfirmado = true;

            // Comprueba que la respuesta corresponde al registro editado.
            JsonObject cuerpo = respuesta.body();
            JsonObject actualizado = cuerpo == null
                    ? null : cuerpo.getAsJsonObject("residuo");

            if (actualizado == null || !actualizado.has("_id")
                    || !idMongo.equals(actualizado.get("_id").getAsString())) {
                return "La respuesta del servidor no identifica "
                        + "correctamente el registro. Revisa el reporte.";
            }

            ContentValues valores = new ContentValues();
            valores.put("tipo", nombre);
            valores.put("cantidad_kg", cantidad);
            valores.put("estado_sync", "sincronizado");

            int filas = db.update(
                    "residuo_local",
                    valores,
                    "id_local = ? AND id_empleado = ? AND id_mongo = ?",
                    new String[]{idLocal, empleado, idMongo});

            if (filas != 1) {
                return "El servidor actualizó el registro, "
                        + "pero no se pudo actualizar su copia local.";
            }

            android.util.Log.d("EDITAR_RESIDUO",
                    "Actualizado en servidor y SQLite. idLocal=" + idLocal);
            return null;

        } catch (java.io.IOException e) {
            android.util.Log.e("EDITAR_RESIDUO",
                    "No se pudo confirmar la respuesta", e);
            return "No se pudo confirmar la actualización. "
                    + "Revisa el reporte al recuperar la conexión.";
        } catch (RuntimeException e) {
            android.util.Log.e("EDITAR_RESIDUO",
                    "Error procesando la edición", e);
            return servidorConfirmado
                    ? "El servidor respondió correctamente, "
                      + "pero falló la actualización local. Revisa el reporte."
                    : "No se pudo procesar la edición.";
        }
    }
}