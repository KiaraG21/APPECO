package com.example.appecolim.clasificacion;

import android.app.Application;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.example.appecolim.data.local.DataBaseHelper;
import com.example.appecolim.utils.FechaUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Mantiene un único guardado durante rotaciones y persiste fuera del hilo de interfaz. */
public class RegistroViewModel extends AndroidViewModel {
    public static final class Resultado {
        final long id; final String error;
        Resultado(long id, String error) { this.id = id; this.error = error; }
    }
    final MutableLiveData<Resultado> resultado = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    volatile boolean guardando;
    public RegistroViewModel(Application app) { super(app); }
    void guardar(Categoria categoria, int cantidad, boolean vistaPrevia) {
        if (guardando || cantidad <= 0 || cantidad > Cantidad.MAXIMO) return;
        guardando = true;

        executor.execute(() -> {
            Resultado salida;
            boolean pruebas = com.example.appecolim.BuildConfig.CLASIFICACION_PREVIEW && vistaPrevia;

            String idEmpleadoUsado = null;
            String idLocalGenerado = null;
            String tipoGuardado = null;
            double cantidadKgGuardada = 0;
            String fechaHoraGuardada = null;

            try (DataBaseHelper helper = new DataBaseHelper(getApplication(), pruebas)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                try {
                    String empleado = null;
                    try (Cursor cursor = db.rawQuery("SELECT id_empleado FROM empleado_local LIMIT 1", null)) {
                        if (cursor.moveToFirst()) empleado = cursor.getString(0);
                    }
                    if (pruebas) empleado = "prueba-clasificacion";

                    if (empleado == null || empleado.trim().isEmpty()) {
                        salida = new Resultado(-1, "Inicia sesión antes de guardar un registro.");
                    } else {
                        String fechaHora = FechaUtils.obtenerFechaHoraActual();
                        double cantidadKg = cantidad / 100.0;

                        ContentValues values = new ContentValues();
                        values.put("id_empleado", empleado);
                        values.put("tipo", categoria.nombre);
                        values.put("cantidad_kg", cantidadKg);
                        values.put("fecha_hora", fechaHora);
                        values.put("estado_sync", "pendiente");
                        values.putNull("id_mongo");

                        long id = db.insertOrThrow("residuo_local", null, values);
                        db.setTransactionSuccessful();
                        salida = new Resultado(id, null);

                        // Guarda los datos para intentar sincronizar DESPUÉS de cerrar la transacción
                        idEmpleadoUsado = empleado;
                        idLocalGenerado = String.valueOf(id);
                        tipoGuardado = categoria.nombre;
                        cantidadKgGuardada = cantidadKg;
                        fechaHoraGuardada = fechaHora;
                    }
                } finally {
                    db.endTransaction();
                }
            } catch (RuntimeException e) {
                salida = new Resultado(-1, "No se pudo guardar el registro. Inténtalo de nuevo.");
            }

            // Intenta sincronizar con el servidor (sin bloquear el guardado local, que ya se hizo)
            if (idEmpleadoUsado != null && !pruebas) {
                intentarSincronizar(idEmpleadoUsado, idLocalGenerado, tipoGuardado,
                        cantidadKgGuardada, fechaHoraGuardada);
            }

            resultado.postValue(salida);
        });
    }

