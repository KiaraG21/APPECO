const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const Empleado = require('../models/Empleado');

// POST /login
async function login(req, res) {
  try {
    const { codigo, password } = req.body;

    const empleado = await Empleado.findOne({ codigo });
    if (!empleado) {
      // Mensaje genérico a propósito: no revelar si el código existe o no
      return res.status(401).json({ mensaje: 'Credenciales inválidas' });
    }

    const passwordValido = await bcrypt.compare(password, empleado.password);
    if (!passwordValido) {
      return res.status(401).json({ mensaje: 'Credenciales inválidas' });
    }

    const token = jwt.sign(
      {
        id_empleado: empleado._id,
        codigo: empleado.codigo,
        rol: empleado.rol,
      },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '30m' }
    );

    return res.status(200).json({
      token,
      empleado: {
        id_empleado: empleado._id,
        nombre: empleado.nombre,
        cargo: empleado.cargo,
        turno: empleado.turno,
        codigo: empleado.codigo,
        rol: empleado.rol,
      },
    });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

module.exports = { login };
