const jwt = require('jsonwebtoken');

// Verifica que el request traiga un token válido en el header Authorization: Bearer <token>
function verificarToken(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ mensaje: 'Token no proporcionado' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET);
    req.empleado = payload; // { id_empleado, codigo, rol }
    next();
  } catch (error) {
    return res.status(401).json({ mensaje: 'Token inválido o expirado' });
  }
}

// Debe usarse DESPUÉS de verificarToken. Bloquea la ruta si el rol no es admin.
function soloAdmin(req, res, next) {
  if (req.empleado?.rol !== 'admin') {
    return res.status(403).json({ mensaje: 'Acceso restringido a administradores' });
  }
  next();
}

module.exports = { verificarToken, soloAdmin };
