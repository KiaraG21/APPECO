package com.example.appecolim.clasificacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Centésimas de kilogramo: evita errores acumulados de coma flotante. */
public final class Cantidad {
    public static final int MAXIMO = 99999999;
    private Cantidad() {}
    public static int leer(String texto) {
        try {
            BigDecimal valor = new BigDecimal(texto.trim().replace(',', '.'));
            int centesimas = valor.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();
            return centesimas >= 0 && centesimas <= MAXIMO ? centesimas : -1;
        } catch (NumberFormatException | ArithmeticException e) { return -1; }
    }
    public static String mostrar(int valor) { return BigDecimal.valueOf(valor, 2).toPlainString(); }
}