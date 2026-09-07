package com.example.appecolim.network.dto;

import com.google.gson.annotations.SerializedName;

public class EmpleadoDto {
    @SerializedName("id_empleado")
    public String idEmpleado;

    public String nombre;
    public String cargo;
    public String turno;
    public String codigo;
    public String rol;
}