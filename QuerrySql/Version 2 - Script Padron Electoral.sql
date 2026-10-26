/* =====================================================================
   PROYECTO BASE DE DATOS I - PADRÓN ELECTORAL (JUTIAPA) - VERSION 2
   Script DDL basado en el modelo Arquitecto_Datos.dm1
   Motor: SQL Server
   VERSION 2: correcciones mínimas documentadas en comentarios V2.
   ---------------------------------------------------------------------
   Reglas de negocio aplicadas:
     RN-1  No. de empadronamiento: 10 caracteres alfanuméricos, único,
           generado por el sistema.
     RN-2  DPI (CUI): exactamente 13 dígitos numéricos, único.
     RN-3  Edad: nacido el 01/01/2009 o antes (18 años al 01/01/2027).
     RN-4  Género: solo MASCULINO o FEMENINO.
     RN-5  Solo departamento de JUTIAPA y sus 17 municipios.
     RN-6  Todos los datos de texto se almacenan en MAYÚSCULAS.
   ===================================================================== */

-- =====================================================================
-- 1. BASE DE DATOS Y ESQUEMA
-- =====================================================================
USE master;
GO

-- V2 CORRECCIÓN (sección 1): se evita borrar una base existente por accidente.
-- Para recrearla intencionalmente, cambia @EliminarBaseExistente a 1.
DECLARE @EliminarBaseExistente BIT = 0;

IF DB_ID('PadronElectoral') IS NOT NULL
BEGIN
    IF @EliminarBaseExistente = 0
    BEGIN
        ;THROW 50001, 'La base PadronElectoral ya existe. Haz respaldo y cambia @EliminarBaseExistente a 1 solo si deseas eliminarla y recrearla.', 1;
    END;

    ALTER DATABASE PadronElectoral SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE PadronElectoral;
END;
GO

CREATE DATABASE PadronElectoral;
GO

USE PadronElectoral;
GO

CREATE SCHEMA Temporal;
GO

-- =====================================================================
-- 2. CATÁLOGOS
-- =====================================================================

-- RN-5: el único departamento permitido es JUTIAPA
CREATE TABLE Temporal.Departamentos (
    ID_Departamento INT          NOT NULL,
    Nombre          VARCHAR(50)  NOT NULL,
    CONSTRAINT PK_ID_Departamento PRIMARY KEY (ID_Departamento),
    CONSTRAINT CK_Departamentos_Jutiapa CHECK (Nombre = 'JUTIAPA'),                         -- RN-5
    CONSTRAINT CK_Departamentos_Mayus CHECK (Nombre COLLATE Latin1_General_CS_AS = UPPER(Nombre)) -- RN-6
);
GO

CREATE TABLE Temporal.Municipios (
    ID_Municipio    INT          NOT NULL,
    Nombre          VARCHAR(50)  NOT NULL,
    ID_Departamento INT          NOT NULL,
    CONSTRAINT PK_ID_Municipio PRIMARY KEY (ID_Municipio),
    CONSTRAINT FK_Municipios_Departamentos FOREIGN KEY (ID_Departamento)
        REFERENCES Temporal.Departamentos (ID_Departamento),
    CONSTRAINT UQ_Municipios_Nombre UNIQUE (Nombre),
    CONSTRAINT CK_Municipios_Mayus CHECK (Nombre COLLATE Latin1_General_CS_AS = UPPER(Nombre))   -- RN-6
);
GO

-- RN-4: solo MASCULINO o FEMENINO
CREATE TABLE Temporal.Generos (
    ID_Genero INT          NOT NULL,
    Nombre    VARCHAR(20)  NOT NULL,
    CONSTRAINT PK_Genero PRIMARY KEY (ID_Genero),
    CONSTRAINT UQ_Generos_Nombre UNIQUE (Nombre),
    CONSTRAINT CK_Generos_Valores CHECK (Nombre IN ('MASCULINO', 'FEMENINO')),            -- RN-4
    CONSTRAINT CK_Generos_Mayus CHECK (Nombre COLLATE Latin1_General_CS_AS = UPPER(Nombre))     -- RN-6
);
GO

