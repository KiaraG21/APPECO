const express = require('express');
const router = express.Router();

const { login } = require('../controllers/authController');
const { validarBody } = require('../middleware/validar');

// POST /login
router.post('/login', validarBody({ codigo: 'string', password: 'string' }), login);

module.exports = router;
