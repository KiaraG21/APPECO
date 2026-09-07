package com.example.appecolim.data.model;

/**
 * Clase que representa un registro de residuo.
 * Los nombres de los atributos coinciden con los de la base de datos.
 */
public class Residuo {
    private int idLocal;
    private String tipo;
    private double cantidadKg;
    private String fechaHora;

    public Residuo(int idLocal, String tipo, double cantidadKg, String fechaHora) {
        this.idLocal = idLocal;
        this.tipo = tipo;
        this.cantidadKg = cantidadKg;
        this.fechaHora = fechaHora;
    }

    // Getters
    public int getIdLocal() { return idLocal; }
    public String getTipo() { return tipo; }
    public double getCantidadKg() { return cantidadKg; }
    public String getFechaHora() { return fechaHora; }
}