CREATE TABLE Temporal.Estados_Padron (
    ID_Estado   INT          NOT NULL,
    Descripcion VARCHAR(30)  NOT NULL,
    CONSTRAINT PK_Estado PRIMARY KEY (ID_Estado),
    CONSTRAINT UQ_Estados_Descripcion UNIQUE (Descripcion),
    CONSTRAINT CK_Estados_Mayus CHECK (Descripcion COLLATE Latin1_General_CS_AS = UPPER(Descripcion)) -- RN-6
);
GO

CREATE TABLE Temporal.Afiliaciones (
    ID_Afiliacion INT          NOT NULL,
    Situacion     VARCHAR(50)  NOT NULL,
    CONSTRAINT PK_Afiliaciones PRIMARY KEY (ID_Afiliacion),
    CONSTRAINT UQ_Afiliaciones_Situacion UNIQUE (Situacion),
    CONSTRAINT CK_Afiliaciones_Mayus CHECK (Situacion COLLATE Latin1_General_CS_AS = UPPER(Situacion)) -- RN-6
);
GO

CREATE TABLE Temporal.Centros_Votacion (
    ID_Centro    INT           NOT NULL,
    Nombre       VARCHAR(100)  NOT NULL,
    Direccion    VARCHAR(150)  NOT NULL,
    ID_Municipio INT           NOT NULL,
    CONSTRAINT PK_Centro PRIMARY KEY (ID_Centro),
    CONSTRAINT FK_Centros_Municipios FOREIGN KEY (ID_Municipio)
        REFERENCES Temporal.Municipios (ID_Municipio),
    CONSTRAINT CK_Centros_Mayus CHECK (                                                     -- RN-6
        Nombre    COLLATE Latin1_General_CS_AS = UPPER(Nombre) AND
        Direccion COLLATE Latin1_General_CS_AS = UPPER(Direccion))
);
GO

CREATE TABLE Temporal.Mesas_Electorales (
    ID_Mesa     INT NOT NULL,
    Numero_Mesa INT NOT NULL,
    ID_Centro   INT NOT NULL,
    CONSTRAINT PK_ID_Mesa PRIMARY KEY (ID_Mesa),
    CONSTRAINT FK_Mesas_Centros FOREIGN KEY (ID_Centro)
        REFERENCES Temporal.Centros_Votacion (ID_Centro),
    CONSTRAINT Uq_Centro_NumeroMesa UNIQUE (Numero_Mesa, ID_Centro),
    CONSTRAINT CK_Mesas_Numero CHECK (Numero_Mesa > 0)
);
GO

