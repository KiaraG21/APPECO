package com.example.appecolim.ui.reporte;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class DonutDrawable extends Drawable {

    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final double[] valores;
    private final int[] colores;

    public DonutDrawable(double[] valores, int[] colores) {
        if (valores.length != colores.length) {
            throw new IllegalArgumentException(
                    "Cada valor debe tener un color");
        }

        this.valores = valores.clone();
        this.colores = colores.clone();
        pintura.setStyle(Paint.Style.STROKE);
        pintura.setStrokeCap(Paint.Cap.BUTT);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect limites = getBounds();
        float lado = Math.min(limites.width(), limites.height());

        if (lado <= 0) return;

        float grosor = lado * 0.21f;
        float radio = (lado - grosor) / 2f - 1f;
        if (radio <= 0) return;

        float centroX = limites.exactCenterX();
        float centroY = limites.exactCenterY();

        RectF ovalo = new RectF(
                centroX - radio,
                centroY - radio,
                centroX + radio,
                centroY + radio
        );

        pintura.setStrokeWidth(grosor);

        // Anillo gris cuando no hay datos.
        pintura.setColor(Color.rgb(190, 190, 190));
        canvas.drawOval(ovalo, pintura);

        double total = 0;
        int ultimoPositivo = -1;

        for (int i = 0; i < valores.length; i++) {
            if (valores[i] > 0 && Double.isFinite(valores[i])) {
                total += valores[i];
                ultimoPositivo = i;
            }
        }

        if (total <= 0 || !Double.isFinite(total)) return;

        float inicio = -90f;
        float acumulado = 0f;

        for (int i = 0; i < valores.length; i++) {
            if (valores[i] <= 0 || !Double.isFinite(valores[i])) continue;

            float angulo = i == ultimoPositivo
                    ? 360f - acumulado
                    : (float) (valores[i] / total * 360.0);

            pintura.setColor(colores[i]);
            canvas.drawArc(ovalo, inicio, angulo, false, pintura);

            inicio += angulo;
            acumulado += angulo;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        pintura.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        pintura.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}