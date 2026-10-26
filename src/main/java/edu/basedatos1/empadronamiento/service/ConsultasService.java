
package edu.basedatos1.empadronamiento.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.basedatos1.empadronamiento.repository.PadronRepository;
import edu.basedatos1.empadronamiento_api.model.ConsultaRequest;
import edu.basedatos1.empadronamiento_api.model.ConsultaResponse;
import edu.basedatos1.empadronamiento_api.model.SeccionDatosPersonales;
import edu.basedatos1.empadronamiento_api.model.SeccionIdentificacion;
import edu.basedatos1.empadronamiento_api.model.SeccionResidenciaVotacion;
import edu.basedatos1.empadronamiento_api.model.SeccionVecindad;
import edu.basedatos1.empadronamiento_api.model.TipoDocumento;

/**
 * Servicio encargado de ejecutar el proceso de consulta de un ciudadano
 * empadronado.
 *
 * Flujo general:
 * 1. Comprueba que la solicitud contenga los datos obligatorios.
 * 2. Valida el CAPTCHA mediante CaptchaService.
 * 3. Normaliza y valida el número del documento.
 * 4. Consulta los datos mediante PadronRepository.
 * 5. Transforma la fila devuelta por SQL Server en la respuesta de la API.
 *
 * El controlador recibe la solicitud HTTP y delega el proceso en este servicio.
 * El repositorio se encarga de ejecutar el procedimiento almacenado;
 * este servicio no construye consultas SQL directamente.
 */
@Service
public class ConsultasService {

    /*
     * El CUI debe contener exactamente 13 dígitos.
     * No se permiten espacios, letras ni otros caracteres.
     */
    private static final Pattern PATRON_DPI =
            Pattern.compile("^[0-9]{13}$");

    /*
     * El número de empadronamiento definido por el proyecto tiene
     * 10 caracteres alfanuméricos: letras mayúsculas y números.
     */
    private static final Pattern PATRON_EMPADRONAMIENTO =
            Pattern.compile("^[A-Z0-9]{10}$");

    private final PadronRepository padronRepository;
    private final CaptchaService captchaService;

    /**
     * Spring inyecta las dependencias necesarias para consultar los datos
     * y verificar el código CAPTCHA.
     *
     * No es necesario crear manualmente estos objetos.
     */
    public ConsultasService(
            PadronRepository padronRepository,
            CaptchaService captchaService) {

        this.padronRepository = padronRepository;
        this.captchaService = captchaService;
    }

    /**
     * Busca un ciudadano por DPI o número de empadronamiento.
     *
     * La fecha de nacimiento debe coincidir con la almacenada en la base
     * de datos. El CAPTCHA debe ser válido antes de ejecutar la consulta.
     *
     * @param request datos recibidos para efectuar la búsqueda
     * @return información del ciudadano organizada por secciones
     * @throws ResponseStatusException si faltan datos, el CAPTCHA no es
     *         válido, el formato es incorrecto o no existe coincidencia
     */
    public ConsultaResponse consultar(ConsultaRequest request) {

        // 1. Comprobar que exista la solicitud y que incluya los campos
        // obligatorios para realizar la consulta.
        if (request == null
                || request.getTipo() == null
                || request.getNumero() == null
                || request.getNumero().isBlank()
                || request.getFechaNacimiento() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe completar los datos de la consulta.");
        }

        /*
         * 2. Verificar el CAPTCHA.
         *
         * CaptchaService comprueba el identificador, la respuesta y
         * la vigencia del código. Esta comprobación se realiza antes
         * de acceder al repositorio y consultar los datos del ciudadano.
         */
        boolean captchaValido = captchaService.validarCaptcha(
                request.getCaptchaId(),
                request.getCaptchaRespuesta());

        if (!captchaValido) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El CAPTCHA es incorrecto o ha expirado. Solicite uno nuevo.");
        }

        /*
         * 3. Normalizar el número del documento.
         *
         * Se eliminan los espacios exteriores y se convierten las letras
         * a mayúsculas. Esto permite aceptar, por ejemplo, un número de
         * empadronamiento escrito con letras minúsculas.
         *
         * Locale.ROOT evita que la conversión dependa del idioma
         * configurado en el sistema operativo.
         */
        String numero = request.getNumero()
                .trim()
                .toUpperCase(Locale.ROOT);

        TipoDocumento tipo = request.getTipo();

        // 4. Validar el formato correspondiente al documento seleccionado.
        if (tipo == TipoDocumento.DPI
                && !PATRON_DPI.matcher(numero).matches()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El DPI debe contener exactamente 13 dígitos.");
        }

        if (tipo == TipoDocumento.EMPADRONAMIENTO
                && !PATRON_EMPADRONAMIENTO.matcher(numero).matches()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El número de empadronamiento debe contener 10 caracteres alfanuméricos.");
        }

        /*
         * 5. Preparar y ejecutar la consulta.
         *
         * El modelo ConsultaRequest utiliza LocalDate, mientras que el
         * repositorio recibe java.sql.Date para enviar la fecha al
         * procedimiento almacenado de SQL Server.
         *
         * PadronRepository ejecuta:
         * Temporal.sp_consultar_empadronamiento
         *
         * El procedimiento busca por el tipo de documento, el número
         * y la fecha de nacimiento. Los parámetros se envían separados,
         * no concatenados dentro de una sentencia SQL.
         */
        Date fechaNacimiento =
                Date.valueOf(request.getFechaNacimiento());

        List<Map<String, Object>> resultados =
                padronRepository.consultarEmpadronamiento(
                        tipo.name(),
                        numero,
                        fechaNacimiento);