-- =====================================================================
-- 3. TABLA PRINCIPAL
-- =====================================================================
CREATE TABLE Temporal.Ciudadanos (
    ID_Ciudadano                 INT IDENTITY(1,1) NOT NULL,
    Numero_Empadronamiento       VARCHAR(10)   NOT NULL,
    CUI                          VARCHAR(13)   NOT NULL,
    -- V2 NOTA: se conserva NULL porque el requerimiento no declara explícitamente
    -- que esta fecha sea obligatoria; si el grupo la define como obligatoria,
    -- cambiar a NOT NULL y exigirla también en sp_registrar_ciudadano.
    Fecha_Emision_DPI            DATE          NULL,
    Fecha_Nacimiento             DATE          NOT NULL,
    Primer_Nombre                VARCHAR(50)   NOT NULL,
    Segundo_Nombre               VARCHAR(50)   NULL,
    Apellidos                    VARCHAR(100)  NOT NULL,
    Lee_Escribe                  BIT           NOT NULL,
    Tiene_Direccion_Geoelectoral BIT           NOT NULL,
    Vidente                      BIT           NOT NULL,
    ID_Genero                    INT           NOT NULL,
    ID_Estado                    INT           NOT NULL,
    ID_Afiliacion                INT           NOT NULL,
    ID_Muni_Vecindad             INT           NOT NULL,
    ID_Mesa                      INT           NOT NULL,
    ID_Muni_Residencia           INT           NOT NULL,

    CONSTRAINT PK_ID_Ciudadano PRIMARY KEY (ID_Ciudadano),

    -- Llaves foráneas (las 9 relaciones del modelo, 6 aquí y 3 en los catálogos)
    CONSTRAINT FK_Ciudadanos_Generos     FOREIGN KEY (ID_Genero)          REFERENCES Temporal.Generos (ID_Genero),
    CONSTRAINT FK_Ciudadanos_Estados     FOREIGN KEY (ID_Estado)          REFERENCES Temporal.Estados_Padron (ID_Estado),
    CONSTRAINT FK_Ciudadanos_Afiliacion  FOREIGN KEY (ID_Afiliacion)      REFERENCES Temporal.Afiliaciones (ID_Afiliacion),
    CONSTRAINT FK_Ciudadanos_MuniVecind  FOREIGN KEY (ID_Muni_Vecindad)   REFERENCES Temporal.Municipios (ID_Municipio),
    CONSTRAINT FK_Ciudadanos_Mesas       FOREIGN KEY (ID_Mesa)            REFERENCES Temporal.Mesas_Electorales (ID_Mesa),
    CONSTRAINT FK_Ciudadanos_MuniResid   FOREIGN KEY (ID_Muni_Residencia) REFERENCES Temporal.Municipios (ID_Municipio),

    -- RN-1: No. de empadronamiento de 10 caracteres alfanuméricos y único
    CONSTRAINT Uq_NumeroEmpadronamiento UNIQUE (Numero_Empadronamiento),
    -- V2 CORRECCIÓN (tabla Ciudadanos): DATALENGTH evita aceptar espacios finales,
    -- ya que LEN() no cuenta los espacios finales en SQL Server.
    CONSTRAINT CK_Ciudadanos_NoEmpadronamiento CHECK (
        DATALENGTH(Numero_Empadronamiento) = 10 AND
        Numero_Empadronamiento COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9]%'),

    -- RN-2: CUI de 13 dígitos numéricos y único
    CONSTRAINT Uq_CUI UNIQUE (CUI),
    CONSTRAINT CK_Ciudadanos_CUI CHECK (
        DATALENGTH(CUI) = 13 AND
        CUI COLLATE Latin1_General_BIN2 NOT LIKE '%[^0-9]%'),

    -- RN-3: nacido el 01/01/2009 o antes (fecha fija, no GETDATE())
    CONSTRAINT CK_Ciudadanos_Edad CHECK (Fecha_Nacimiento <= '20090101'),

    -- Coherencia de fechas: el DPI se emite después del nacimiento
    CONSTRAINT CK_Ciudadanos_FechaDPI CHECK (
        Fecha_Emision_DPI IS NULL OR Fecha_Emision_DPI >= Fecha_Nacimiento),

    -- RN-6: nombres y apellidos en mayúsculas
    CONSTRAINT CK_Ciudadanos_Mayus CHECK (
        Primer_Nombre COLLATE Latin1_General_CS_AS = UPPER(Primer_Nombre) AND
        Apellidos     COLLATE Latin1_General_CS_AS = UPPER(Apellidos) AND
        (Segundo_Nombre IS NULL OR
         Segundo_Nombre COLLATE Latin1_General_CS_AS = UPPER(Segundo_Nombre))),

    -- Evita nombres vacíos o solo con espacios
    CONSTRAINT CK_Ciudadanos_NoVacios CHECK (
        LEN(LTRIM(RTRIM(Primer_Nombre))) > 0 AND
        LEN(LTRIM(RTRIM(Apellidos))) > 0)
);
GO

-- RN-4 y RN-5 también se cumplen en Ciudadanos por las FK:
--   ID_Genero solo puede apuntar a MASCULINO o FEMENINO, y los municipios
--   de vecindad y residencia solo pueden ser de JUTIAPA (Municipios -> Departamentos).

-- =====================================================================
-- 4. DATOS DE CATÁLOGOS
-- =====================================================================

-- RN-5
INSERT INTO Temporal.Departamentos (ID_Departamento, Nombre) VALUES (1, 'JUTIAPA');

