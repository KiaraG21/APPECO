const mongoose = require('mongoose');
const Residuo = require('../models/Residuo');

// Valida que un string sea un ObjectId válido de Mongo (evita queries con basura)
function esObjectIdValido(id) {
  return mongoose.Types.ObjectId.isValid(id);
}

// POST /residuos
async function crearResiduo(req, res) {
  try {
    const { id_empleado, id_local, tipo, cantidad_kg, fecha_hora } = req.body;

    if (!esObjectIdValido(id_empleado)) {
      return res.status(400).json({ mensaje: 'id_empleado inválido' });
    }

    const fecha = new Date(fecha_hora);
    if (isNaN(fecha.getTime())) {
      return res.status(400).json({ mensaje: 'fecha_hora inválida' });
    }

    const nuevoResiduo = await Residuo.create({
      id_empleado,
      id_local,
      tipo,
      cantidad_kg,
      fecha_hora: fecha,
    });

    return res.status(201).json({
      id_mongo: nuevoResiduo._id,
      mensaje: 'Registro guardado correctamente',
    });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

// GET /residuos/hoy?id_empleado=
async function residuosHoy(req, res) {
  try {
    const { id_empleado } = req.query;

    if (!id_empleado || !esObjectIdValido(id_empleado)) {
      return res.status(400).json({ mensaje: 'id_empleado inválido o no proporcionado' });
    }

    const inicioDia = new Date();
    inicioDia.setHours(0, 0, 0, 0);
    const finDia = new Date();
    finDia.setHours(23, 59, 59, 999);

    const residuos = await Residuo.find({
      id_empleado,
      fecha_hora: { $gte: inicioDia, $lte: finDia },
    }).sort({ fecha_hora: -1 });

    return res.status(200).json(residuos);
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

// GET /historial?id_empleado=&fecha=&tipo=
async function historial(req, res) {
  try {
    const { id_empleado, fecha, tipo } = req.query;

    if (!id_empleado || !esObjectIdValido(id_empleado)) {
      return res.status(400).json({ mensaje: 'id_empleado inválido o no proporcionado' });
    }

    const filtro = { id_empleado };

    if (fecha) {
      const dia = new Date(fecha);
      if (isNaN(dia.getTime())) {
        return res.status(400).json({ mensaje: 'fecha inválida' });
      }
      const inicioDia = new Date(dia.setHours(0, 0, 0, 0));
      const finDia = new Date(dia.setHours(23, 59, 59, 999));
      filtro.fecha_hora = { $gte: inicioDia, $lte: finDia };
    }

    if (tipo) {
      filtro.tipo = tipo;
    }

    const residuos = await Residuo.find(filtro).sort({ fecha_hora: -1 });
    return res.status(200).json(residuos);
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

// GET /reporte/:mes/:anio?id_empleado=
async function reporteMensual(req, res) {
  try {
    const { mes, anio } = req.params;
    const { id_empleado } = req.query;

    const mesNum = Number(mes);
    const anioNum = Number(anio);

    if (!Number.isInteger(mesNum) || mesNum < 1 || mesNum > 12) {
      return res.status(400).json({ mensaje: 'mes inválido (debe ser 1-12)' });
    }
    if (!Number.isInteger(anioNum) || anioNum < 2000) {
      return res.status(400).json({ mensaje: 'anio inválido' });
    }
    if (id_empleado && !esObjectIdValido(id_empleado)) {
      return res.status(400).json({ mensaje: 'id_empleado inválido' });
    }

    const inicioMes = new Date(anioNum, mesNum - 1, 1);
    const finMes = new Date(anioNum, mesNum, 0, 23, 59, 59, 999);

    const filtro = { fecha_hora: { $gte: inicioMes, $lte: finMes } };
    if (id_empleado) {
      filtro.id_empleado = new mongoose.Types.ObjectId(id_empleado);
    }

    const agregado = await Residuo.aggregate([
      { $match: filtro },
      {
        $group: {
          _id: '$tipo',
          total_kg: { $sum: '$cantidad_kg' },
        },
      },
      { $sort: { _id: 1 } },
    ]);

    const totalGeneral = agregado.reduce((suma, item) => suma + item.total_kg, 0);

    return res.status(200).json({
      mes: mesNum,
      anio: anioNum,
      por_tipo: agregado.map((item) => ({ tipo: item._id, total_kg: item.total_kg })),
      total_kg: totalGeneral,
    });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

// Los 7 valores válidos de "tipo", según la especificación acordada con el equipo
const TIPOS_VALIDOS = [
  'papel_carton',
  'plastico',
  'metal',
  'restos_de_comida',
  'residuos_de_jardin',
  'pilas_baterias',
  'material_punzocortante',
];

// PUT /residuos/:id_local
async function actualizarResiduo(req, res) {
  try {
    const { id_local } = req.params;
    const { cantidad_kg, tipo } = req.body;

    if (cantidad_kg === undefined && tipo === undefined) {
      return res.status(400).json({
        mensaje: 'Debes enviar al menos cantidad_kg o tipo para actualizar',
      });
    }

    const camposActualizar = {};

    if (cantidad_kg !== undefined) {
      if (typeof cantidad_kg !== 'number') {
        return res.status(400).json({ mensaje: "El campo 'cantidad_kg' debe ser de tipo number" });
      }
      camposActualizar.cantidad_kg = cantidad_kg;
    }

    if (tipo !== undefined) {
      if (typeof tipo !== 'string') {
        return res.status(400).json({ mensaje: "El campo 'tipo' debe ser de tipo string" });
      }
      if (!TIPOS_VALIDOS.includes(tipo)) {
        return res.status(400).json({
          mensaje: `El campo 'tipo' debe ser uno de: ${TIPOS_VALIDOS.join(', ')}`,
        });
      }
      camposActualizar.tipo = tipo;
    }

    const residuo = await Residuo.findOneAndUpdate(
      { id_local },
      camposActualizar,
      { new: true }
    );

    if (!residuo) {
      return res.status(404).json({ mensaje: 'Registro no encontrado' });
    }

    return res.status(200).json({
      mensaje: 'Registro actualizado correctamente',
      residuo,
    });
  } catch (error) {
    return res.status(500).json({ mensaje: 'Error en el servidor', error: error.message });
  }
}

module.exports = {
  crearResiduo,
  residuosHoy,
  historial,
  reporteMensual,
  actualizarResiduo,
};
