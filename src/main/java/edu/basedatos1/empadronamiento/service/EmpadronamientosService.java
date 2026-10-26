
package edu.basedatos1.empadronamiento.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.basedatos1.empadronamiento.repository.PadronRepository;
import edu.basedatos1.empadronamiento_api.model.RegistroRequest;
import edu.basedatos1.empadronamiento_api.model.RegistroResponse;

/**
 * Servicio responsable del registro de nuevos ciudadanos.
 *
 * Valida los datos recibidos, los prepara para la base de datos
 * y procesa el resultado del procedimiento almacenado.
 */
@Service
public class EmpadronamientosService {

    /*
     * Fecha de corte establecida en el proyecto:
     * el ciudadano debe haber nacido el 01/01/2009 o antes.
     */
    private static final LocalDate FECHA_CORTE_NACIMIENTO =
            LocalDate.of(2009, 1, 1);

    /*
     * Se rechazan fechas de nacimiento anteriores al año 1900.
     */
    private static final LocalDate FECHA_MINIMA_NACIMIENTO =
            LocalDate.of(1900, 1, 1);

    /*
     * Formato utilizado para presentar las fechas en los mensajes.
     */
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PadronRepository padronRepository;

    /**
     * Spring inyecta el repositorio encargado del acceso a SQL Server.
     */
    public EmpadronamientosService(PadronRepository padronRepository) {
        this.padronRepository = padronRepository;
    }

    /**
     * Valida y registra un nuevo ciudadano.
     *
     * @param request datos recibidos desde la API
     * @return respuesta con el número de empadronamiento generado
     */
    public RegistroResponse registrar(RegistroRequest request) {

        validarSolicitud(request);

        // Convertir la fecha de emisión a java.sql.Date.
        // Se permite null cuando no se proporciona esa fecha.
        Date fechaEmisionDpi = request.getFechaEmisionDpi() == null
                ? null
                : Date.valueOf(request.getFechaEmisionDpi());

        // Convertir la fecha de nacimiento para enviarla a SQL Server.
        Date fechaNacimiento =
                Date.valueOf(request.getFechaNacimiento());

        /*
         * Si no se especifica un estado, se utiliza el estado 1,
         * que corresponde a ACTIVO en los datos del proyecto.
         */
        int idEstado = request.getIdEstado() == null
                ? 1
                : request.getIdEstado();

        // Ejecutar el procedimiento almacenado mediante el repositorio.
        Map<String, Object> resultado =
                padronRepository.registrarCiudadano(
                        request.getCui().trim(),
                        fechaEmisionDpi,
                        fechaNacimiento,
                        request.getPrimerNombre().trim(),
                        request.getSegundoNombre() == null
                                ? null
                                : request.getSegundoNombre().trim(),
                        request.getApellidos().trim(),
                        request.getLeeEscribe(),
                        request.getTieneDireccionGeoelectoral(),
                        request.getVidente(),
                        request.getIdGenero(),
                        idEstado,
                        request.getIdAfiliacion(),
                        request.getIdMuniVecindad(),
                        request.getIdMesa(),
                        request.getIdMuniResidencia()
                );

        // Interpretar el resultado devuelto por SQL Server.
        return procesarResultado(resultado);
    }

    /**
     * Comprueba las reglas de validación principales
     * antes de intentar registrar al ciudadano.
     */
    private void validarSolicitud(RegistroRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe proporcionar los datos del empadronamiento.");
        }

        // El CUI debe contener exactamente 13 dígitos.
        if (request.getCui() == null
                || !request.getCui().trim().matches("[0-9]{13}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El DPI debe contener exactamente 13 dígitos numéricos.");
        }

        if (request.getFechaNacimiento() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de nacimiento es obligatoria.");
        }

        LocalDate nacimiento = request.getFechaNacimiento();

        // Rechazar fechas demasiado antiguas.
        if (nacimiento.isBefore(FECHA_MINIMA_NACIMIENTO)) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422),
                    "La fecha de nacimiento no puede ser anterior al "
                            + FECHA_MINIMA_NACIMIENTO.format(FORMATO_FECHA)
                            + ".");
        }

        // Aplicar la fecha de corte definida en el proyecto.
        if (nacimiento.isAfter(FECHA_CORTE_NACIMIENTO)) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422),
                    "La persona debe haber nacido el "
                            + FECHA_CORTE_NACIMIENTO.format(FORMATO_FECHA)
                            + " o antes.");
        }

        // Los nombres y apellidos no pueden estar vacíos.
        if (request.getPrimerNombre() == null
                || request.getPrimerNombre().isBlank()
                || request.getApellidos() == null
                || request.getApellidos().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El primer nombre y los apellidos son obligatorios.");
        }

        // Los tres campos booleanos deben tener un valor.
        if (request.getLeeEscribe() == null
                || request.getVidente() == null
                || request.getTieneDireccionGeoelectoral() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar si lee y escribe, si es vidente "
                            + "y si tiene dirección geoelectoral.");
        }

        /*
         * Si se proporciona la fecha de emisión del DPI,
         * no puede ser anterior al nacimiento ni posterior al día actual.
         */
        if (request.getFechaEmisionDpi() != null) {

            LocalDate emision = request.getFechaEmisionDpi();

            if (emision.isBefore(nacimiento)
                    || emision.isAfter(LocalDate.now())) {

                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(422),
                        "La fecha de emisión del DPI no es válida.");
            }
        }
    }

    /**
     * Interpreta la respuesta del procedimiento almacenado.
     *
     * El código 0 indica que el registro se realizó correctamente.
     * Los demás códigos se convierten en errores HTTP apropiados.
     */
    private RegistroResponse procesarResultado(
            Map<String, Object> resultado) {

        if (resultado == null || resultado.get("codigo") == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La base de datos devolvió una respuesta inesperada.");
        }

        int codigo;

        try {
            codigo = ((Number) resultado.get("codigo")).intValue();
        } catch (ClassCastException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La base de datos devolvió una respuesta inesperada.");
        }

        String mensaje = resultado.get("mensaje") == null
                ? "No se pudo procesar el empadronamiento."
                : resultado.get("mensaje").toString();

        if (codigo != 0) {

            String mensajeNormalizado =
                    mensaje.toUpperCase(Locale.ROOT);

            /*
             * No se exponen detalles internos de errores de SQL Server
             * al cliente de la API.
             */
            if (mensajeNormalizado.startsWith("ERROR INTERNO")) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ocurrió un error interno al registrar "
                                + "el empadronamiento.");
            }

            // Los rechazos de negocio se devuelven como HTTP 422.
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422),
                    mensaje);
        }

        Object numero = resultado.get("numero");

        if (numero == null || numero.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La base de datos no devolvió el número "
                            + "de empadronamiento.");
        }

        // Construir el modelo de respuesta definido en la API.
        RegistroResponse respuesta = new RegistroResponse();
        respuesta.setNumeroEmpadronamiento(numero.toString());
        respuesta.setMensaje(mensaje);

        return respuesta;
    }
}