-- RN-5: los 17 municipios de Jutiapa
INSERT INTO Temporal.Municipios (ID_Municipio, Nombre, ID_Departamento) VALUES
 (1,  'JUTIAPA',              1),
 (2,  'EL PROGRESO',          1),
 (3,  'SANTA CATARINA MITA',  1),
 (4,  'AGUA BLANCA',          1),
 (5,  'ASUNCIÓN MITA',        1),
 (6,  'YUPILTEPEQUE',         1),
 (7,  'ATESCATEMPA',          1),
 (8,  'JEREZ',                1),
 (9,  'EL ADELANTO',          1),
 (10, 'ZAPOTITLÁN',           1),
 (11, 'COMAPA',               1),
 (12, 'JALPATAGUA',           1),
 (13, 'CONGUACO',             1),
 (14, 'MOYUTA',               1),
 (15, 'PASACO',               1),
 (16, 'SAN JOSÉ ACATEMPA',    1),
 (17, 'QUESADA',              1);

-- RN-4
INSERT INTO Temporal.Generos (ID_Genero, Nombre) VALUES
 (1, 'MASCULINO'),
 (2, 'FEMENINO');

-- Ajustar a los estados que definan en el documento de análisis
INSERT INTO Temporal.Estados_Padron (ID_Estado, Descripcion) VALUES
 (1, 'ACTIVO'),
 (2, 'INACTIVO'),
 (3, 'SUSPENDIDO');

INSERT INTO Temporal.Afiliaciones (ID_Afiliacion, Situacion) VALUES
 (1, 'AFILIADO'),
 (2, 'NO ESTÁ AFILIADO');

-- Datos ficticios: un centro de votación por cada uno de los 17 municipios.
-- El ID del centro coincide con el ID de su municipio.
INSERT INTO Temporal.Centros_Votacion (ID_Centro, Nombre, Direccion, ID_Municipio)
SELECT ID_Municipio,
       'ESCUELA OFICIAL URBANA MIXTA ' + Nombre,
       'CABECERA MUNICIPAL, ZONA 1, ' + Nombre,
       ID_Municipio
FROM Temporal.Municipios;

-- Dos mesas por centro (mesa 1 y mesa 2), 34 en total.
-- ID_Mesa = (ID_Centro - 1) * 2 + Numero_Mesa  ->  centro 1: mesas 1 y 2,
-- centro 2: mesas 3 y 4, centro 3: mesas 5 y 6, etc.
INSERT INTO Temporal.Mesas_Electorales (ID_Mesa, Numero_Mesa, ID_Centro)
SELECT (c.ID_Centro - 1) * 2 + n.Numero_Mesa,
       n.Numero_Mesa,
       c.ID_Centro
FROM Temporal.Centros_Votacion c
CROSS JOIN (VALUES (1), (2)) AS n (Numero_Mesa);
GO

-- =====================================================================
-- 5. PROCEDIMIENTOS ALMACENADOS
-- =====================================================================

-- 5.1 Generación del No. de empadronamiento (RN-1)
-- Va en un procedimiento porque NEWID() no se puede usar dentro de una
-- función escalar. Se repite hasta obtener un valor que no exista.
CREATE PROCEDURE Temporal.sp_generar_no_empadronamiento
    @Numero VARCHAR(10) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @chars CHAR(36) = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    DECLARE @i INT, @candidato VARCHAR(10);

    WHILE 1 = 1
    BEGIN
        SET @candidato = '';
        SET @i = 1;
        WHILE @i <= 10
        BEGIN
            -- V2 CORRECCIÓN (procedimiento generador): convertir a BIGINT evita
            -- el desbordamiento excepcional de ABS(-2147483648).
            SET @candidato = @candidato + SUBSTRING(@chars, ABS(CONVERT(BIGINT, CHECKSUM(NEWID()))) % 36 + 1, 1);
            SET @i += 1;
        END;

        -- V2 CORRECCIÓN (procedimiento generador): UPDLOCK + HOLDLOCK protege
        -- la comprobación mientras la transacción de registro permanezca abierta.
        IF NOT EXISTS (SELECT 1 FROM Temporal.Ciudadanos WITH (UPDLOCK, HOLDLOCK)
                       WHERE Numero_Empadronamiento = @candidato)
            BREAK;
    END;

    SET @Numero = @candidato;
END;
GO

