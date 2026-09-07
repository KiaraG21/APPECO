const express = require('express');
const router = express.Router();
const { crearEmpleado, cambiarPassword } = require('../controllers/empleadoController');
const { verificarToken, soloAdmin } = require('../middleware/auth');
const { validarBody } = require('../middleware/validar');

// POST /empleados/crear (solo admin, requiere JWT)
router.post(
  '/crear',
  verificarToken,
  soloAdmin,
  validarBody({
    nombre: 'string',
    codigo: 'string',
    cargo: 'string',
    turno: 'string',
    password: 'string',
    rol: 'string',
  }),
  crearEmpleado
);

// PUT /empleados/cambiar-password (requiere JWT, cualquier rol puede cambiar la suya)
router.put(
  '/cambiar-password',
  verificarToken,
  validarBody({ password_actual: 'string', password_nuevo: 'string' }),
  cambiarPassword
);

module.exports = router;