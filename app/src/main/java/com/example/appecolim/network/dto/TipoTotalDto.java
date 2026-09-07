package com.example.appecolim.network.dto;

import com.google.gson.annotations.SerializedName;

public class TipoTotalDto {
    public String tipo;

    @SerializedName("total_kg")
    public double totalKg;
}