-- 5.2 Registro de un ciudadano (Módulo 1)
-- Aplica RN-6 convirtiendo a mayúsculas, valida RN-1 a RN-5 con mensajes
-- claros y devuelve: codigo (0 = éxito, 1 = error), mensaje, numero.
CREATE PROCEDURE Temporal.sp_registrar_ciudadano
    @CUI                          VARCHAR(20),
    @Fecha_Emision_DPI            DATE = NULL,
    @Fecha_Nacimiento             DATE,
    @Primer_Nombre                VARCHAR(50),
    @Segundo_Nombre               VARCHAR(50) = NULL,
    @Apellidos                    VARCHAR(100),
    @Lee_Escribe                  BIT,
    @Tiene_Direccion_Geoelectoral BIT,
    @Vidente                      BIT,
    @ID_Genero                    INT,
    @ID_Estado                    INT = 1,
    @ID_Afiliacion                INT,
    @ID_Muni_Vecindad             INT,
    @ID_Mesa                      INT,
    @ID_Muni_Residencia           INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @Numero VARCHAR(10);

    -- RN-6: limpiar espacios y convertir a mayúsculas
    SET @CUI            = LTRIM(RTRIM(@CUI));
    SET @Primer_Nombre  = UPPER(LTRIM(RTRIM(@Primer_Nombre)));
    SET @Segundo_Nombre = NULLIF(UPPER(LTRIM(RTRIM(@Segundo_Nombre))), '');
    SET @Apellidos      = UPPER(LTRIM(RTRIM(@Apellidos)));

    -- RN-2
    IF @CUI IS NULL OR LEN(@CUI) <> 13 OR @CUI LIKE '%[^0-9]%'
    BEGIN
        SELECT 1 AS codigo, 'EL DPI (CUI) DEBE TENER EXACTAMENTE 13 DÍGITOS NUMÉRICOS.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    IF EXISTS (SELECT 1 FROM Temporal.Ciudadanos WHERE CUI = @CUI)
    BEGIN
        SELECT 1 AS codigo, 'YA EXISTE UN CIUDADANO EMPADRONADO CON ESE DPI.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    -- RN-3
    IF @Fecha_Nacimiento IS NULL OR @Fecha_Nacimiento > '20090101'
    BEGIN
        SELECT 1 AS codigo, 'LA PERSONA DEBE HABER NACIDO EL 01/01/2009 O ANTES.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    IF @Fecha_Emision_DPI IS NOT NULL
       AND (@Fecha_Emision_DPI < @Fecha_Nacimiento OR @Fecha_Emision_DPI > CAST(GETDATE() AS DATE))
    BEGIN
        SELECT 1 AS codigo, 'LA FECHA DE EMISIÓN DEL DPI NO ES VÁLIDA.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    IF @Primer_Nombre = '' OR @Apellidos = ''
    BEGIN
        SELECT 1 AS codigo, 'EL PRIMER NOMBRE Y LOS APELLIDOS SON OBLIGATORIOS.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    -- RN-4
    IF NOT EXISTS (SELECT 1 FROM Temporal.Generos WHERE ID_Genero = @ID_Genero)
    BEGIN
        SELECT 1 AS codigo, 'EL GÉNERO DEBE SER MASCULINO O FEMENINO.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    -- RN-5
    IF NOT EXISTS (SELECT 1 FROM Temporal.Municipios WHERE ID_Municipio = @ID_Muni_Vecindad)
       OR NOT EXISTS (SELECT 1 FROM Temporal.Municipios WHERE ID_Municipio = @ID_Muni_Residencia)
    BEGIN
        SELECT 1 AS codigo, 'LOS MUNICIPIOS DEBEN PERTENECER AL DEPARTAMENTO DE JUTIAPA.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    -- V2 CORRECCIÓN (procedimiento de registro): validar estos catálogos antes
    -- del INSERT para devolver mensajes claros en vez de depender solo de la FK.
    IF NOT EXISTS (SELECT 1 FROM Temporal.Estados_Padron WHERE ID_Estado = @ID_Estado)
    BEGIN
        SELECT 1 AS codigo, 'EL ESTADO DEL PADRÓN NO ES VÁLIDO.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM Temporal.Afiliaciones WHERE ID_Afiliacion = @ID_Afiliacion)
    BEGIN
        SELECT 1 AS codigo, 'LA SITUACIÓN DE AFILIACIÓN NO ES VÁLIDA.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    IF NOT EXISTS (SELECT 1 FROM Temporal.Mesas_Electorales WHERE ID_Mesa = @ID_Mesa)
    BEGIN
        SELECT 1 AS codigo, 'LA MESA ELECTORAL SELECCIONADA NO ES VÁLIDA.' AS mensaje, NULL AS numero;
        RETURN;
    END;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- RN-1
        EXEC Temporal.sp_generar_no_empadronamiento @Numero OUTPUT;

        INSERT INTO Temporal.Ciudadanos (
            Numero_Empadronamiento, CUI, Fecha_Emision_DPI, Fecha_Nacimiento,
            Primer_Nombre, Segundo_Nombre, Apellidos,
            Lee_Escribe, Tiene_Direccion_Geoelectoral, Vidente,
            ID_Genero, ID_Estado, ID_Afiliacion,
            ID_Muni_Vecindad, ID_Mesa, ID_Muni_Residencia)
        VALUES (
            @Numero, @CUI, @Fecha_Emision_DPI, @Fecha_Nacimiento,
            @Primer_Nombre, @Segundo_Nombre, @Apellidos,
            @Lee_Escribe, @Tiene_Direccion_Geoelectoral, @Vidente,
            @ID_Genero, @ID_Estado, @ID_Afiliacion,
            @ID_Muni_Vecindad, @ID_Mesa, @ID_Muni_Residencia);

        COMMIT TRANSACTION;

        SELECT 0 AS codigo, 'EMPADRONAMIENTO REGISTRADO CORRECTAMENTE.' AS mensaje, @Numero AS numero;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 1 AS codigo, ERROR_MESSAGE() AS mensaje, NULL AS numero;
    END CATCH;
