package com.example.appecolim;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;


public class SplashActivity extends AppCompatActivity {

    // Tiempo total que se muestra el splash antes de pasar a la siguiente pantalla.
    private static final long TIEMPO_TOTAL_SPLASH_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgSplash = findViewById(R.id.imgSplashCompleto);

        // Carga la animación definida en res/anim/
        Animation animLogo = AnimationUtils.loadAnimation(this, R.anim.anim_logo_aparecer);

        // Hace visible la imagen justo antes de animarla (estaba en "invisible" en el XML)
        imgSplash.setVisibility(View.VISIBLE);
        imgSplash.startAnimation(animLogo);

        // Después de la animación, pasa a la siguiente pantalla (Login/Main)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, TIEMPO_TOTAL_SPLASH_MS);
    }
}
