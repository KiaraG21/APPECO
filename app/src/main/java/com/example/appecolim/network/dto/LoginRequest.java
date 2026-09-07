package com.example.appecolim.network.dto;

public class LoginRequest {
    public String codigo;
    public String password;

    public LoginRequest(String codigo, String password) {
        this.codigo = codigo;
        this.password = password;
    }
}