END;
GO

-- 5.3 Consulta de empadronamiento (Módulo 2)
-- Busca por No. de empadronamiento o por DPI y exige que coincida la fecha
-- de nacimiento. Si no hay coincidencia devuelve un resultado vacío, sin
-- indicar si el documento existe o qué dato estuvo mal.
--   @Tipo:   'EMPADRONAMIENTO' o 'DPI'
--   @Numero: No. de empadronamiento (10) o CUI (13)
CREATE PROCEDURE Temporal.sp_consultar_empadronamiento
    @Tipo             VARCHAR(20),
    @Numero           VARCHAR(13),
    @Fecha_Nacimiento DATE
AS
BEGIN
    SET NOCOUNT ON;

    SET @Tipo   = UPPER(LTRIM(RTRIM(@Tipo)));
    SET @Numero = UPPER(LTRIM(RTRIM(@Numero)));   -- RN-6: el No. se guarda en mayúsculas

    SELECT
        -- Identificación
        c.Numero_Empadronamiento                          AS No_Empadronamiento,
        c.CUI                                             AS DPI,
        c.Fecha_Emision_DPI,
        -- Datos personales
        c.Primer_Nombre,
        c.Segundo_Nombre,
        c.Apellidos,
        c.Fecha_Nacimiento,
        g.Nombre                                          AS Genero,
        CASE c.Lee_Escribe WHEN 1 THEN 'SI' ELSE 'NO' END AS Lee_Escribe,
        CASE c.Vidente     WHEN 1 THEN 'SI' ELSE 'NO' END AS Vidente,
        -- Vecindad
        mv.Nombre                                         AS Municipio_Vecindad,
        dv.Nombre                                         AS Departamento_Vecindad,
        -- Residencia
        mr.Nombre                                         AS Municipio_Residencia,
        dr.Nombre                                         AS Departamento_Residencia,
        CASE c.Tiene_Direccion_Geoelectoral WHEN 1 THEN 'SI' ELSE 'NO' END AS Direccion_Geoelectoral,
        -- Votación
        cv.Nombre                                         AS Centro_Votacion,
        cv.Direccion                                      AS Direccion_Centro,
        mc.Nombre                                         AS Municipio_Centro,
        me.Numero_Mesa,
        a.Situacion                                       AS Afiliacion,
        e.Descripcion                                     AS Estado_Padron
    FROM Temporal.Ciudadanos c
    JOIN Temporal.Generos           g  ON g.ID_Genero        = c.ID_Genero
    JOIN Temporal.Estados_Padron    e  ON e.ID_Estado        = c.ID_Estado
    JOIN Temporal.Afiliaciones      a  ON a.ID_Afiliacion    = c.ID_Afiliacion
    JOIN Temporal.Municipios        mv ON mv.ID_Municipio    = c.ID_Muni_Vecindad
    JOIN Temporal.Departamentos     dv ON dv.ID_Departamento = mv.ID_Departamento
    JOIN Temporal.Municipios        mr ON mr.ID_Municipio    = c.ID_Muni_Residencia
    JOIN Temporal.Departamentos     dr ON dr.ID_Departamento = mr.ID_Departamento
    JOIN Temporal.Mesas_Electorales me ON me.ID_Mesa         = c.ID_Mesa
    JOIN Temporal.Centros_Votacion  cv ON cv.ID_Centro       = me.ID_Centro
    JOIN Temporal.Municipios        mc ON mc.ID_Municipio    = cv.ID_Municipio
    WHERE c.Fecha_Nacimiento = @Fecha_Nacimiento
      AND (   (@Tipo = 'DPI'             AND c.CUI                    = @Numero)
           OR (@Tipo = 'EMPADRONAMIENTO' AND c.Numero_Empadronamiento = @Numero));
