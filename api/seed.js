/**
 * Script de una sola ejecución: crea el primer usuario admin directo en MongoDB.
 * Uso: npm run seed
 *
 * Después de correr esto, ya se puede hacer login con ese admin y usar
 * POST /empleados/crear para dar de alta al resto de trabajadores.
 */
require('dotenv').config();
const bcrypt = require('bcryptjs');
const mongoose = require('mongoose');
const Empleado = require('./models/Empleado');

async function seed() {
  try {
    await mongoose.connect(process.env.MONGO_URI);
    console.log('Conectado a MongoDB');

    const codigoAdmin = process.env.ADMIN_CODIGO || 'ADM001';

    const existente = await Empleado.findOne({ codigo: codigoAdmin });
    if (existente) {
      console.log(`Ya existe un empleado con código ${codigoAdmin}. No se creó nada.`);
      process.exit(0);
    }

    const passwordHasheado = await bcrypt.hash(
      process.env.ADMIN_PASSWORD || 'CambiarPassword123!',
      10
    );

    const admin = await Empleado.create({
      nombre: process.env.ADMIN_NOMBRE || 'Administrador General',
      codigo: codigoAdmin,
      password: passwordHasheado,
      cargo: process.env.ADMIN_CARGO || 'Administrador',
      turno: process.env.ADMIN_TURNO || 'Mañana',
      rol: 'admin',
    });

    console.log('Usuario admin creado correctamente:');
    console.log({ id: admin._id, codigo: admin.codigo, rol: admin.rol });
    process.exit(0);
  } catch (error) {
    console.error('Error al crear el admin:', error.message);
    process.exit(1);
  }
}

seed();
