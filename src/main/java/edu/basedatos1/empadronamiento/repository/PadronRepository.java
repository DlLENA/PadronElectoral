
package edu.basedatos1.empadronamiento.repository;

import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;

/**
 * Repositorio encargado de comunicarse con SQL Server.
 *
 * Utiliza JdbcTemplate para ejecutar los procedimientos
 * almacenados definidos en el esquema Temporal.
 *
 * Los resultados se reciben como listas o mapas para que
 * los servicios puedan transformarlos en los modelos de la API.
 */
@Repository
public class PadronRepository {

    /*
     * JdbcTemplate permite ejecutar instrucciones SQL
     * y recuperar sus resultados sin administrar manualmente
     * las conexiones JDBC.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Spring inyecta automáticamente JdbcTemplate.
     *
     * @param jdbcTemplate herramienta de acceso a SQL Server
     */
    public PadronRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // CATÁLOGOS
    // =====================================================

    /**
     * Obtiene los municipios disponibles.
     */
    public List<Map<String, Object>> listarMunicipios() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_municipios",
                new ColumnMapRowMapper()
        );
    }

    /**
     * Obtiene los géneros disponibles.
     */
    public List<Map<String, Object>> listarGeneros() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_generos",
                new ColumnMapRowMapper()
        );
    }

    /**
     * Obtiene las situaciones de afiliación disponibles.
     */
    public List<Map<String, Object>> listarAfiliaciones() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_afiliaciones",
                new ColumnMapRowMapper()
        );
    }

    /**
     * Obtiene los estados del padrón.
     */
    public List<Map<String, Object>> listarEstados() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_estados",
                new ColumnMapRowMapper()
        );
    }

    /**
     * Obtiene los centros de votación de un municipio.
     *
     * @param municipioId identificador del municipio
     */
    public List<Map<String, Object>> listarCentros(int municipioId) {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_centros ?",
                new ColumnMapRowMapper(),
                municipioId
        );
    }

    /**
     * Obtiene las mesas electorales de un centro.
     *
     * @param centroId identificador del centro de votación
     */
    public List<Map<String, Object>> listarMesas(int centroId) {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_mesas ?",
                new ColumnMapRowMapper(),
                centroId
        );
    }

    // =====================================================
    // REGISTRO DE CIUDADANOS
    // =====================================================

    /**
     * Ejecuta el procedimiento almacenado que registra
     * un ciudadano en el padrón electoral.
     *
     * El procedimiento almacenado valida los datos,
     * genera el número de empadronamiento y devuelve
     * el resultado de la operación.
     *
     * @return mapa con las columnas devueltas por el procedimiento
     */
    public Map<String, Object> registrarCiudadano(
            String cui,
            Date fechaEmisionDpi,
            Date fechaNacimiento,
            String primerNombre,
            String segundoNombre,
            String apellidos,
            boolean leeEscribe,
            boolean tieneDireccionGeoelectoral,
            boolean vidente,
            int idGenero,
            int idEstado,
            int idAfiliacion,
            int idMuniVecindad,
            int idMesa,
            int idMuniResidencia) {

        /*
         * Los signos ? representan parámetros.
         * JdbcTemplate envía sus valores por separado,
         * evitando concatenarlos directamente en el SQL.
         */
        String sql = """
                EXEC Temporal.sp_registrar_ciudadano
                    @CUI = ?,
                    @Fecha_Emision_DPI = ?,
                    @Fecha_Nacimiento = ?,
                    @Primer_Nombre = ?,
                    @Segundo_Nombre = ?,
                    @Apellidos = ?,
                    @Lee_Escribe = ?,
                    @Tiene_Direccion_Geoelectoral = ?,
                    @Vidente = ?,
                    @ID_Genero = ?,
                    @ID_Estado = ?,
                    @ID_Afiliacion = ?,
                    @ID_Muni_Vecindad = ?,
                    @ID_Mesa = ?,
                    @ID_Muni_Residencia = ?
                """;

        /*
         * queryForMap espera una única fila de resultado.
         * Se indica explícitamente Types.DATE para la fecha
         * de emisión, que puede ser null según el procedimiento.
         */
        return jdbcTemplate.queryForMap(
                sql,
                cui,
                new SqlParameterValue(Types.DATE, fechaEmisionDpi),
                fechaNacimiento,
                primerNombre,
                segundoNombre,
                apellidos,
                leeEscribe,
                tieneDireccionGeoelectoral,
                vidente,
                idGenero,
                idEstado,
                idAfiliacion,
                idMuniVecindad,
                idMesa,
                idMuniResidencia
        );
    }

    // =====================================================
    // CONSULTA DE EMPADRONAMIENTO
    // =====================================================

    /**
     * Busca un ciudadano mediante su DPI o número
     * de empadronamiento y su fecha de nacimiento.
     *
     * @param tipo tipo de documento utilizado en la consulta
     * @param numero número de DPI o empadronamiento
     * @param fechaNacimiento fecha de nacimiento que debe coincidir
     * @return filas devueltas por el procedimiento almacenado
     */
    public List<Map<String, Object>> consultarEmpadronamiento(
            String tipo,
            String numero,
            Date fechaNacimiento) {

        String sql = """
                EXEC Temporal.sp_consultar_empadronamiento
                    @Tipo = ?,
                    @Numero = ?,
                    @Fecha_Nacimiento = ?
                """;

        return jdbcTemplate.query(
                sql,
                new ColumnMapRowMapper(),
                tipo,
                numero,
                fechaNacimiento
        );
    }
}