END;
GO

-- 5.4 Procedimientos para llenar los combos de la aplicación
CREATE PROCEDURE Temporal.sp_listar_municipios
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Municipio, Nombre FROM Temporal.Municipios ORDER BY Nombre;
END;
GO

CREATE PROCEDURE Temporal.sp_listar_generos
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Genero, Nombre FROM Temporal.Generos ORDER BY ID_Genero;
END;
GO

CREATE PROCEDURE Temporal.sp_listar_afiliaciones
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Afiliacion, Situacion FROM Temporal.Afiliaciones ORDER BY ID_Afiliacion;
END;
GO

CREATE PROCEDURE Temporal.sp_listar_estados
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Estado, Descripcion FROM Temporal.Estados_Padron ORDER BY ID_Estado;
END;
GO

-- Centros de votación de un municipio (combo dependiente)
CREATE PROCEDURE Temporal.sp_listar_centros
    @ID_Municipio INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Centro, Nombre, Direccion
    FROM Temporal.Centros_Votacion
    WHERE ID_Municipio = @ID_Municipio
    ORDER BY Nombre;
END;
GO

-- Mesas de un centro de votación (combo dependiente)
CREATE PROCEDURE Temporal.sp_listar_mesas
    @ID_Centro INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT ID_Mesa, Numero_Mesa
    FROM Temporal.Mesas_Electorales
    WHERE ID_Centro = @ID_Centro
    ORDER BY Numero_Mesa;
END;
GO

-- =====================================================================
-- 6. USUARIO DE LA APLICACIÓN (permisos mínimos)
-- ---------------------------------------------------------------------
-- La app se conecta con este usuario y solo puede ejecutar los
-- procedimientos del esquema Temporal. No puede leer ni modificar las
-- tablas directamente, así todo pasa por las validaciones de los SP.
-- Requiere que el servidor tenga autenticación mixta (SQL Server y Windows).
-- CAMBIEN LA CONTRASEÑA y no la incluyan en la documentación.
-- =====================================================================
USE master;
GO

IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'app_padron')
    DROP LOGIN app_padron;
GO

CREATE LOGIN app_padron
    WITH PASSWORD = 'Cambiar_Esta_Clave#2026',
         DEFAULT_DATABASE = PadronElectoral,
         CHECK_POLICY = ON;
GO

USE PadronElectoral;
GO

CREATE USER app_padron FOR LOGIN app_padron WITH DEFAULT_SCHEMA = Temporal;
GO

-- Solo EXECUTE sobre los procedimientos del esquema
GRANT EXECUTE ON SCHEMA::Temporal TO app_padron;
GO

-- =====================================================================
-- 7. PRUEBAS (datos ficticios)
-- =====================================================================

-- Caso válido: debe devolver codigo 0 y el No. de empadronamiento
EXEC Temporal.sp_registrar_ciudadano
    @CUI = '1234567890101', @Fecha_Emision_DPI = '20200315', @Fecha_Nacimiento = '20000510',
    @Primer_Nombre = 'juan', @Segundo_Nombre = 'carlos', @Apellidos = 'pérez lópez',
    @Lee_Escribe = 1, @Tiene_Direccion_Geoelectoral = 1, @Vidente = 1,
    @ID_Genero = 1, @ID_Afiliacion = 2, @ID_Muni_Vecindad = 1, @ID_Mesa = 1, @ID_Muni_Residencia = 1;

