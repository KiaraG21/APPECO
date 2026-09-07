package com.example.appecolim.network;

import com.example.appecolim.network.dto.LoginRequest;
import com.example.appecolim.network.dto.LoginResponse;
import com.example.appecolim.network.dto.ReporteResponse;
import com.example.appecolim.network.dto.ResiduoRequest;
import com.example.appecolim.network.dto.ResiduoResponse;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.Call;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);
    @GET("reporte/{mes}/{anio}")
    Call<ReporteResponse> obtenerReporte(
            @retrofit2.http.Header("Authorization") String authorization,
            @Path("mes") int mes,
            @Path("anio") int anio,
            @Query("id_empleado") String idEmpleado
    );
    @POST("residuos")
    Call<ResiduoResponse> crearResiduo(
            @retrofit2.http.Header("Authorization") String authorization,
            @Body ResiduoRequest request
    );

    @retrofit2.http.PUT("residuos/{id_local}")
    Call<com.google.gson.JsonObject> actualizarResiduo(
            @retrofit2.http.Header("Authorization") String authorization,
            @Path("id_local") String idLocal,
            @Query("id_mongo") String idMongo,
            @Body com.google.gson.JsonObject cambios
    );
}