package com.example.appecolim.clasificacion;

/** Contenido transcrito de las siete pantallas de ECOREG en Figma. */
public enum Categoria {
    COMIDA("Restos de comida", "RESTOS DE COMIDA", "Orgánicos, alimentos y cáscaras.", "Restos de alimentos, cáscaras de frutas y verduras, posos de café, cáscaras de huevo, entre otros orgánicos.", "Sin bolsas plásticas, sin líquidos en exceso.", "🍎", "food"),
    PAPEL("Papel/Cartón", "PAPEL / CARTÓN", "Cajas, hojas de papel, periódicos y envases de cartón.", "Cajas, hojas de papel, periódicos, revistas, sobres y envases de cartón.", "Limpio, seco y sin residuos de comida o líquidos.", "📦", "paper"),
    PILAS("Pilas y Baterías", "PILAS Y BATERÍAS", "Pilas, baterías y acumuladores.", "Pilas, baterías, acumuladores y baterías recargables.", "Secas, sin fugas y, de ser posible, en su empaque o aisladas con cinta.", "🔋", "battery"),
    JARDIN("Residuos de jardín", "RESIDUOS DE JARDÍN", "Hojas secas, ramas, césped y podas.", "Hojas, ramas, césped, flores marchitas y otros residuos de jardín.", "Sin tierra en exceso, sin bolsas plásticas ni residuos de otros materiales.", "🍃", "garden"),
    METALES("Metales", "METALES", "Latas, envases metálicos y chatarra.", "Latas, envases metálicos, tapas, ollas, sartenes y chatarra en general.", "Limpio, sin residuos de comida o líquidos.", "🥫", "metal"),
    PUNZOCORTANTE("Material Punzocortante", "MATERIAL PUNZOCORTANTE", "Agujas, jeringas, vidrios y objetos cortantes.", "Agujas, jeringas, bisturíes, vidrios rotos y objetos cortantes o punzantes.", "En un recipiente rígido, resistente a perforaciones y bien cerrado.", "✂", "sharp"),
    PLASTICOS("Plásticos", "PLÁSTICOS", "Botellas, envases, bolsas y productos plásticos.", "Botellas, envases, bolsas, empaques y productos plásticos.", "Limpio, seco y sin residuos de comida o líquidos.", "🧴", "plastic");

    public final String nombre, titulo, resumen, incluye, condicion, icono, imagen;
    Categoria(String nombre, String titulo, String resumen, String incluye, String condicion, String icono, String imagen) {
        this.nombre = nombre; this.titulo = titulo; this.resumen = resumen;
        this.incluye = incluye; this.condicion = condicion; this.icono = icono; this.imagen = imagen;
    }
}