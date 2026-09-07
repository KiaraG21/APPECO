package com.example.appecolim.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper {

    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "ecoreg.db";
    private static final int DATABASE_VERSION = 1;

    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public DataBaseHelper(Context context, boolean pruebasClasificacion) {
        super(context, pruebasClasificacion ? "ecoreg_clasificacion_pruebas.db" : DATABASE_NAME,
                null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabla del empleado (datos guardados en el celular para login offline)
        String crearTablaEmpleado = "CREATE TABLE empleado_local (" +
                "id_empleado TEXT PRIMARY KEY, " +
                "nombre TEXT, " +
                "cargo TEXT, " +
                "turno TEXT, " +
                "codigo TEXT, " +
                "token_sesion TEXT, " +
                "ultima_sync TEXT)";
        db.execSQL(crearTablaEmpleado);

        // Tabla de residuos (cada registro que hace el empleado)
        String crearTablaResiduo = "CREATE TABLE residuo_local (" +
                "id_local INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_empleado TEXT, " +
                "tipo TEXT, " +
                "cantidad_kg REAL, " +
                "fecha_hora TEXT, " +
                "estado_sync TEXT, " +
                "id_mongo TEXT)";
        db.execSQL(crearTablaResiduo);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Si en el futuro cambian la estructura de las tablas, aquí se maneja
        db.execSQL("DROP TABLE IF EXISTS empleado_local");
        db.execSQL("DROP TABLE IF EXISTS residuo_local");
        onCreate(db);
    }
}