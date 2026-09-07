package com.example.appecolim.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EmpleadoDAO {

    private DataBaseHelper dbHelper;

    public EmpleadoDAO(Context context) {
        dbHelper = new DataBaseHelper(context);
    }

    // Guarda (o reemplaza) los datos del empleado que acaba de iniciar sesión
    public void guardarEmpleado(String idEmpleado, String nombre, String cargo,
                                String turno, String codigo, String tokenSesion, String ultimaSync) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id_empleado", idEmpleado);
        values.put("nombre", nombre);
        values.put("cargo", cargo);
        values.put("turno", turno);
        values.put("codigo", codigo);
        values.put("token_sesion", tokenSesion);
        values.put("ultima_sync", ultimaSync);

        // Borra cualquier empleado guardado antes (solo puede haber uno logueado a la vez)
        db.delete("empleado_local", null, null);

        // Guarda el nuevo
        db.insert("empleado_local", null, values);
    }

    // Lee al empleado guardado (para saber si ya hay alguien logueado)
    public Cursor obtenerEmpleadoGuardado() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM empleado_local LIMIT 1", null);
    }

    // Borra el empleado guardado (para cuando alguien cierra sesión)
    public void cerrarSesion() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("empleado_local", null, null);
    }
}