        /*
         * 6. Tratar la ausencia de coincidencias.
         *
         * El mismo mensaje se utiliza cuando no coincide el documento
         * o la fecha de nacimiento. Así no se informa por separado
         * si un documento existe en la base de datos.
         */
        if (resultados == null || resultados.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró un empadronamiento que coincida con los datos proporcionados.");
        }

        /*
         * El procedimiento está diseñado para devolver un ciudadano.
         * Se toma la primera fila devuelta por SQL Server.
         */
        Map<String, Object> fila = resultados.get(0);

        /*
         * 7. Construir la sección de identificación.
         *
         * Los nombres utilizados en fila.get(...) deben corresponder
         * a las columnas o alias devueltos por el procedimiento SQL.
         */
        SeccionIdentificacion identificacion =
                new SeccionIdentificacion();

        identificacion.setNumeroEmpadronamiento(
                texto(fila, "No_Empadronamiento"));

        identificacion.setCui(
                texto(fila, "DPI"));

        identificacion.setFechaEmisionDpi(
                fecha(fila.get("Fecha_Emision_DPI")));

        /*
         * 8. Construir la sección de datos personales.
         *
         * Se unen el primer nombre, el segundo nombre (si existe)
         * y los apellidos en un solo campo para la respuesta.
         */
        SeccionDatosPersonales datosPersonales =
                new SeccionDatosPersonales();

        datosPersonales.setNombreCompleto(
                construirNombre(
                        texto(fila, "Primer_Nombre"),
                        texto(fila, "Segundo_Nombre"),
                        texto(fila, "Apellidos")));

        datosPersonales.setGenero(
                texto(fila, "Genero"));

        datosPersonales.setLeeEscribe(
                texto(fila, "Lee_Escribe"));

        datosPersonales.setVidente(
                texto(fila, "Vidente"));

        /*
         * 9. Construir la sección de vecindad.
         *
         * Corresponde al departamento y municipio de vecindad del DPI,
         * no a la residencia actual del ciudadano.
         */
        SeccionVecindad vecindad = new SeccionVecindad();

        vecindad.setDepartamento(
                texto(fila, "Departamento_Vecindad"));

        vecindad.setMunicipio(
                texto(fila, "Municipio_Vecindad"));

        /*
         * 10. Construir la sección de residencia y votación.
         *
         * Además de los datos de residencia y del centro de votación,
         * se asignan el número de mesa y la situación de afiliación.
         * Estos dos últimos valores también forman parte del resultado
         * que exige el modelo de respuesta del proyecto.
         */
        SeccionResidenciaVotacion residenciaVotacion =
                new SeccionResidenciaVotacion();

        residenciaVotacion.setDepartamentoResidencia(
                texto(fila, "Departamento_Residencia"));

        residenciaVotacion.setMunicipioResidencia(
                texto(fila, "Municipio_Residencia"));

        residenciaVotacion.setDireccionGeoelectoral(
                texto(fila, "Direccion_Geoelectoral"));

        residenciaVotacion.setCentroVotacion(
                texto(fila, "Centro_Votacion"));

        residenciaVotacion.setDireccionCentro(
                texto(fila, "Direccion_Centro"));

        residenciaVotacion.setMunicipioCentro(
                texto(fila, "Municipio_Centro"));

        // Número de mesa devuelto por Temporal.sp_consultar_empadronamiento.
        residenciaVotacion.setNumeroMesa(
                entero(fila, "Numero_Mesa"));

        // Situación de afiliación: por ejemplo, AFILIADO o NO ESTÁ AFILIADO.
        residenciaVotacion.setAfiliacion(
                texto(fila, "Afiliacion"));

        /*
         * 11. Ensamblar la respuesta.
         *
         * Se devuelve el estado del padrón y las cuatro secciones
         * definidas por el contrato de la API.
         */
        ConsultaResponse respuesta = new ConsultaResponse();

        respuesta.setEstado(
                texto(fila, "Estado_Padron"));

        respuesta.setIdentificacion(identificacion);
        respuesta.setDatosPersonales(datosPersonales);
        respuesta.setVecindad(vecindad);
        respuesta.setResidenciaVotacion(residenciaVotacion);

        return respuesta;
    }

    /**
     * Obtiene una columna de la fila SQL y la convierte a texto.
     *
     * Si el valor de la base de datos es NULL, devuelve null en lugar
     * de la cadena "null".
     */
    private String texto(Map<String, Object> fila, String columna) {
        Object valor = fila.get(columna);
        return valor == null ? null : valor.toString();
    }

    /**
     * Convierte una fecha devuelta por JDBC a LocalDate.
     *
     * Se contemplan java.sql.Date y LocalDate. La conversión desde
     * texto se conserva como alternativa para un resultado que llegue
     * representado en formato ISO, por ejemplo, 2000-05-21.
     */
    private LocalDate fecha(Object valor) {
        if (valor == null) {
            return null;
        }

        if (valor instanceof Date fechaSql) {
            return fechaSql.toLocalDate();
        }

        if (valor instanceof LocalDate fechaLocal) {
            return fechaLocal;
        }

        return LocalDate.parse(valor.toString());
    }

    /**
     * Convierte una columna numérica a Integer.
     *
     * JDBC normalmente devuelve Numero_Mesa como un tipo Number.
     * Si el procedimiento devuelve NULL, se conserva como null.
     */
    private Integer entero(Map<String, Object> fila, String columna) {
        Object valor = fila.get(columna);

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "La base de datos devolvió un formato inesperado.");
    }

    /**
     * Une los componentes del nombre completo omitiendo los valores
     * nulos o vacíos. Así no se generan espacios innecesarios cuando
     * el ciudadano no tiene segundo nombre registrado.
     */
    private String construirNombre(
            String primerNombre,
            String segundoNombre,
            String apellidos) {

        return java.util.stream.Stream.of(
                    primerNombre,
                    segundoNombre,
                    apellidos)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
