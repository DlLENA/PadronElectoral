
package edu.basedatos1.empadronamiento.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import edu.basedatos1.empadronamiento_api.model.ErrorResponse;

/**
 * Manejador global de excepciones de la API REST.
 *
 * Centraliza la transformación de excepciones en respuestas HTTP con
 * un formato uniforme, utilizando el modelo ErrorResponse.
 *
 * Los controladores no necesitan repetir bloques try-catch para los
 * errores contemplados aquí.
 *
 * Los mensajes enviados al cliente deben ser comprensibles y no revelar
 * consultas SQL, nombres internos de tablas, credenciales ni trazas Java.
 * Los detalles técnicos se registran en el servidor cuando corresponde.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*
     * Mensajes generales reutilizados para evitar duplicar textos
     * y mantener respuestas consistentes entre los endpoints.
     */
    private static final String MENSAJE_SOLICITUD_INVALIDA =
            "La solicitud contiene campos faltantes o inválidos.";

    private static final String MENSAJE_ERROR_INTERNO =
            "Ocurrió un error interno. Intente nuevamente más tarde.";

    /**
     * Maneja las excepciones que contienen un estado HTTP explícito.
     *
     * Los errores 4xx representan problemas de la solicitud y pueden
     * devolver el mensaje preparado por el servicio.
     *
     * Los errores 5xx se registran en el servidor y se transforman
     * en un mensaje genérico para no exponer detalles internos.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarEstadoHttp(
            ResponseStatusException excepcion) {

        if (excepcion.getStatusCode().is4xxClientError()) {
            String mensaje = excepcion.getReason();

            if (mensaje == null || mensaje.isBlank()) {
                mensaje = "La solicitud no pudo procesarse.";
            }

            return ResponseEntity.status(excepcion.getStatusCode())
                    .body(new ErrorResponse(mensaje));
        }

        log.error(
                "Se produjo una excepción con estado HTTP {}.",
                excepcion.getStatusCode().value(),
                excepcion);

        return respuestaErrorInterno();
    }

    /**
     * Maneja errores de validación de los argumentos de los métodos.
     *
     * Incluye validaciones realizadas mediante anotaciones como
     * @Valid, @NotNull, @NotBlank y otras restricciones de Jakarta
     * Validation, según las validaciones declaradas en los modelos.
     *
     * Se devuelve un mensaje general para mantener una respuesta
     * uniforme sin exponer información interna del servidor.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> manejarValidacion(
            Exception excepcion) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(MENSAJE_SOLICITUD_INVALIDA));
    }

    /**
     * Maneja cuerpos JSON incorrectos o incompatibles con el modelo
     * esperado por el endpoint.
     *
     * Por ejemplo, una fecha con formato inválido o un cuerpo que no
     * puede convertirse al objeto de solicitud correspondiente.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(
            HttpMessageNotReadableException excepcion) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "El cuerpo de la solicitud tiene un formato inválido."));
    }

    /**
     * Maneja solicitudes que omiten parámetros obligatorios.
     *
     * Se aplica a parámetros de la solicitud que Spring esperaba
     * recibir y que no fueron proporcionados.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(
            MissingServletRequestParameterException excepcion) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Falta uno o más parámetros obligatorios de la solicitud."));
    }

    /**
     * Maneja parámetros que no pueden convertirse al tipo esperado.
     *
     * Por ejemplo, cuando un endpoint espera un número entero y recibe
     * un texto que no representa un entero válido.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoParametroInvalido(
            MethodArgumentTypeMismatchException excepcion) {

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "Uno o más parámetros tienen un formato inválido."));
    }

    /**
     * Maneja solicitudes que utilizan un método HTTP no admitido
     * por el endpoint solicitado.
     *
     * Por ejemplo, enviar GET a una ruta que solo admite POST.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException excepcion) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        "El método HTTP utilizado no está permitido para este recurso."));
    }

    /**
     * Maneja solicitudes cuyo Content-Type no es compatible con
     * el contenido que espera el endpoint.
     *
     * Por ejemplo, enviar un cuerpo JSON cuando el endpoint exige
     * otro tipo de contenido.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarTipoContenidoNoAdmitido(
            HttpMediaTypeNotSupportedException excepcion) {

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse(
                        "El tipo de contenido de la solicitud no está admitido."));
    }

    /**
     * Maneja solicitudes cuyo encabezado Accept no permite generar
     * una representación compatible con lo solicitado por el cliente.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> manejarRespuestaNoAceptable(
            HttpMediaTypeNotAcceptableException excepcion) {

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ErrorResponse(
                        "No se puede generar una respuesta en el formato solicitado."));
    }

    /**
     * Maneja errores de acceso a datos.
     *
     * Spring puede envolver en DataAccessException errores relacionados
     * con la conexión, la ejecución de procedimientos almacenados u
     * otras operaciones JDBC.
     *
     * El detalle se registra en el servidor. Al cliente solo se le
     * devuelve el mensaje genérico de error interno.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> manejarErrorBaseDatos(
            DataAccessException excepcion) {

        log.error("Error de acceso a la base de datos.", excepcion);

        return respuestaErrorInterno();
    }

    /**
     * Última protección para excepciones no contempladas por los
     * manejadores específicos.
     *
     * Registrar la excepción permite investigar el problema en los
     * registros del servidor sin enviar su traza al consumidor de la API.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInesperado(
            Exception excepcion) {

        log.error(
                "Error inesperado al procesar la solicitud.",
                excepcion);

        return respuestaErrorInterno();
    }

    /**
     * Construye la respuesta estándar para errores internos.
     *
     * Este método se reutiliza para evitar que diferentes manejadores
     * devuelvan mensajes distintos ante errores del servidor.
     */
    private ResponseEntity<ErrorResponse> respuestaErrorInterno() {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(MENSAJE_ERROR_INTERNO));
    }
}
