const mongoose = require('mongoose');

const empleadoSchema = new mongoose.Schema(
  {
    nombre: { type: String, required: true, trim: true },
    codigo: { type: String, required: true, unique: true, trim: true },
    password: { type: String, required: true }, // se guarda ya encriptada con bcrypt
    cargo: { type: String, required: true, trim: true },
    turno: { type: String, required: true, trim: true },
    rol: {
      type: String,
      required: true,
      enum: ['admin', 'operario'],
      default: 'operario',
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Empleado', empleadoSchema);
