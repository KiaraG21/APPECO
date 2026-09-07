package com.example.appecolim.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ReporteResponse {
    public int mes;
    public int anio;

    @SerializedName("por_tipo")
    public List<TipoTotalDto> porTipo;

    @SerializedName("total_kg")
    public double totalKg;
}