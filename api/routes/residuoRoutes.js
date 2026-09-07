const express = require('express');
const router = express.Router();

const {
  crearResiduo,
  residuosHoy,
  historial,
  reporteMensual,
  actualizarResiduo,
} = require('../controllers/residuoController');
const { verificarToken } = require('../middleware/auth');
const { validarBody, validarQueryStrings } = require('../middleware/validar');

// Todas las rutas de este archivo requieren estar autenticado
router.use(verificarToken);

// POST /residuos
router.post(
  '/residuos',
  validarBody({
    id_empleado: 'string',
    id_local: 'string',
    tipo: 'string',
    cantidad_kg: 'number',
    fecha_hora: 'string',
  }),
  crearResiduo
);

// GET /residuos/hoy?id_empleado=
router.get('/residuos/hoy', validarQueryStrings(['id_empleado']), residuosHoy);

// PUT /residuos/:id_local
router.put('/residuos/:id_local', actualizarResiduo);

// GET /historial?id_empleado=&fecha=&tipo=
router.get(
  '/historial',
  validarQueryStrings(['id_empleado', 'fecha', 'tipo']),
  historial
);

// GET /reporte/:mes/:anio?id_empleado=
router.get(
  '/reporte/:mes/:anio',
  validarQueryStrings(['id_empleado']),
  reporteMensual
);

module.exports = router;
