require('dotenv').config();
const express = require('express');
const cors = require('cors');
const conectarDB = require('./config/db');

const authRoutes = require('./routes/authRoutes');
const empleadoRoutes = require('./routes/empleadoRoutes');
const residuoRoutes = require('./routes/residuoRoutes');

const app = express();

app.use(cors());
app.use(express.json());

conectarDB();

app.use('/', authRoutes); // POST /login
app.use('/empleados', empleadoRoutes); // POST /empleados/crear
app.use('/', residuoRoutes); // /residuos, /residuos/hoy, /historial, /reporte/:mes/:anio

app.get('/', (req, res) => {
  res.json({ mensaje: 'API AppReciclaje funcionando correctamente' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Servidor corriendo en http://localhost:${PORT}`);
});
