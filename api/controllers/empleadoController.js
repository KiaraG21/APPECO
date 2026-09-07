const bcrypt = require('bcryptjs');
const Empleado = require('../models/Empleado');

// POST /empleados/crear (solo admin, requiere JWT)
async function crearEmpleado(req, res) {
  try {
    const { nombre, codigo, cargo, turno, password, rol } = req.body;

    if (password.length < 8) {
      return res.status(400).json({
        mensaje: "La contraseña debe tener al menos 8 caracteres",
      });
    }

    if (codigo.length < 4) {
      return res.status(400).json({
        mensaje: "El código debe tener al menos 4 caracteres",
      });
    }

    if (rol !== 'admin' && rol !== 'operario') {
      return res.status(400).json({ mensaje: "El campo 'rol' debe ser 'admin' u 'operario'" });
    }

    const yaExiste = await Empleado.findOne({ codigo });
    if (yaExiste) {
      return res.status(409).json({ mensaje: 'Ya existe un empleado con ese código' });
    }

    const passwordHasheado = await bcrypt.hash(password, 10);

    const nuevoEmpleado = await Empleado.create({
      nombre,
      codigo,
      cargo,
      turno,
      rol,
      password: passwordHasheado,
    });

    return res.status(201).json({
      mensaje: 'Empleado creado correctamente',
      empleado: {
        id_empleado: nuevoEmpleado._id,
        nombre: nuevoEmpleado.nombre,
        codigo: nuevoEmpleado.codigo,
        cargo: nuevoEmpleado.cargo,
        turno: nuevoEmpleado.turno,
        rol: nuevoEmpleado.rol,
      },
    });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

// PUT /empleados/cambiar-password (requiere JWT, cada quien cambia SU PROPIA contraseña)
async function cambiarPassword(req, res) {
  try {
    const { password_actual, password_nuevo } = req.body;
    const id_empleado = req.empleado.id_empleado; // viene del token JWT, no del body

    const empleado = await Empleado.findById(id_empleado);
    if (!empleado) {
      return res.status(404).json({ mensaje: 'Empleado no encontrado' });
    }

    const passwordValido = await bcrypt.compare(password_actual, empleado.password);
    if (!passwordValido) {
      return res.status(401).json({ mensaje: 'La contraseña actual no es correcta' });
    }

    empleado.password = await bcrypt.hash(password_nuevo, 10);
    await empleado.save();

    return res.status(200).json({ mensaje: 'Contraseña actualizada correctamente' });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

module.exports = { crearEmpleado, cambiarPassword };
