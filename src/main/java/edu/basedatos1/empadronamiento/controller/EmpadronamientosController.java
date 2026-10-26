
package edu.basedatos1.empadronamiento.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.basedatos1.empadronamiento.service.EmpadronamientosService;
import edu.basedatos1.empadronamiento_api.api.EmpadronamientosApi;
import edu.basedatos1.empadronamiento_api.model.RegistroRequest;
import edu.basedatos1.empadronamiento_api.model.RegistroResponse;
import jakarta.validation.Valid;

/**
 * Controlador REST encargado de registrar nuevos
 * empadronamientos.
 *
 * Implementa EmpadronamientosApi para exponer el endpoint
 * de registro definido en la especificación de la API.
 *
 * La validación y la lógica de negocio se delegan
 * a EmpadronamientosService.
 */
@RestController
public class EmpadronamientosController implements EmpadronamientosApi {

    /*
     * Servicio que valida los datos y coordina
     * el registro del ciudadano en la base de datos.
     */
    private final EmpadronamientosService empadronamientosService;

    /**
     * Constructor utilizado por Spring para inyectar
     * el servicio de empadronamientos.
     *
     * @param empadronamientosService servicio de registro
     */
    public EmpadronamientosController(
            EmpadronamientosService empadronamientosService) {

        this.empadronamientosService = empadronamientosService;
    }

    /**
     * Registra un nuevo empadronamiento.
     *
     * @param registroRequest datos del ciudadano que se registrará
     * @return respuesta con el número de empadronamiento generado
     */
    @Override
    public ResponseEntity<RegistroResponse> registrarEmpadronamiento(
            @Valid @RequestBody RegistroRequest registroRequest) {

        // Delegar la validación y el registro al servicio.
        RegistroResponse respuesta =
                empadronamientosService.registrar(registroRequest);

        /*
         * HTTP 201 CREATED indica que la solicitud
         * creó correctamente un nuevo recurso.
         */
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