    // Envía el residuo recién guardado al servidor y, si tiene éxito, marca el registro
// local como sincronizado. Si falla (sin internet, servidor caído, etc.), el registro
// se queda como "pendiente" en SQLite para intentarlo más adelante.
    private void intentarSincronizar(String idEmpleado, String idLocal,
                                     String tipo, double cantidadKg,
                                     String fechaHora) {
        final String TAG = "SYNC_RESIDUO";

        try {
            // Recupera el token de la misma persona que creó el registro.
            String token = null;

            try (DataBaseHelper helper =
                         new DataBaseHelper(getApplication(), false)) {
                SQLiteDatabase db = helper.getReadableDatabase();

                try (Cursor cursor = db.rawQuery(
                        "SELECT token_sesion FROM empleado_local "
                                + "WHERE id_empleado = ? LIMIT 1",
                        new String[]{idEmpleado})) {
                    if (cursor.moveToFirst()) {
                        token = cursor.getString(0);
                    }
                }
            }

            if (token == null || token.trim().isEmpty()) {
                android.util.Log.e(TAG,
                        "No hay token guardado para este empleado. "
                                + "Inicia sesión con internet. "
                                + "El residuo sigue pendiente en SQLite.");
                return;
            }

            // Convierte el nombre visible al código utilizado por la API.
            String tipoApi;
            switch (tipo) {
                case "Restos de comida":
                    tipoApi = "restos_de_comida";
                    break;
                case "Papel/Cartón":
                    tipoApi = "papel_carton";
                    break;
                case "Pilas y Baterías":
                    tipoApi = "pilas_baterias";
                    break;
                case "Residuos de jardín":
                    tipoApi = "residuos_de_jardin";
                    break;
                case "Metales":
                    tipoApi = "metal";
                    break;
                case "Material Punzocortante":
                    tipoApi = "material_punzocortante";
                    break;
                case "Plásticos":
                    tipoApi = "plastico";
                    break;
                default:
                    android.util.Log.e(TAG,
                            "Categoría no reconocida. El residuo sigue pendiente.");
                    return;
            }

            com.example.appecolim.network.dto.ResiduoRequest request =
                    new com.example.appecolim.network.dto.ResiduoRequest(
                            idEmpleado, idLocal, tipoApi,
                            cantidadKg, fechaHora);

            android.util.Log.d(TAG,
                    "Intentando sincronizar con autorización: idLocal="
                            + idLocal + " tipo=" + tipoApi);

            retrofit2.Response<com.example.appecolim.network.dto.ResiduoResponse>
                    response =
                    com.example.appecolim.network.RetrofitClient.getApiService()
                            .crearResiduo("Bearer " + token.trim(), request)
                            .execute();

            if (!response.isSuccessful()) {
                String detalle = response.errorBody() != null
                        ? response.errorBody().string()
                        : "sin cuerpo";

                android.util.Log.e(TAG,
                        "Respuesta NO exitosa. Código: " + response.code()
                                + " Body: " + detalle);

                if (response.code() == 401) {
                    android.util.Log.e(TAG,
                            "Inicia sesión nuevamente con internet. "
                                    + "El residuo permanece pendiente.");
                }
                return;
            }

            com.example.appecolim.network.dto.ResiduoResponse cuerpo =
                    response.body();

            if (cuerpo == null || cuerpo.idMongo == null
                    || cuerpo.idMongo.trim().isEmpty()) {
                android.util.Log.e(TAG,
                        "El servidor respondió sin id_mongo. "
                                + "No se marcará como sincronizado.");
                return;
            }

            try (DataBaseHelper helper =
                         new DataBaseHelper(getApplication(), false)) {
                SQLiteDatabase db = helper.getWritableDatabase();

                ContentValues valores = new ContentValues();
                valores.put("estado_sync", "sincronizado");
                valores.put("id_mongo", cuerpo.idMongo);

                int filas = db.update(
                        "residuo_local",
                        valores,
                        "id_local = ? AND id_empleado = ?",
                        new String[]{idLocal, idEmpleado});

                android.util.Log.d(TAG,
                        "Servidor confirmó el registro: id_mongo="
                                + cuerpo.idMongo
                                + ". Filas actualizadas en SQLite: " + filas);
            }
        } catch (java.io.IOException e) {
            android.util.Log.e(TAG,
                    "Fallo de conexión. El residuo sigue pendiente.", e);
        } catch (RuntimeException e) {
            android.util.Log.e(TAG,
                    "No se pudo completar la sincronización. "
                            + "Revisa el estado local antes de reintentar.", e);
        }
    }
    @Override protected void onCleared() { executor.shutdown(); }
}