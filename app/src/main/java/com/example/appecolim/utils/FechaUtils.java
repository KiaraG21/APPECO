package com.example.appecolim.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilidad para manejar el formato de fecha_hora de la base de datos.
 */
public class FechaUtils {

    // Formato estándar: "yyyy-MM-dd HH:mm:ss"
    private static final String FORMATO_BD = "yyyy-MM-dd HH:mm:ss";

    /**
     * Obtiene la fecha y hora actual para guardar en la BD.
     */
    public static String obtenerFechaHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMATO_BD, Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Extrae solo la HORA de un string "yyyy-MM-dd HH:mm:ss"
     */
    public static String extraerHora(String fechaHoraCompleta) {
        try {
            if (fechaHoraCompleta == null || !fechaHoraCompleta.contains(" ")) return fechaHoraCompleta;
            String horaPart = fechaHoraCompleta.split(" ")[1]; // Tomamos la segunda parte
            return horaPart.substring(0, 5); // Retorna "HH:mm"
        } catch (Exception e) {
            return fechaHoraCompleta;
        }
    }

    /**
     * Extrae solo la FECHA de un string "yyyy-MM-dd HH:mm:ss"
     */
    public static String extraerFecha(String fechaHoraCompleta) {
        try {
            if (fechaHoraCompleta == null || !fechaHoraCompleta.contains(" ")) return fechaHoraCompleta;
            return fechaHoraCompleta.split(" ")[0]; // Retorna "yyyy-MM-dd"
        } catch (Exception e) {
            return fechaHoraCompleta;
        }
    }
}
