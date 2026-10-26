r"""# API REST — Sistema de Registro y Consulta de Padrón Electoral

Proyecto académico de **Base de Datos I** de la Universidad Mariano Gálvez de Guatemala, Campus Jutiapa. El backend expone una API REST para registrar empadronamientos, consultar registros y proporcionar catálogos a una futura interfaz de usuario.

> **Estado del proyecto:** backend en desarrollo. La API está construida con Spring Boot y SQL Server. La interfaz web y las medidas adicionales de seguridad descritas como futuras mejoras no deben considerarse implementadas hasta que se desarrollen y prueben.

## Contenido

- [Objetivo y alcance](#objetivo-y-alcance)
- [Tecnologías](#tecnologías)
- [Arquitectura general](#arquitectura-general)
- [Requisitos previos](#requisitos-previos)
- [Preparación de SQL Server](#preparación-de-sql-server)
- [Configuración de la aplicación](#configuración-de-la-aplicación)
- [Ejecutar el proyecto](#ejecutar-el-proyecto)
- [Endpoints principales](#endpoints-principales)
- [Documentación OpenAPI y Swagger](#documentación-openapi-y-swagger)
- [Seguridad actual y mejoras previstas](#seguridad-actual-y-mejoras-previstas)
- [Interfaz de usuario futura](#interfaz-de-usuario-futura)
- [Pruebas y verificación](#pruebas-y-verificación)
- [Trabajo en equipo y Git](#trabajo-en-equipo-y-git)
- [Limitaciones conocidas](#limitaciones-conocidas)

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
Interfaz de usuario (futura)
          |
          | HTTP/HTTPS + JSON
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
2. Ejecutar el script SQL del proyecto siguiendo sus instrucciones y revisando antes las secciones que crean o modifican objetos y datos de prueba.
3. Confirmar que existen la base `PadronElectoral`, el esquema `Temporal` y los procedimientos almacenados requeridos por el backend.
4. Crear o configurar la cuenta de servicio de la aplicación con **privilegios mínimos**. La cuenta prevista, `app_padron`, debe tener permiso para ejecutar únicamente los procedimientos necesarios, no acceso general a las tablas.
5. Probar la conectividad desde el equipo donde correrá la API.

**Importante:** revisa y cambia cualquier contraseña de ejemplo incluida en scripts SQL antes de ejecutar el script en un entorno compartido. No publiques contraseñas reales en GitHub. Los datos de prueba deben ser ficticios.

La dirección configurada actualmente para SQL Server es `172.16.0.100`, puerto `1433`. Debe ajustarse si la dirección, instancia o puerto reales son distintos.

## Configuración de la aplicación

La configuración base se encuentra en:

`src/main/resources/application.properties`

Actualmente contiene, entre otras, estas propiedades:

- Nombre de aplicación: `empadronamiento-api`
- Puerto HTTP de la API: `8080`
- Base de datos: `PadronElectoral`
- Usuario de base de datos: `app_padron`
- Contraseña: se obtiene de la variable de entorno `DB_PASSWORD`

La contraseña está configurada mediante:

```properties
spring.datasource.password=${DB_PASSWORD:}
```

Por tanto, define `DB_PASSWORD` en el entorno de ejecución. No reemplaces esta configuración por una contraseña real guardada en el repositorio.

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

La API escucha actualmente en el puerto `8080`. La aplicación necesita conectividad con SQL Server para completar las operaciones que acceden a la base de datos.

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

- `http://localhost:8080/swagger-ui.html`

La dirección puede variar si se cambia la configuración de Springdoc o el puerto de la aplicación.

## Seguridad actual y mejoras previstas

La seguridad debe desarrollarse y probarse antes de considerar un despliegue real. Las siguientes medidas son una propuesta de trabajo; no todas están implementadas actualmente.

### Medidas de base de datos y red

- Mantener el firewall del servidor habilitado.
- Abrir solo los puertos necesarios y limitar las reglas por dirección IP de origen cuando sea posible.
- Permitir el puerto de SQL Server (`1433`, si ese es el puerto configurado) únicamente desde los equipos que deban conectarse; **no exponer SQL Server directamente a Internet**.
- Si la API y SQL Server están en equipos distintos, permitir la conexión solo entre el host de la API y el servidor de base de datos.
- No abrir el puerto de administración remota a toda la red.
- Usar una cuenta de base de datos dedicada con permisos mínimos y contraseñas robustas.
- Mantener copias de seguridad y restringir quién puede acceder a ellas.

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

## Interfaz de usuario futura

El frontend no forma parte del backend REST actual. Puede desarrollarse como una aplicación web independiente que consuma la API por HTTP/HTTPS y envíe y reciba JSON.

Como mínimo, se prevén dos pantallas:

1. **Registro:** formulario para los datos requeridos, selección de municipio, centro y mesa mediante los endpoints de catálogos, validación de campos y confirmación del número de empadronamiento generado.
2. **Consulta:** selección del tipo de búsqueda, captura de los datos requeridos por el contrato, resolución del CAPTCHA y presentación controlada del resultado.

La tecnología del frontend (por ejemplo, React, Angular o una interfaz web más sencilla) queda pendiente de decisión del equipo. Antes de implementarlo, conviene acordar el contrato de API y el manejo de errores. También se debe configurar CORS con los orígenes concretos del frontend, si se despliega en un origen distinto.

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

## Limitaciones conocidas

- El frontend todavía debe desarrollarse.
- La estrategia final de autenticación y autorización con Spring Security está pendiente de definición e implementación.
- HTTPS debe configurarse para el entorno de despliegue; la URL local HTTP no representa una configuración de producción.
- Las reglas definitivas de firewall dependen de dónde se desplieguen la API y SQL Server y de las IP de los clientes autorizados.
- La conexión, las operaciones y las pruebas deben validarse en el entorno del equipo; una compilación correcta por sí sola no demuestra que todos los endpoints funcionen con la base de datos.

---