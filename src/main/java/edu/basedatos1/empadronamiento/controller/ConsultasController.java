
package edu.basedatos1.empadronamiento.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import edu.basedatos1.empadronamiento.service.ConsultasService;
import edu.basedatos1.empadronamiento_api.api.ConsultasApi;
import edu.basedatos1.empadronamiento_api.model.ConsultaRequest;
import edu.basedatos1.empadronamiento_api.model.ConsultaResponse;
import jakarta.validation.Valid;

/**
 * Controlador REST encargado de consultar los datos
 * de un ciudadano empadronado.
 *
 * Implementa ConsultasApi para exponer el endpoint
 * definido en la especificación de la API.
 *
 * La lógica de consulta se delega a ConsultasService.
 */
@RestController
public class ConsultasController implements ConsultasApi {

    /*
     * Servicio que contiene la lógica de consulta
     * y validación de los datos recibidos.
     */
    private final ConsultasService consultasService;

    /**
     * Constructor utilizado por Spring para inyectar
     * el servicio de consultas.
     *
     * @param consultasService servicio de consultas
     */
    public ConsultasController(ConsultasService consultasService) {
        this.consultasService = consultasService;
    }

    /**
     * Consulta un empadronamiento utilizando los datos
     * recibidos en el cuerpo de la solicitud HTTP.
     *
     * @param consultaRequest datos necesarios para consultar
     * @return respuesta con el resultado de la consulta
     */
    @Override
    public ResponseEntity<ConsultaResponse> consultarEmpadronamiento(
            @Valid @RequestBody ConsultaRequest consultaRequest) {

        // Delegar la lógica de consulta al servicio.
        ConsultaResponse respuesta =
                consultasService.consultar(consultaRequest);

        // Devolver la respuesta con el estado HTTP 200 OK.
        return ResponseEntity.ok(respuesta);
    }
}
