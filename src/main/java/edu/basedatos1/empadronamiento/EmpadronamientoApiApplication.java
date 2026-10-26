
package edu.basedatos1.empadronamiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la API de empadronamiento.
 *
 * Spring Boot utiliza esta clase como punto de entrada
 * para iniciar la aplicación.
 */
@SpringBootApplication
public class EmpadronamientoApiApplication {

    /**
     * Método principal de Java.
     * Se ejecuta cuando iniciamos el proyecto.
     *
     * SpringApplication.run() inicia Spring Boot y prepara
     * los componentes necesarios para ejecutar la API.
     */
    public static void main(String[] args) {
        SpringApplication.run(EmpadronamientoApiApplication.class, args);
    }

}
