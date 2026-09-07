# API AppReciclaje — ECOLIM S.A.C.

API REST en Node.js + Express + MongoDB (Mongoose) para el registro digital de recolección de residuos sólidos.

## Instalación

```bash
npm install
cp .env.example .env
```

Edita `.env` con tu cadena de conexión de MongoDB y una clave JWT_SECRET propia.

## Crear el primer usuario admin

```bash
npm run seed
```

Esto crea un empleado con rol `admin` usando los datos de `.env` (ADMIN_CODIGO, ADMIN_PASSWORD, etc.). Solo se ejecuta una vez; si el código ya existe, no hace nada.

## Levantar el servidor

```bash
npm run dev   # con nodemon, recarga automática
npm start     # producción
```

## Rutas

| Método | Ruta | Protegida | Descripción |
|---|---|---|---|
| POST | `/login` | No | Login, devuelve token JWT |
| POST | `/empleados/crear` | Sí (JWT + admin) | Crear nuevo empleado |
| POST | `/residuos` | Sí (JWT) | Registrar un residuo recolectado |
| GET | `/residuos/hoy?id_empleado=` | Sí (JWT) | Residuos del día actual |
| GET | `/historial?id_empleado=&fecha=&tipo=` | Sí (JWT) | Historial filtrado (solo lectura) |
| GET | `/reporte/:mes/:anio?id_empleado=` | Sí (JWT) | Totales agregados por tipo y total del mes |
| PUT | `/residuos/:id_local` | Sí (JWT) | Corregir cantidad_kg de un registro |

Todas las rutas protegidas requieren el header:
```
Authorization: Bearer <token>
```

## Seguridad implementada

- Contraseñas encriptadas con **bcrypt** antes de guardarse
- Autenticación con **JWT**, expiración configurable (30 min por defecto)
- Validación de tipos de dato en body y query params (`middleware/validar.js`) — rechaza objetos donde se espera string/number, previniendo inyección NoSQL (ej. `{ "$ne": "" }` como password)
- Todas las consultas usan **Mongoose** (sin concatenar texto a MongoDB)
- No existe ruta DELETE para residuos — solo corrección vía PUT

## Ejemplo: crear un residuo

**Request** `POST /residuos`
```json
{
  "id_empleado": "64f1a2b3c4d5e6f7a8b9c0d1",
  "id_local": "local-001",
  "tipo": "plastico",
  "cantidad_kg": 12.5,
  "fecha_hora": "2026-08-30T14:30:00"
}
```

**Response**
```json
{
  "id_mongo": "64f1a2b3c4d5e6f7a8b9c0d2",
  "mensaje": "Registro guardado correctamente"
}
```
