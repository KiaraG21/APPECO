package com.example.appecolim.clasificacion;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import androidx.core.content.res.ResourcesCompat;
import com.example.appecolim.R;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.text.Normalizer;
import java.util.Locale;

/** Flujo de Clasificación, aislado de las pantallas existentes. */
public class ClasificacionActivity extends AppCompatActivity {
    private static final int VERDE = Color.rgb(9, 139, 89), GRIS = Color.rgb(217, 217, 217);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RegistroViewModel modelo;
    private LinearLayout raiz;
    private Categoria categoria;
    private EditText cantidad;
    private int valor, pagina;
    private boolean exito, activo;
    private String busqueda = "";
    private Dialog confirmacion;
    private HorizontalScrollView carrusel;
    private TextView guardar;
    private final Runnable avanzar = new Runnable() {
        @Override public void run() {
            if (activo && carrusel != null && confirmacion == null && !exito) {
                pagina = (pagina + 1) % 2;
                carrusel.smoothScrollTo(pagina * carrusel.getWidth(), 0);
                handler.postDelayed(this, 4500);
            }
        }
    };
    private final Runnable volver = this::mostrarMenu;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        modelo = new ViewModelProvider(this).get(RegistroViewModel.class);
        if (saved != null) {
            String nombre = saved.getString("categoria");
            if (nombre != null) categoria = Categoria.valueOf(nombre);
            valor = saved.getInt("valor"); pagina = saved.getInt("pagina");
            busqueda = saved.getString("busqueda", ""); exito = saved.getBoolean("exito");
        }
        if (exito) mostrarExito(); else if (categoria != null) mostrarDetalle(); else mostrarMenu();
        modelo.resultado.observe(this, resultado -> {
            if (resultado == null) return;
            modelo.resultado.setValue(null);
            modelo.guardando = false;
            if (resultado.id > 0) mostrarExito();
            else { if (guardar != null) guardar.setEnabled(true); aviso(resultado.error); }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (modelo.guardando) return;
                if (categoria != null || exito) mostrarMenu(); else finish();
            }
        });
        if (saved != null && saved.getBoolean("confirmacion") && !modelo.guardando && !exito) {
            raiz.post(this::confirmar);
        }
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable fondo(int color, int radio) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radio)); return d;
    }
    private TextView texto(String contenido, int size, int color, boolean negrita) {
        TextView t = new TextView(this); t.setText(contenido); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(ResourcesCompat.getFont(this, negrita ? R.font.inter_bold : R.font.inter_regular));
        return t;
    }
    private ImageView imagen(String nombre) {
        ImageView v = new ImageView(this);
        int id = getResources().getIdentifier("figma_" + nombre, "drawable", getPackageName());
        if (id != 0) v.setImageResource(id);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return v;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void espacio(LinearLayout padre, int alto) { padre.addView(new View(this), new LinearLayout.LayoutParams(1, dp(alto))); }
    private void preparar(boolean degradado) {
        handler.removeCallbacks(avanzar); handler.removeCallbacks(volver); carrusel = null;
        if (confirmacion != null) { confirmacion.dismiss(); confirmacion = null; }
        raiz = vertical();
        if (degradado) raiz.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#05593D"), Color.parseColor("#060C09")}));
        else raiz.setBackgroundColor(Color.parseColor("#F9F9F9"));
        setContentView(raiz);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        ViewCompat.requestApplyInsets(raiz);
    }
    private void cabecera(String titulo, boolean menu) {
        LinearLayout fila = new LinearLayout(this); fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(dp(12), 0, dp(12), 0);
        if (menu) fila.setBackgroundColor(Color.parseColor("#103C31"));
        ImageView atras = imagen("arrow"); atras.setRotation(180); atras.setPadding(dp(10), dp(16), dp(10), dp(16));
        atras.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        atras.setContentDescription("Volver"); atras.setFocusable(true);
        atras.setOnClickListener(v -> { if (!modelo.guardando) { if (menu) finish(); else mostrarMenu(); } });
        fila.addView(atras, new LinearLayout.LayoutParams(dp(48), dp(64)));
        View separacion = new View(this); fila.addView(separacion, new LinearLayout.LayoutParams(dp(20), 1));
        TextView t = texto(titulo, menu ? 23 : 18, Color.WHITE, true);
        t.setSingleLine(true);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(t, 12, menu ? 22 : 18, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        fila.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        raiz.addView(fila, new LinearLayout.LayoutParams(-1, dp(70)));
    }

    private void mostrarMenu() {
        exito = false; categoria = null; guardar = null; cantidad = null; valor = 0; pagina = 0;
        preparar(false); cabecera("CLASIFICACIÓN", true);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout contenido = vertical();
        FrameLayout franja = new FrameLayout(this);
        franja.setBackgroundColor(Color.parseColor("#E7E6E6"));
        ImageView hierba = imagen("grass"); FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(dp(92), dp(92), Gravity.END | Gravity.BOTTOM); hp.rightMargin = dp(8); franja.addView(hierba, hp);
        LinearLayout intro = vertical(); intro.setPadding(dp(22), dp(24), dp(22), dp(12));
        intro.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout linea = new LinearLayout(this); linea.setGravity(Gravity.CENTER_VERTICAL);
        TextView lema = texto("CLASIFICA TU RESIDUO", 16, Color.BLACK, true);
        android.text.SpannableString tituloIntro = new android.text.SpannableString("CLASIFICA TU RESIDUO");
        tituloIntro.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#04786B")), 13, tituloIntro.length(), 0); lema.setText(tituloIntro);
        linea.addView(lema); ImageView reciclaje = imagen("recycle"); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(16), dp(16)); rp.leftMargin = dp(4); linea.addView(reciclaje, rp); intro.addView(linea);
        TextView subtitulo = texto("Selecciona la categoria", 15, Color.BLACK, false); intro.addView(subtitulo);
        franja.addView(intro); contenido.addView(franja, new LinearLayout.LayoutParams(-1, dp(86)));
        LinearLayout panel = vertical(); panel.setPadding(dp(22), dp(20), dp(14), dp(20));
        EditText buscar = new EditText(this); buscar.setSingleLine(true); buscar.setTextSize(14);
        buscar.setTypeface(ResourcesCompat.getFont(this, R.font.inter_regular));
        android.graphics.drawable.Drawable lupa = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.figma_search);
        if (lupa != null) { lupa.setBounds(0, 0, dp(18), dp(18)); buscar.setCompoundDrawables(lupa, null, null, null); buscar.setCompoundDrawablePadding(dp(10)); }
        buscar.setHint("Buscar"); buscar.setContentDescription("Buscar categoría"); buscar.setText(busqueda);
        buscar.setTextColor(Color.BLACK); buscar.setHintTextColor(Color.GRAY);
        GradientDrawable borde = fondo(Color.WHITE, 8); borde.setStroke(dp(1), GRIS); buscar.setBackground(borde);
        buscar.setPadding(dp(14), 0, dp(14), 0); panel.addView(buscar, new LinearLayout.LayoutParams(-1, dp(44)));
        espacio(panel, 19);
        LinearLayout tarjetas = vertical(); panel.addView(tarjetas);
        construirTarjetas(tarjetas);
        buscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { busqueda = s.toString(); construirTarjetas(tarjetas); }
            public void afterTextChanged(Editable e) {}
        });
        contenido.addView(panel); scroll.addView(contenido); raiz.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }
    private String normalizar(String s) { return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT); }
    private void construirTarjetas(LinearLayout contenedor) {
        contenedor.removeAllViews(); LinearLayout fila = null; int indice = 0;
        for (Categoria cat : Categoria.values()) {
            if (!normalizar(cat.nombre + " " + cat.resumen).contains(normalizar(busqueda))) continue;
            if (indice % 2 == 0) { fila = new LinearLayout(this); fila.setGravity(Gravity.CENTER); contenedor.addView(fila); }
            LinearLayout tarjeta = vertical(); tarjeta.setGravity(Gravity.CENTER); tarjeta.setPadding(dp(6), dp(5), dp(6), dp(8));
            tarjeta.setBackground(fondo(Color.WHITE, 9));
            ImageView icono = imagen("menu_" + cat.imagen); tarjeta.addView(icono, new LinearLayout.LayoutParams(dp(52), dp(52)));
            TextView nombre = texto(cat.nombre, 12, Color.BLACK, true); nombre.setGravity(Gravity.CENTER); nombre.setSingleLine(true);
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(nombre, 10, 12, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            tarjeta.addView(nombre, new LinearLayout.LayoutParams(-1, dp(20)));
            TextView desc = texto(cat.resumen, cat == Categoria.PAPEL || cat == Categoria.JARDIN ? 10 : 12, Color.BLACK, false); desc.setGravity(Gravity.CENTER); tarjeta.addView(desc);
            tarjeta.setMinimumHeight(dp(119));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(0, 0, indice % 2 == 0 ? dp(24) : 0, dp(23));
            fila.addView(tarjeta, p);
            tarjeta.setFocusable(true); tarjeta.setContentDescription(cat.nombre + ". " + cat.resumen);
            tarjeta.setOnClickListener(v -> {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                categoria = cat; valor = 0; pagina = 0; mostrarDetalle();
            }); indice++;
        }
        if (indice == 0) contenedor.addView(texto("No se encontraron categorías", 14, Color.DKGRAY, false));
        if (indice % 2 == 1 && fila != null) {
            View tarjeta = fila.getChildAt(0);
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)tarjeta.getLayoutParams(); p.weight = 0;
            p.width = dp(169); p.rightMargin = 0; tarjeta.setLayoutParams(p);
        }
    }
    private void mostrarDetalle() {
        preparar(true); cabecera(categoria.titulo, false);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout cuerpo = vertical(); cuerpo.setPadding(dp(18), 0, dp(18), dp(36));
        crearCarrusel(cuerpo); espacio(cuerpo, 12);
        tarjetaInfo(cuerpo, "¿Qué incluye?", categoria.incluye, categoria.icono, 104);
        espacio(cuerpo, 12); tarjetaInfo(cuerpo, "¿Cómo debe estar?", categoria.condicion, "✨", 78);
        espacio(cuerpo, 20);
        TextView etiqueta = texto("Cantidad", 19, Color.WHITE, true); etiqueta.setGravity(Gravity.CENTER); cuerpo.addView(etiqueta);
        espacio(cuerpo, 10);
        LinearLayout grupo = new LinearLayout(this); grupo.setGravity(Gravity.CENTER); grupo.setBackground(fondo(GRIS, 10));
        cantidad = new EditText(this); cantidad.setId(R.id.clasificacion_cantidad); cantidad.setSingleLine(true); cantidad.setSelectAllOnFocus(true);
        cantidad.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        cantidad.setText(Cantidad.mostrar(valor)); cantidad.setTextColor(Color.BLACK); cantidad.setTextSize(18); cantidad.setGravity(Gravity.END); cantidad.setTypeface(ResourcesCompat.getFont(this, R.font.inter_bold));
        cantidad.setBackgroundColor(Color.TRANSPARENT); cantidad.setContentDescription("Cantidad en kilogramos");
        grupo.addView(cantidad, new LinearLayout.LayoutParams(dp(100), -1)); grupo.addView(texto(" kg", 16, Color.BLACK, true));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(205), dp(57)); cp.gravity = Gravity.CENTER_HORIZONTAL; cuerpo.addView(grupo, cp);
        espacio(cuerpo, 30);
        LinearLayout controles = new LinearLayout(this); controles.setGravity(Gravity.CENTER);
        TextView menos = boton("−", GRIS, Color.BLACK, () -> cambiar(-50)); menos.setTextSize(26); menos.setContentDescription("Restar medio kilogramo");
        TextView mas = boton("+", VERDE, Color.WHITE, () -> cambiar(50)); mas.setTextSize(26); mas.setContentDescription("Sumar medio kilogramo");
        menos.setBackground(fondo(GRIS, 24)); mas.setBackground(fondo(VERDE, 24));
        controles.addView(menos, new LinearLayout.LayoutParams(dp(48), dp(48))); View hueco = new View(this); controles.addView(hueco, new LinearLayout.LayoutParams(dp(24), 1));
        controles.addView(mas, new LinearLayout.LayoutParams(dp(48), dp(48))); cuerpo.addView(controles);
        espacio(cuerpo, 20); guardar = boton("Guardar Registro", VERDE, Color.WHITE, this::confirmar);
        guardar.setEnabled(!modelo.guardando); LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(56)); gp.setMargins(dp(24), 0, dp(24), 0); cuerpo.addView(guardar, gp);
        scroll.addView(cuerpo); raiz.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        programarCarrusel();
    }
    private void tarjetaInfo(LinearLayout padre, String titulo, String detalle, String icono, int alto) {
        LinearLayout fila = new LinearLayout(this); fila.setGravity(Gravity.CENTER_VERTICAL); fila.setPadding(dp(12), dp(10), dp(12), dp(10)); fila.setBackground(fondo(GRIS, 13));
        LinearLayout textos = vertical(); textos.addView(texto(titulo, 12, Color.BLACK, true)); espacio(textos, 4); textos.addView(texto(detalle, 12, Color.BLACK, false));
        fila.addView(textos, new LinearLayout.LayoutParams(0, -2, 1));
        View simbolo;
        if ("✨".equals(icono)) {
            FrameLayout estrellas = new FrameLayout(this);
            ImageView primera = imagen("star1"), segunda = imagen("star2");
            estrellas.addView(primera, new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.START | Gravity.BOTTOM));
            estrellas.addView(segunda, new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.END | Gravity.TOP));
            simbolo = estrellas;
        } else if (categoria == Categoria.METALES || categoria == Categoria.PUNZOCORTANTE) {
            simbolo = imagen("menu_" + categoria.imagen);
        } else simbolo = imagen("detail_" + categoria.imagen);
        fila.addView(simbolo, new LinearLayout.LayoutParams(dp(60), dp(50))); fila.setMinimumHeight(dp(alto)); padre.addView(fila, new LinearLayout.LayoutParams(-1, -2));
    }
    private TextView boton(String label, int fondo, int color, Runnable accion) {
        TextView t = texto(label, 15, color, true); t.setGravity(Gravity.CENTER); t.setBackground(fondo(fondo, 10));
        t.setFocusable(true); t.setOnClickListener(v -> accion.run()); return t;
    }
    private void cambiar(int paso) {
        if (modelo.guardando) return;
        int actual = Cantidad.leer(cantidad.getText().toString());
        if (actual < 0) { cantidad.setError("Ingresa un número con hasta dos decimales"); return; }
        valor = Math.max(0, Math.min(Cantidad.MAXIMO, actual + paso)); cantidad.setText(Cantidad.mostrar(valor));
    }
    private void crearCarrusel(LinearLayout padre) {
        carrusel = new HorizontalScrollView(this) {
            // El carrusel termina siempre en una imagen completa, sin inercia entre páginas.
            @Override public void fling(int velocidad) { }
        };
        carrusel.setHorizontalScrollBarEnabled(false); carrusel.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout fotos = new LinearLayout(this); carrusel.addView(fotos);
        for (int n = 1; n <= 2; n++) {
            ImageView foto = new ImageView(this); foto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int recurso = getResources().getIdentifier("clasificacion_" + categoria.imagen + n, "drawable", getPackageName());
            if (recurso != 0) foto.setImageResource(recurso);
            foto.setContentDescription(categoria.nombre + ", imagen " + n + " de 2");
            foto.setBackground(fondo(Color.parseColor("#DDE6DC"), 12)); foto.setClipToOutline(true);
            fotos.addView(foto, new LinearLayout.LayoutParams(dp(320), -1));
        }
        carrusel.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int ancho = r - l;
            if (ancho > 0 && ancho != or - ol) {
                for (int i = 0; i < fotos.getChildCount(); i++) { View foto = fotos.getChildAt(i); ViewGroup.LayoutParams p = foto.getLayoutParams(); p.width = ancho; foto.setLayoutParams(p); }
                carrusel.post(() -> { if (carrusel != null) carrusel.scrollTo(pagina * ancho, 0); });
            }
        });
        carrusel.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) handler.removeCallbacks(avanzar);
            if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                HorizontalScrollView vista = (HorizontalScrollView)v;
                pagina = Math.max(0, Math.min(1, Math.round((float)vista.getScrollX() / Math.max(1, vista.getWidth()))));
                vista.post(() -> vista.smoothScrollTo(pagina * vista.getWidth(), 0)); programarCarrusel();
                return false;
            }
            return false;
        });
        espacio(padre, 10); padre.addView(carrusel, new LinearLayout.LayoutParams(-1, dp(200)));
    }
    private void programarCarrusel() { handler.removeCallbacks(avanzar); if (activo && carrusel != null && confirmacion == null && !exito) handler.postDelayed(avanzar, 4500); }
    private void confirmar() {
        if (modelo.guardando || categoria == null || confirmacion != null) return;
        valor = Cantidad.leer(cantidad.getText().toString());
        if (valor <= 0) { cantidad.setError("Ingresa una cantidad mayor que 0 con hasta dos decimales"); return; }
        cantidad.clearFocus(); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(cantidad.getWindowToken(), 0);
        handler.removeCallbacks(avanzar);
        confirmacion = new Dialog(this); confirmacion.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout contenido = vertical(); contenido.setPadding(dp(30), dp(36), dp(20), dp(50)); contenido.setGravity(Gravity.CENTER); contenido.setBackground(fondo(GRIS, 14));
        TextView pregunta = texto("¿Estás seguro que los\ndatos son correctos?", 17, Color.BLACK, true); pregunta.setGravity(Gravity.CENTER); contenido.addView(pregunta);
        espacio(contenido, 18); TextView dato = texto("Cantidad: " + Cantidad.mostrar(valor) + " kg", 14, Color.parseColor("#687586"), true); contenido.addView(dato);
        espacio(contenido, 24); LinearLayout botones = new LinearLayout(this);
        TextView cancelar = boton("Cancelar", Color.parseColor("#E2E8F0"), Color.parseColor("#475569"), () -> confirmacion.dismiss());
        TextView aceptar = boton("Aceptar", VERDE, Color.WHITE, () -> {
            if (modelo.guardando) return;
            confirmarGuardado();
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(44), 1); bp.setMargins(0, 0, dp(28), 0); botones.addView(cancelar, bp); botones.addView(aceptar, new LinearLayout.LayoutParams(0, dp(44), 1)); contenido.addView(botones, new LinearLayout.LayoutParams(-1, -2));
        confirmacion.setContentView(contenido); confirmacion.setOnDismissListener(d -> { confirmacion = null; programarCarrusel(); });
        confirmacion.show(); Window window = confirmacion.getWindow(); if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent); window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(400)), -2); window.setDimAmount(.75f); }
    }
    private void confirmarGuardado() { guardar.setEnabled(false); confirmacion.dismiss(); modelo.guardar(categoria, valor, getIntent().getBooleanExtra("clasificacion_preview", false)); }
    private void mostrarExito() {
        exito = true; preparar(true);
        raiz.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.parseColor("#10B981"), Color.BLACK}));
        LinearLayout centro = vertical(); centro.setGravity(Gravity.CENTER); centro.setPadding(dp(25), dp(50), dp(25), dp(100));
        TextView check = texto("✓", 130, Color.parseColor("#A7CB73"), true); check.setGravity(Gravity.CENTER); centro.addView(check);
        espacio(centro, 35); TextView titulo = texto("REGISTRO GUARDADO\nCON ÉXITO", 26, Color.WHITE, true); titulo.setGravity(Gravity.CENTER); centro.addView(titulo);
        espacio(centro, 70); TextView aviso = texto("Redirigiendo automáticamente...", 18, Color.parseColor("#85AC9C"), true); aviso.setGravity(Gravity.CENTER); centro.addView(aviso);
        raiz.addView(centro, new LinearLayout.LayoutParams(-1, 0, 1)); if (activo) handler.postDelayed(volver, 2500);
    }
    private void aviso(String texto) { Toast.makeText(this, texto, Toast.LENGTH_LONG).show(); }
    @Override protected void onResume() { super.onResume(); activo = true; programarCarrusel(); if (exito) handler.postDelayed(volver, 2500); }
    @Override protected void onPause() { activo = false; handler.removeCallbacks(avanzar); handler.removeCallbacks(volver); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); if (categoria != null) out.putString("categoria", categoria.name());
        if (cantidad != null && categoria != null && !exito) { int actual = Cantidad.leer(cantidad.getText().toString()); if (actual >= 0) valor = actual; }
        out.putInt("valor", valor); out.putInt("pagina", pagina); out.putString("busqueda", busqueda); out.putBoolean("exito", exito); out.putBoolean("confirmacion", confirmacion != null);
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); if (confirmacion != null) confirmacion.dismiss(); super.onDestroy(); }
}