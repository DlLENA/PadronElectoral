# API REST — Sistema de Registro y Consulta de Padrón Electoral

Proyecto académico de **Base de Datos I** de la Universidad Mariano Gálvez de Guatemala, Campus Jutiapa. La aplicación integra una API REST desarrollada con Spring Boot, una interfaz web para el registro y la consulta de empadronamientos, y SQL Server como sistema de persistencia.

> **Estado del proyecto:** la interfaz web está integrada en la aplicación Spring Boot y se sirve desde el mismo origen que la API. El backend utiliza el puerto **8081** en el entorno configurado.

## Contenido

- [Objetivo y alcance](#objetivo-y-alcance)
- [Tecnologías](#tecnologías)
- [Arquitectura general](#arquitectura-general)
- [Requisitos previos](#requisitos-previos)
- [Preparación de SQL Server](#preparación-de-sql-server)
- [Fase 5: Seguridad de red y firewall](#fase-5-seguridad-de-red-y-firewall)
- [Configuración de la aplicación](#configuración-de-la-aplicación)
- [Ejecutar el proyecto](#ejecutar-el-proyecto)
- [Endpoints principales](#endpoints-principales)
- [Documentación OpenAPI y Swagger](#documentación-openapi-y-swagger)
- [Seguridad actual y mejoras previstas](#seguridad-actual-y-mejoras-previstas)
- [Frontend web](#frontend-web)
- [Pruebas y verificación](#pruebas-y-verificación)
- [Trabajo en equipo y Git](#trabajo-en-equipo-y-git)
- [Consideraciones de seguridad y despliegue](#consideraciones-de-seguridad-y-despliegue)

## Objetivo y alcance

El sistema contempla dos módulos funcionales principales:

1. **Registro de empadronamiento:** recibe los datos del ciudadano y delega las reglas de negocio a los procedimientos almacenados de SQL Server.
2. **Consulta de empadronamiento:** permite buscar por número de empadronamiento o por CUI/DPI, con los datos de verificación definidos por el contrato de la API.

También proporciona endpoints para obtener un CAPTCHA y consultar catálogos utilizados por los formularios.

El proyecto es académico y utiliza datos ficticios para pruebas. No está integrado con sistemas oficiales de identificación ni debe tratarse como un sistema de producción.

## Tecnologías

- **Java 21**
- **Spring Boot 4.1.1**
- **Maven** para gestionar dependencias y construir el proyecto
- **Spring MVC** para la API REST
- **Jakarta Validation** para validaciones de solicitudes
- **JDBC / `JdbcTemplate`** para invocar procedimientos almacenados
- **Microsoft SQL Server** como motor de base de datos
- **Microsoft JDBC Driver for SQL Server**
- **OpenAPI Generator** para generar modelos e interfaces a partir del contrato OpenAPI
- **Springdoc / Swagger UI** para consultar y probar los endpoints
- **Eclipse IDE** como entorno de desarrollo utilizado

La aplicación **no crea las tablas de la base de datos automáticamente**. La creación y preparación de la base se realiza en SQL Server mediante los scripts SQL correspondientes.

## Arquitectura general

Flujo simplificado:

```text
Navegador web
    |
    | HTTP + JSON (mismo origen)
    v
Frontend integrado (src/main/resources/static)
    |  registro.html · consulta.html · app.js
    |
    | solicitudes a /api/...
    v
API REST — Spring Boot
          |
          +-- Controllers
          +-- Services
          +-- PadronRepository (JDBC)
                    |
                    | procedimientos almacenados
                    v
             SQL Server
             Base PadronElectoral
             Esquema Temporal
```

Responsabilidades principales:

- **Frontend web:** presenta los formularios de registro y consulta, carga catálogos, solicita CAPTCHA, envía solicitudes JSON y presenta mensajes y resultados.
- **Controllers:** reciben solicitudes HTTP y devuelven respuestas REST.
- **Services:** coordinan las operaciones y reglas propias de la aplicación.
- **Repository:** accede a SQL Server mediante JDBC y ejecuta procedimientos almacenados.
- **OpenAPI:** define las rutas, los modelos de solicitud y respuesta y los códigos HTTP.
- **SQL Server:** conserva los datos y aplica las reglas de negocio definidas en tablas, restricciones y procedimientos almacenados.

## Requisitos previos

Antes de ejecutar el backend, se necesita:

- Java Development Kit (JDK) **21**.
- Maven instalado y disponible en el sistema (`mvn -version`).
- SQL Server accesible desde el equipo donde se ejecuta la API.
- La base de datos `PadronElectoral` preparada con el esquema `Temporal`, sus tablas, catálogos y procedimientos almacenados.
- Credenciales válidas para la cuenta de aplicación de SQL Server.
- Acceso de red permitido entre el equipo de la API y el servidor de base de datos.

El proyecto no incluye Maven Wrapper; se espera que cada desarrollador tenga Maven instalado.

## Preparación de SQL Server

1. Configurar SQL Server en el servidor o máquina virtual asignada al proyecto.
2. Ejecutar el script SQL del proyecto **Versión 3** (`Version_3_-_Script_Padron_Electoral.txt`) siguiendo sus instrucciones y revisando antes las secciones que crean o modifican objetos y datos de prueba.
3. Confirmar que existen la base `PadronElectoral`, el esquema `Temporal` y los procedimientos almacenados requeridos por el backend.
4. Crear o configurar la cuenta de servicio de la aplicación con **privilegios mínimos**. La cuenta prevista, `app_padron`, debe tener permiso para ejecutar únicamente los procedimientos necesarios, no acceso general a las tablas.
5. Probar la conectividad desde el equipo donde correrá la API.

**Script de base de datos utilizado:** se trabajó con la **Versión 3 del Script del Padrón Electoral para SQL Server**. Esta versión conserva el historial de cambios de la V2 e incorpora correcciones identificadas como V3. Antes de ejecutarlo, revisa especialmente la variable `@EliminarBaseExistente`, cuyo valor predeterminado es `0` para evitar borrar accidentalmente una base existente. No cambies ese valor a `1` sin respaldo y autorización para recrear la base.

**Importante:** revisa y cambia cualquier contraseña de ejemplo incluida en scripts SQL antes de ejecutar el script en un entorno compartido. No publiques contraseñas reales en GitHub. Los datos de prueba deben ser ficticios.

La dirección configurada actualmente para SQL Server es `172.16.0.100`, puerto `1433`. Debe ajustarse si la dirección, instancia o puerto reales son distintos.

## Configuración de la aplicación

La configuración base se encuentra en:

`src/main/resources/application.properties`

Actualmente contiene, entre otras, estas propiedades:

- Nombre de aplicación: `empadronamiento-api`
- Puerto HTTP de la API: `8081`
- Base de datos: `PadronElectoral`
- Usuario de base de datos: `app_padron`
- Credenciales: se configuran en `application.properties`; se recomienda externalizar la contraseña mediante una variable de entorno (`DB_PASSWORD`) y no guardar secretos en Git

Para evitar exponer credenciales, configura la contraseña mediante una variable de entorno y evita guardar valores reales en `application.properties` o en el repositorio. Si se adopta `DB_PASSWORD`, la propiedad debe referenciarla así:

```properties
spring.datasource.password=${DB_PASSWORD}
```

Define `DB_PASSWORD` en el entorno de ejecución. No incluyas la contraseña real en este README ni la subas a Git.

### Configurar `DB_PASSWORD` en Windows

En una terminal de Windows, para establecerla solo durante esa sesión:

```bat
set DB_PASSWORD=TU_CONTRASENA_REAL
```

Después, ejecuta la aplicación desde esa misma terminal. En Eclipse, también puedes definir la variable en la configuración de ejecución (**Run Configurations → Environment**) para la aplicación.

No escribas la contraseña en este README ni la envíes por commits, capturas públicas o mensajes de error.

### Nota sobre cifrado de SQL Server

La URL actual utiliza `encrypt=true;trustServerCertificate=true`. Aunque la conexión se cifra, `trustServerCertificate=true` omite la validación normal de confianza del certificado del servidor. Es una configuración que puede servir en un entorno académico controlado, pero **no es la configuración recomendada para producción**. Para un despliegue seguro, instala y configura un certificado válido y verifica la cadena de confianza; después, desactiva la confianza automática en el certificado.

## Ejecutar el proyecto

Desde la carpeta raíz del repositorio:

### Compilar

```bash
mvn compile
```

### Ejecutar las pruebas y verificaciones de Maven

```bash
mvn verify
```

### Iniciar la aplicación

```bash
mvn spring-boot:run
```

También puede ejecutarse desde Eclipse como **Run As → Spring Boot App** o **Run As → Java Application**, según las opciones que ofrezca la instalación.

La aplicación escucha actualmente en el puerto `8081`. La aplicación necesita conectividad con SQL Server para completar las operaciones que acceden a la base de datos.

## Endpoints principales

Las rutas se definen en `src/main/resources/openapi/openapi.yaml`. Las rutas principales previstas por el contrato son:

| Método | Ruta | Propósito |
|---|---|---|
| `POST` | `/api/empadronamientos` | Registrar un nuevo empadronamiento |
| `POST` | `/api/consultas` | Consultar un empadronamiento |
| `GET` | `/api/captcha` | Obtener un CAPTCHA |
| `GET` | `/api/catalogos/municipios` | Listar municipios |
| `GET` | `/api/catalogos/generos` | Listar géneros |
| `GET` | `/api/catalogos/afiliaciones` | Listar afiliaciones |
| `GET` | `/api/catalogos/estados` | Listar estados |
| `GET` | `/api/catalogos/centros?municipioId={id}` | Listar centros de votación de un municipio |
| `GET` | `/api/catalogos/mesas?centroId={id}` | Listar mesas de un centro de votación |

Los códigos de respuesta y los modelos concretos deben consultarse en el contrato OpenAPI. No se deben asumir detalles distintos a los que allí se definan.

La consulta se ha diseñado como `POST` para enviar el CUI/DPI y la fecha de nacimiento en el cuerpo JSON en lugar de incluirlos en la URL. Esto reduce su exposición accidental en historiales y registros de URL, pero **no sustituye HTTPS, la protección de logs ni los controles de acceso**.

## Documentación OpenAPI y Swagger

El contrato de la API se encuentra en:

`src/main/resources/openapi/openapi.yaml`

El plugin de OpenAPI Generator genera modelos e interfaces Java durante el ciclo de construcción de Maven. Los archivos generados se ubican en `target/generated-sources/openapi`; son archivos derivados y no deben editarse manualmente. Para cambiar el contrato, modifica el archivo `openapi.yaml` y vuelve a generar/compilar.

Con la aplicación en ejecución, Swagger UI está configurado para abrirse en:

- `http://localhost:8081/swagger-ui.html`

El frontend y Swagger UI se acceden en el mismo origen local. Si se cambia el puerto en la configuración, deben actualizarse las direcciones utilizadas.

## Seguridad actual y mejoras previstas

La aplicación incluye validaciones y un CAPTCHA en los flujos que corresponden según el contrato y el código del proyecto. Para un despliegue fuera del entorno académico, deben revisarse y configurarse las medidas de seguridad de infraestructura y aplicación que correspondan. Las recomendaciones siguientes describen buenas prácticas de despliegue; no implican que cada mecanismo esté habilitado en el entorno actual.

### Medidas de base de datos y red

- Mantener el firewall del servidor habilitado.
- Abrir solo los puertos necesarios y limitar las reglas por dirección IP de origen cuando sea posible.
- Permitir el puerto de SQL Server (`1433`, si ese es el puerto configurado) únicamente desde los equipos que deban conectarse; **no exponer SQL Server directamente a Internet**.
- Si la API y SQL Server están en equipos distintos, permitir la conexión solo entre el host de la API y el servidor de base de datos.
- No abrir el puerto de administración remota a toda la red.
- Usar una cuenta de base de datos dedicada con permisos mínimos y contraseñas robustas.
- Mantener copias de seguridad y restringir quién puede acceder a ellas.

### Fase 5: Seguridad de red y configuración de firewall (reglas de entrada)

De acuerdo con los lineamientos del proyecto (sección 6.1) y las buenas prácticas de administración de servidores, el sistema operativo **Windows Server 2022 mantuvo activo el Firewall de Windows Defender**, garantizando el aislamiento del entorno.

Para permitir la conectividad entre la aplicación externa —backend desarrollado con Spring Boot— y el motor de base de datos, se configuró una regla específica de tráfico entrante:

- **Regla de conexión a SQL Server (motor de base de datos):** se creó una regla de entrada (*Inbound Rule*) que permite explícitamente el tráfico a través del **puerto TCP 1433**, puerto predeterminado utilizado por Microsoft SQL Server para recibir conexiones en la configuración del proyecto.
- **Justificación técnica:** sin la apertura de este puerto específico, el firewall bloquearía los intentos de conexión desde el backend hacia SQL Server, provocando fallos en las operaciones de validación, consulta e inserción del sistema de registro ciudadano. Estos problemas podrían manifestarse como tiempos de espera (*timeout*) o errores de la aplicación.
- **Principio de mínimo acceso:** se habilitó únicamente el puerto requerido para la comunicación con SQL Server, evitando exponer innecesariamente otros servicios del servidor a la red interna.

La regla debe limitarse, cuando sea posible, a las direcciones IP de los equipos autorizados para comunicarse con SQL Server. El puerto 1433 no debe exponerse directamente a Internet. Si cambia la dirección del servidor, el puerto configurado o la ubicación del backend, se debe revisar la regla del firewall y volver a comprobar la conectividad.

### Spring Security

La dependencia y las reglas de Spring Security deben incorporarse cuando se defina el modelo de acceso. Su configuración no debe darse por implementada solo porque la aplicación utiliza Spring Boot.

Antes de añadirla, el equipo debe decidir:

1. Si habrá autenticación para los usuarios de la interfaz, para administradores o para ambos.
2. Qué endpoints serán públicos y cuáles requerirán autenticación.
3. Si se usarán sesiones, tokens (por ejemplo, JWT) u otro mecanismo apropiado para el cliente elegido.
4. Cómo se gestionarán roles, permisos, expiración, revocación y almacenamiento de credenciales.
5. Cómo se limitarán intentos repetidos, automatización abusiva y solicitudes excesivas.

El CAPTCHA puede ser una medida adicional contra consultas automatizadas, pero no reemplaza la autenticación, la autorización ni la limitación de solicitudes. No se debe habilitar una política permisiva de CORS para cualquier origen sin una necesidad concreta.

### HTTPS

En desarrollo local puede usarse HTTP para facilitar las pruebas. En una red compartida o en un despliegue accesible por otros equipos, la API debería publicarse mediante HTTPS.

Alternativas para el despliegue futuro:

- **Proxy inverso (opción habitual):** terminar TLS en un proxy inverso configurado con un certificado válido y reenviar las solicitudes al backend en una red privada.
- **TLS directamente en Spring Boot:** configurar un certificado y una clave para que el servidor de la aplicación atienda HTTPS directamente.

En ambos casos, deben protegerse las claves privadas, renovarse los certificados y evitar enviar credenciales o datos personales por HTTP sin cifrado. Si se utiliza un proxy, debe configurarse correctamente el manejo de cabeceras reenviadas y evitar confiar en cabeceras que puedan falsificarse desde clientes externos.

### Otras medidas recomendadas

- Validar toda entrada en la API y mantener las validaciones de negocio en la base de datos.
- No devolver trazas internas, consultas SQL, contraseñas ni información sensible en errores.
- Evitar registrar en logs CUI/DPI, fechas de nacimiento, contraseñas, tokens o cuerpos completos de solicitudes que contengan datos personales.
- Limitar la exposición de Swagger UI en entornos que no sean de desarrollo.
- Actualizar dependencias de forma controlada y ejecutar pruebas después de cada actualización.
- Usar exclusivamente datos ficticios en las pruebas y demostraciones.
- Definir una política de respaldo, acceso, retención y eliminación de datos para cualquier entorno que llegue a contener datos reales.

## Frontend web

El frontend está integrado en el proyecto Spring Boot dentro de `src/main/resources/static/`. Spring Boot sirve estos archivos directamente, por lo que las páginas pueden consumir la API mediante rutas relativas (`/api/...`) sin configurar un servidor frontend independiente para el uso local.

| Archivo | Responsabilidad |
|---|---|
| `src/main/resources/static/registro.html` | Formulario de registro de empadronamiento, datos personales y de identificación, selección de catálogos, CAPTCHA y envío de la solicitud. |
| `src/main/resources/static/consulta.html` | Formulario de consulta por tipo de identificador, número, fecha de nacimiento y CAPTCHA; muestra los datos devueltos por la API. |
| `src/main/resources/static/app.js` | Carga catálogos, solicita CAPTCHA, prepara y envía las solicitudes JSON de registro y consulta, y presenta mensajes y resultados. |

La interfaz utiliza **HTML, CSS, JavaScript y Bootstrap 5.3.2**. Los formularios se comunican con los endpoints REST del mismo servidor:

- Registro: `POST /api/empadronamientos`.
- Consulta: `POST /api/consultas`.
- CAPTCHA: `GET /api/captcha`.
- Catálogos: rutas bajo `/api/catalogos/`.

### Acceso local

Con la aplicación iniciada, abrir en el navegador:

- **Registro:** `http://localhost:8081/registro.html`
- **Consulta:** `http://localhost:8081/consulta.html`
- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

Se recomienda abrir estas direcciones desde Spring Boot y no abrir los HTML directamente mediante `file://`, ya que el frontend utiliza rutas relativas para comunicarse con la API.

## Pruebas y verificación

Antes de integrar cambios en la rama principal:

1. Ejecutar `mvn compile`.
2. Ejecutar `mvn verify` cuando las pruebas estén preparadas.
3. Probar el registro válido y los casos rechazados por las reglas de negocio.
4. Probar consultas por los tipos de identificador definidos en OpenAPI.
5. Verificar respuestas para solicitudes incorrectas, registros no encontrados y errores internos.
6. Confirmar que los endpoints de catálogos devuelvan los datos esperados.
7. Probar la conexión desde el equipo cliente y revisar las reglas del firewall.
8. Verificar que los logs no revelen datos personales ni secretos.

## Trabajo en equipo y Git

Se utilizan ramas para separar el desarrollo. El flujo sugerido es:

1. Trabajar en una rama de funcionalidad (por ejemplo, `feature/api-rest`).
2. Revisar los cambios y ejecutar las pruebas.
3. Crear un commit con un mensaje descriptivo.
4. Subir la rama a GitHub (`push`).
5. Cambiar a la rama principal (`main` o `master`) y fusionar la rama de funcionalidad (`merge`).
6. Probar la rama principal y subirla a GitHub.

No se deben subir contraseñas, certificados privados, archivos locales de configuración con secretos ni datos reales. La carpeta `target/` y los archivos generados durante la compilación están excluidos por `.gitignore`.

## Consideraciones de seguridad y despliegue

- La estrategia de autenticación y autorización debe corresponder al alcance y al entorno en el que se utilice el sistema.
- HTTPS debe configurarse para entornos compartidos o accesibles desde otros equipos; el acceso local mediante HTTP es para desarrollo.
- Las reglas de firewall dependen de dónde se desplieguen la API y SQL Server y de las direcciones autorizadas.
- La compilación correcta no sustituye las pruebas funcionales en el entorno conectado a SQL Server.
- No publicar contraseñas, datos personales ni otros secretos en el repositorio, capturas o documentación.