# EcoReg

Sistema móvil de registro y gestión de residuos para operarios, diseñado con un enfoque offline-first.

## Normas y Restricciones del Proyecto

* **Aislamiento de Código:** Queda estrictamente prohibido modificar código de forma directa sobre la rama principal (`master`/`main`). Todo desarrollo debe ejecutarse en ramas de trabajo independientes.
* **Control de Archivos Excluidos:** No se deben subir archivos pesados ni directorios generados automáticamente por el entorno (como `build/`, `.gradle/` o configuraciones locales), los cuales deben gestionarse mediante `.gitignore`.
* **Comunicación y Sincronización:** Es obligatorio notificar al equipo previa realización de fusiones (*merges*) y ejecutar el flujo de actualización de manera regular para mantener la estabilidad del repositorio.

## Guía de Comandos Git

### Gestión de Ramas
* **Crear y posicionarse en una nueva rama:**
  `git checkout -b nombre-de-rama`
* **Cambiar a una rama existente:**
  `git checkout nombre-de-rama`
* **Verificar la rama activa:**
  `git branch`

### Flujo de Actualización Obligatorio
Para incorporar los cambios recientes del equipo a la rama de trabajo local, ejecutar en la terminal:
1. Cambiar a la rama principal y obtener las actualizaciones:
   `git checkout master` y `git pull origin master`
2. Retornar a la rama de desarrollo individual:
   `git checkout nombre-de-rama`
3. Fusionar los cambios estables:
   `git merge master`

## Roles y Responsabilidades del Equipo

* **Frontend (Android / UI):** Desarrollo de interfaces gráficas, navegación, control de flujos de usuario y consumo de datos.
* **Bases de Datos (SQLite y MongoDB):** Administración del almacenamiento persistente offline mediante `DatabaseHelper` (SQLite) y gestión de la base de datos centralizada en la nube (MongoDB).
* **API y Backend:** Desarrollo de los servicios de conexión, lógica de servidor y puntos de acceso (*endpoints*).

## Estructura de Directorios y Distribución

* **app/src/main/java/com/example/ecolimapp/**
    * **DatabaseHelper.java:** Lógica de esquemas, consultas, persistencia SQLite y control de sincronización local.
    * **ui/ o views/:** Clases de actividad y fragmentos correspondientes a la interfaz gráfica (Frontend).
    * **network/ o api/:** Clases de conexión, clientes HTTP y servicios de comunicación con el backend.
    * **MainActivity.java:** Controlador de inicio y gestión general de navegación.
* **app/src/main/res/**
    * **layout/:** Diseños de interfaz en formato XML para el desarrollo visual.
    * **values/:** Recursos globales del sistema (colores, cadenas de texto, estilos y temas).
* **app/build.gradle.kts:** Archivo de configuración de dependencias, SDKs y compilación.
* **gradle/ y gradlew:** Herramientas ejecutables de automatización de compilación.