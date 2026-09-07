const mongoose = require('mongoose');

const residuoSchema = new mongoose.Schema(
  {
    id_local: { type: String, required: true }, // id generado en SQLite (frontend), para trazabilidad offline
    id_empleado: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Empleado',
      required: true,
    },
    tipo: { type: String, required: true, trim: true },
    cantidad_kg: { type: Number, required: true, min: 0 },
    fecha_hora: { type: Date, required: true },
    estado_sync: {
      type: String,
      enum: ['pendiente', 'sincronizado'],
      default: 'sincronizado', // si llegó al servidor, ya está sincronizado desde la perspectiva del API
    },
  },
  { timestamps: true }
);

// Acelera las búsquedas por empleado + fecha, usadas en /residuos/hoy, /historial y /reporte
residuoSchema.index({ id_empleado: 1, fecha_hora: -1 });

module.exports = mongoose.model('Residuo', residuoSchema);
