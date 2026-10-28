
package edu.basedatos1.empadronamiento.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import edu.basedatos1.empadronamiento.service.CatalogosService;
import edu.basedatos1.empadronamiento_api.api.CatalogosApi;
import edu.basedatos1.empadronamiento_api.model.CatalogoItem;
import edu.basedatos1.empadronamiento_api.model.CentroItem;
import edu.basedatos1.empadronamiento_api.model.MesaItem;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Controlador REST encargado de exponer los catálogos
 * necesarios para registrar un ciudadano.
 *
 * Implementa CatalogosApi, por lo que utiliza los endpoints
 * definidos en la especificación de la API.
 *
 * La consulta y transformación de los datos se delega
 * a CatalogosService.
 */
@CrossOrigin(origins = "*")
@RestController
public class CatalogosController implements CatalogosApi {

    /*
     * Servicio que contiene la lógica para obtener los catálogos.
     */
    private final CatalogosService catalogosService;

    /**
     * Constructor utilizado por Spring para inyectar el servicio.
     *
     * @param catalogosService servicio de catálogos
     */
    public CatalogosController(CatalogosService catalogosService) {
        this.catalogosService = catalogosService;
    }

    /**
     * Devuelve los municipios disponibles.
     */
    @Override
    public ResponseEntity<List<CatalogoItem>> listarMunicipios() {
        return ResponseEntity.ok(
                catalogosService.listarMunicipios());
    }

    /**
     * Devuelve los géneros disponibles.
     */
    @Override
    public ResponseEntity<List<CatalogoItem>> listarGeneros() {
        return ResponseEntity.ok(
                catalogosService.listarGeneros());
    }

    /**
     * Devuelve las situaciones de afiliación disponibles.
     */
    @Override
    public ResponseEntity<List<CatalogoItem>> listarAfiliaciones() {
        return ResponseEntity.ok(
                catalogosService.listarAfiliaciones());
    }

    /**
     * Devuelve los estados posibles del padrón.
     */
    @Override
    public ResponseEntity<List<CatalogoItem>> listarEstados() {
        return ResponseEntity.ok(
                catalogosService.listarEstados());
    }

    /**
     * Devuelve los centros de votación de un municipio.
     *
     * @param municipioId identificador del municipio seleccionado
     */
    @Override
    public ResponseEntity<List<CentroItem>> listarCentros(
            Integer municipioId) {

        return ResponseEntity.ok(
                catalogosService.listarCentros(municipioId));
    }

    /**
     * Devuelve las mesas electorales de un centro de votación.
     *
     * @param centroId identificador del centro seleccionado
     */
    @Override
    public ResponseEntity<List<MesaItem>> listarMesas(
            Integer centroId) {

        return ResponseEntity.ok(
                catalogosService.listarMesas(centroId));
    }
}
