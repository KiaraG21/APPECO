package com.example.appecolim.network.dto;

public class ResiduoRequest {
    public String id_empleado;
    public String id_local;
    public String tipo;
    public double cantidad_kg;
    public String fecha_hora;

    public ResiduoRequest(String idEmpleado, String idLocal, String tipo,
                          double cantidadKg, String fechaHora) {
        this.id_empleado = idEmpleado;
        this.id_local = idLocal;
        this.tipo = tipo;
        this.cantidad_kg = cantidadKg;
        this.fecha_hora = fechaHora;
    }
}