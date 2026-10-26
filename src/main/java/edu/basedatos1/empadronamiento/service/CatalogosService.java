
package edu.basedatos1.empadronamiento.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import edu.basedatos1.empadronamiento.repository.PadronRepository;
import edu.basedatos1.empadronamiento_api.model.CatalogoItem;
import edu.basedatos1.empadronamiento_api.model.CentroItem;
import edu.basedatos1.empadronamiento_api.model.MesaItem;

/**
 * Servicio encargado de consultar y preparar los catálogos
 * utilizados por la API de empadronamiento.
 *
 * Recibe los resultados del repositorio y los transforma
 * en los modelos definidos en la especificación OpenAPI.
 */
@Service
public class CatalogosService {

    /**
     * Repositorio que obtiene los datos desde SQL Server.
     */
    private final PadronRepository padronRepository;

    /**
     * Construye el servicio e inyecta el repositorio.
     *
     * @param padronRepository acceso a los datos del padrón electoral.
     */
    public CatalogosService(PadronRepository padronRepository) {
        this.padronRepository = padronRepository;
    }

    /**
     * Obtiene el catálogo de municipios.
     *
     * @return lista de municipios con su identificador y nombre.
     */
    public List<CatalogoItem> listarMunicipios() {
        return padronRepository.listarMunicipios()
                .stream()
                .map(fila -> crearCatalogo(
                        fila, "ID_Municipio", "Nombre"))
                .toList();
    }

    /**
     * Obtiene el catálogo de géneros.
     *
     * @return lista de géneros con su identificador y nombre.
     */
    public List<CatalogoItem> listarGeneros() {
        return padronRepository.listarGeneros()
                .stream()
                .map(fila -> crearCatalogo(
                        fila, "ID_Genero", "Nombre"))
                .toList();
    }

    /**
     * Obtiene el catálogo de afiliaciones.
     *
     * @return lista de afiliaciones con su identificador y descripción.
     */
    public List<CatalogoItem> listarAfiliaciones() {
        return padronRepository.listarAfiliaciones()
                .stream()
                .map(fila -> crearCatalogo(
                        fila, "ID_Afiliacion", "Situacion"))
                .toList();
    }

    /**
     * Obtiene el catálogo de estados.
     *
     * @return lista de estados con su identificador y descripción.
     */
    public List<CatalogoItem> listarEstados() {
        return padronRepository.listarEstados()
                .stream()
                .map(fila -> crearCatalogo(
                        fila, "ID_Estado", "Descripcion"))
                .toList();
    }

    /**
     * Obtiene los centros de votación pertenecientes a un municipio.
     *
     * @param municipioId identificador del municipio consultado.
     * @return centros con su identificador, nombre y dirección.
     */
    public List<CentroItem> listarCentros(int municipioId) {
        return padronRepository.listarCentros(municipioId)
                .stream()
                .map(fila -> {

                    CentroItem centro = new CentroItem();

                    centro.setId(entero(fila, "ID_Centro"));
                    centro.setNombre(texto(fila, "Nombre"));
                    centro.setDireccion(texto(fila, "Direccion"));

                    return centro;
                })
                .toList();
    }

    /**
     * Obtiene las mesas de votación de un centro determinado.
     *
     * @param centroId identificador del centro de votación.
     * @return lista de mesas con su identificador y número.
     */
    public List<MesaItem> listarMesas(int centroId) {
        return padronRepository.listarMesas(centroId)
                .stream()
                .map(fila -> {

                    MesaItem mesa = new MesaItem();

                    mesa.setId(entero(fila, "ID_Mesa"));
                    mesa.setNumero(entero(fila, "Numero_Mesa"));

                    return mesa;
                })
                .toList();
    }

    /**
     * Convierte una fila de resultados SQL en un elemento de catálogo.
     *
     * @param fila mapa con las columnas devueltas por SQL Server.
     * @param columnaId nombre de la columna que contiene el identificador.
     * @param columnaNombre nombre de la columna que contiene la descripción.
     * @return elemento de catálogo preparado para la respuesta de la API.
     */
    private CatalogoItem crearCatalogo(
            Map<String, Object> fila,
            String columnaId,
            String columnaNombre) {

        CatalogoItem item = new CatalogoItem();

        item.setId(entero(fila, columnaId));
        item.setNombre(texto(fila, columnaNombre));

        return item;
    }

    /**
     * Obtiene una columna y la convierte a Integer.
     *
     * Acepta valores numéricos devueltos por JDBC y valores
     * que puedan convertirse a partir de su representación textual.
     *
     * @param fila fila de resultados de la consulta.
     * @param columna nombre de la columna requerida.
     * @return valor entero de la columna.
     * @throws IllegalStateException si la columna contiene null.
     * @throws NumberFormatException si el valor no representa un entero.
     */
    private Integer entero(
            Map<String, Object> fila,
            String columna) {

        Object valor = fila.get(columna);

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        if (valor == null) {
            throw new IllegalStateException(
                    "La columna " + columna + " no puede ser nula.");
        }

        return Integer.valueOf(valor.toString());
    }

    /**
     * Obtiene una columna y la convierte a String.
     *
     * @param fila fila de resultados de la consulta.
     * @param columna nombre de la columna requerida.
     * @return valor textual o null si la columna es nula.
     */
    private String texto(
            Map<String, Object> fila,
            String columna) {

        Object valor = fila.get(columna);

        return valor == null ? null : valor.toString();
    }
}