-- RN-2: CUI repetido -> error
EXEC Temporal.sp_registrar_ciudadano
    @CUI = '1234567890101', @Fecha_Nacimiento = '19990101',
    @Primer_Nombre = 'ana', @Apellidos = 'gómez',
    @Lee_Escribe = 1, @Tiene_Direccion_Geoelectoral = 0, @Vidente = 1,
    @ID_Genero = 2, @ID_Afiliacion = 2, @ID_Muni_Vecindad = 3, @ID_Mesa = 5, @ID_Muni_Residencia = 3;

-- RN-3: menor de edad -> error
EXEC Temporal.sp_registrar_ciudadano
    @CUI = '9876543210101', @Fecha_Nacimiento = '20100601',
    @Primer_Nombre = 'luis', @Apellidos = 'ramírez',
    @Lee_Escribe = 1, @Tiene_Direccion_Geoelectoral = 0, @Vidente = 1,
    @ID_Genero = 1, @ID_Afiliacion = 2, @ID_Muni_Vecindad = 1, @ID_Mesa = 1, @ID_Muni_Residencia = 1;

-- RN-4: género no permitido -> error del CHECK
-- INSERT INTO Temporal.Generos (ID_Genero, Nombre) VALUES (3, 'OTRO');

-- RN-5: otro departamento -> error del CHECK
-- INSERT INTO Temporal.Departamentos (ID_Departamento, Nombre) VALUES (2, 'GUATEMALA');

-- RN-6: minúsculas directo a la tabla -> error del CHECK
-- INSERT INTO Temporal.Generos (ID_Genero, Nombre) VALUES (3, 'masculino');

-- Verificación: los nombres quedaron en mayúsculas
SELECT Numero_Empadronamiento, CUI, Primer_Nombre, Segundo_Nombre, Apellidos
FROM Temporal.Ciudadanos;
GO

-- Consulta por DPI con fecha correcta -> devuelve los datos
EXEC Temporal.sp_consultar_empadronamiento
    @Tipo = 'DPI', @Numero = '1234567890101', @Fecha_Nacimiento = '20000510';

-- Consulta por DPI con fecha incorrecta -> resultado vacío
EXEC Temporal.sp_consultar_empadronamiento
    @Tipo = 'DPI', @Numero = '1234567890101', @Fecha_Nacimiento = '20000511';

-- Consulta por No. de empadronamiento (toma el número generado arriba)
DECLARE @no VARCHAR(10) =
    (SELECT Numero_Empadronamiento FROM Temporal.Ciudadanos WHERE CUI = '1234567890101');
EXEC Temporal.sp_consultar_empadronamiento
    @Tipo = 'EMPADRONAMIENTO', @Numero = @no, @Fecha_Nacimiento = '20000510';

-- Combos
EXEC Temporal.sp_listar_municipios;
EXEC Temporal.sp_listar_centros @ID_Municipio = 1;
EXEC Temporal.sp_listar_mesas @ID_Centro = 1;
GO

-- =====================================================================
-- VERSION 2 - RESUMEN DE CORRECCIONES
-- 1. Se evita eliminar PadronElectoral por accidente; recreación opt-in.
-- 2. CHECK de número y CUI usa DATALENGTH y collation BIN2 para validar
--    longitud exacta y evitar aceptar espacios finales/caracteres no válidos.
-- 3. Generador convierte CHECKSUM a BIGINT antes de ABS y protege la
--    comprobación de candidatos con UPDLOCK/HOLDLOCK dentro de la transacción.
-- 4. sp_registrar_ciudadano valida estado, afiliación y mesa antes del INSERT.
-- 5. Se documenta por qué Fecha_Emision_DPI continúa permitiendo NULL.
--
-- PENDIENTE DE VALIDAR EN EL ENTORNO:
-- ejecutar en una base de pruebas con una cuenta autorizada y verificar
-- creación, permisos, pruebas válidas/ inválidas y respaldo/restauración.
-- =====================================================================
