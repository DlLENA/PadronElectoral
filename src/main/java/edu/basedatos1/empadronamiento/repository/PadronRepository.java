
package edu.basedatos1.empadronamiento.repository;

import java.sql.Date;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Repository;

@Repository
public class PadronRepository {

    private final JdbcTemplate jdbcTemplate;

    public PadronRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // CATÁLOGOS
    // =====================================================

    public List<Map<String, Object>> listarMunicipios() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_municipios",
                new ColumnMapRowMapper()
        );
    }

    public List<Map<String, Object>> listarGeneros() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_generos",
                new ColumnMapRowMapper()
        );
    }

    public List<Map<String, Object>> listarAfiliaciones() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_afiliaciones",
                new ColumnMapRowMapper()
        );
    }

    public List<Map<String, Object>> listarEstados() {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_estados",
                new ColumnMapRowMapper()
        );
    }

    public List<Map<String, Object>> listarCentros(int municipioId) {
        return jdbcTemplate.query(
                "EXEC Temporal.sp_listar_centros ?",
                new ColumnMapRowMapper(),
                municipioId
        );
    }

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
