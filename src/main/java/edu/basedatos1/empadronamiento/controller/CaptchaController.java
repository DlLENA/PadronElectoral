
package edu.basedatos1.empadronamiento.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import edu.basedatos1.empadronamiento.service.CaptchaService;
import edu.basedatos1.empadronamiento_api.api.CaptchaApi;
import edu.basedatos1.empadronamiento_api.model.CaptchaResponse;

/**
 * Controlador REST encargado de exponer el servicio de CAPTCHA.
 *
 * Implementa la interfaz generada por OpenAPI para mantener
 * alineados los endpoints de la API con su especificación.
 */
@RestController
public class CaptchaController implements CaptchaApi {

    /**
     * Servicio responsable de generar y administrar los CAPTCHA.
     */
    private final CaptchaService captchaService;

    /**
     * Constructor utilizado por Spring para inyectar el servicio.
     *
     * @param captchaService servicio que genera los CAPTCHA.
     */
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * Atiende la solicitud para obtener un CAPTCHA nuevo.
     *
     * Delega la generación al servicio y devuelve el resultado
     * con el estado HTTP 200 (OK).
     *
     * @return respuesta con el identificador, la imagen Base64
     *         y el tiempo de expiración del CAPTCHA.
     */
    @Override
    public ResponseEntity<CaptchaResponse> obtenerCaptcha() {

        // Generar el CAPTCHA mediante la capa de servicio.
        CaptchaResponse captcha = captchaService.generarCaptcha();

        // Devolver el CAPTCHA al cliente con HTTP 200.
        return ResponseEntity.ok(captcha);
    }
}
