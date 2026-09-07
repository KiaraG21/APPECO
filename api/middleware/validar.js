/**
 * Valida que los campos indicados existan en req.body y sean del tipo esperado
 * ('string' o 'number'). Esto es lo que previene la inyección NoSQL: si alguien
 * manda un objeto como password (ej. { "$ne": "" }) en vez de un string,
 * la petición se rechaza aquí antes de tocar la base de datos.
 *
 * Uso: validarBody({ codigo: 'string', password: 'string' })
 */
function validarBody(esquema) {
  return (req, res, next) => {
    const errores = [];

    for (const [campo, tipoEsperado] of Object.entries(esquema)) {
      const valor = req.body[campo];

      if (valor === undefined || valor === null) {
        errores.push(`El campo '${campo}' es requerido`);
        continue;
      }

      if (typeof valor !== tipoEsperado) {
        errores.push(`El campo '${campo}' debe ser de tipo ${tipoEsperado}`);
      }
    }

    if (errores.length > 0) {
      return res.status(400).json({ mensaje: 'Datos inválidos', errores });
    }

    next();
  };
}

/**
 * Valida que los query params indicados, si están presentes, sean strings
 * "planos" (no arrays ni objetos). Express permite ?campo[$gt]=1 que Mongo
 * podría interpretar como operador si no se filtra antes de usarlo.
 */
function validarQueryStrings(campos) {
  return (req, res, next) => {
    for (const campo of campos) {
      const valor = req.query[campo];
      if (valor !== undefined && typeof valor !== 'string') {
        return res.status(400).json({ mensaje: `El parámetro '${campo}' es inválido` });
      }
    }
    next();
  };
}

module.exports = { validarBody, validarQueryStrings };
