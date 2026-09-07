package com.example.appecolim.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/*
 * NOTA DE SEGURIDAD:
 * Todas las consultas de este archivo usan parámetros (?) en vez de concatenar
 * texto directamente en el query. Esto previene ataques de inyección SQL,
 * ya que SQLite siempre trata el valor como un dato puro, nunca como parte
 * del comando SQL.
 */
public class ResiduoDAO {

    private DataBaseHelper dbHelper;

    public ResiduoDAO(Context context) {
        dbHelper = new DataBaseHelper(context);
    }

    // 1. Guardar un residuo nuevo (cuando el empleado da "Aceptar" en Confirmación)
    public long guardarResiduo(String idEmpleado, String tipo, double cantidadKg, String fechaHora) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id_empleado", idEmpleado);
        values.put("tipo", tipo);
        values.put("cantidad_kg", cantidadKg);
        values.put("fecha_hora", fechaHora);
        values.put("estado_sync", "pendiente"); // recién creado, aún no se sube a MongoDB
        values.put("id_mongo", (String) null);   // todavía no existe, se llena después al sincronizar

        return db.insert("residuo_local", null, values);
    }

    // 2. Traer los residuos de HOY (para la pestaña "Hoy")
    public Cursor obtenerResiduosDeHoy(String fechaHoy) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // fechaHoy debe venir como "2026-08-25" para que el LIKE funcione bien
        return db.rawQuery(
                "SELECT * FROM residuo_local WHERE fecha_hora LIKE ? ORDER BY id_local DESC",
                new String[]{fechaHoy + "%"}
        );
    }

    // 3. Traer el Historial completo, con filtros opcionales (fecha y/o tipo)
    public Cursor obtenerHistorial(String fecha, String tipo) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM residuo_local WHERE 1=1";
        java.util.List<String> parametros = new java.util.ArrayList<>();

        if (fecha != null && !fecha.isEmpty()) {
            query += " AND fecha_hora LIKE ?";
            parametros.add(fecha + "%");
        }
        if (tipo != null && !tipo.isEmpty()) {
            query += " AND tipo = ?";
            parametros.add(tipo);
        }
        query += " ORDER BY fecha_hora DESC";

        return db.rawQuery(query, parametros.toArray(new String[0]));
    }

    // 4. Editar un registro de HOY: tipo y/o cantidad (si el empleado se equivocó)
    public boolean actualizarResiduo(int idLocal, String nuevoTipo, double nuevaCantidad) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("tipo", nuevoTipo);
        values.put("cantidad_kg", nuevaCantidad);
        values.put("estado_sync", "pendiente"); // como cambió, hay que volver a sincronizar

        int filas = db.update("residuo_local", values, "id_local = ?", new String[]{String.valueOf(idLocal)});
        return filas > 0;
    }

    // 5. Marcar un registro como sincronizado (cuando la API confirma que ya subió a MongoDB)
    public void marcarComoSincronizado(int idLocal, String idMongo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("estado_sync", "sincronizado");
        values.put("id_mongo", idMongo);

        db.update("residuo_local", values, "id_local = ?", new String[]{String.valueOf(idLocal)});
    }

    // 6. Traer todos los registros PENDIENTES (para saber qué falta subir a MongoDB)
    public Cursor obtenerPendientesDeSincronizar() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM residuo_local WHERE estado_sync = ?",
                new String[]{"pendiente"}
        );
    }
}