
package edu.basedatos1.empadronamiento.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import edu.basedatos1.empadronamiento_api.model.CaptchaResponse;

/**
 * Servicio encargado de generar y validar los CAPTCHA utilizados
 * por la API de consultas de empadronamiento.
 *
 * Cada CAPTCHA tiene un identificador, una respuesta correcta y
 * una fecha de expiración. La respuesta correcta se conserva
 * únicamente en el servidor.
 */
@Service
public class CaptchaService {

    /**
     * Tiempo máximo durante el cual un CAPTCHA permanece válido.
     */
    private static final int EXPIRACION_SEGUNDOS = 120;

    /**
     * Cantidad de caracteres que tendrá cada código CAPTCHA.
     */
    private static final int LONGITUD_CODIGO = 6;

    /**
     * Caracteres permitidos para generar el código.
     *
     * Se excluyen letras y números que pueden confundirse
     * visualmente, como I, O, 0 y 1.
     */
    private static final String CARACTERES =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * Generador de números aleatorios seguro para crear códigos.
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * Almacenamiento temporal de los CAPTCHA activos.
     *
     * La clave es el identificador que recibe el cliente.
     * El valor contiene el código correcto y su vencimiento.
     *
     * ConcurrentHashMap permite acceder al almacenamiento desde
     * varias solicitudes concurrentes de la aplicación.
     */
    private final ConcurrentHashMap<String, CaptchaData> captchas =
            new ConcurrentHashMap<>();

    /**
     * Genera un CAPTCHA nuevo y devuelve sus datos al cliente.
     *
     * La respuesta correcta no se incluye en el objeto enviado
     * al cliente; solamente se entrega el identificador, la imagen
     * codificada en Base64 y el tiempo de expiración.
     *
     * @return información necesaria para mostrar el CAPTCHA.
     */
    public CaptchaResponse generarCaptcha() {

        // Eliminar los CAPTCHA vencidos para evitar que se acumulen
        // indefinidamente en el almacenamiento.
        Instant ahora = Instant.now();

        captchas.entrySet().removeIf(
                entrada -> !entrada.getValue().expiraEn().isAfter(ahora)
        );

        // Crear un identificador único para localizar este CAPTCHA.
        String id = UUID.randomUUID().toString();

        // Generar la respuesta correcta de forma aleatoria.
        String codigo = generarCodigo();

        // Calcular el instante exacto en el que dejará de ser válido.
        Instant expiracion =
                ahora.plusSeconds(EXPIRACION_SEGUNDOS);

        // Guardar la respuesta correcta exclusivamente en el servidor.
        captchas.put(id, new CaptchaData(codigo, expiracion));

        // Crear la imagen y convertirla a Base64 para enviarla por JSON.
        String imagenBase64 = generarImagenBase64(codigo);

        // Construir el objeto de respuesta definido por OpenAPI.
        CaptchaResponse respuesta = new CaptchaResponse();
        respuesta.setId(id);
        respuesta.setImagenBase64(imagenBase64);
        respuesta.setExpiraEnSegundos(EXPIRACION_SEGUNDOS);

        return respuesta;
    }

    /**
     * Comprueba la respuesta proporcionada por el usuario.
     *
     * El CAPTCHA se consume al realizar el intento: no puede
     * reutilizarse, aunque la respuesta sea incorrecta.
     *
     * @param id identificador del CAPTCHA generado anteriormente.
     * @param respuestaUsuario texto introducido por el usuario.
     * @return true si la respuesta es correcta y el CAPTCHA sigue
     *         vigente; false en cualquier otro caso.
     */
    public boolean validarCaptcha(String id, String respuestaUsuario) {

        // Rechazar identificadores vacíos o inexistentes.
        if (id == null || id.isBlank()) {
            return false;
        }

        /*
         * remove obtiene y elimina el CAPTCHA en una sola operación.
         * Esto evita que una misma respuesta se reutilice en intentos
         * posteriores.
         */
        CaptchaData captcha = captchas.remove(id);

        // Si no existe, ya fue utilizado, no se generó o fue eliminado.
        if (captcha == null) {
            return false;
        }

        // Rechazar CAPTCHA que ya hayan expirado.
        if (!captcha.expiraEn().isAfter(Instant.now())) {
            return false;
        }

        // Rechazar respuestas vacías.
        if (respuestaUsuario == null || respuestaUsuario.isBlank()) {
            return false;
        }

        // Comparar sin distinguir entre mayúsculas y minúsculas.
        return captcha.codigo().equalsIgnoreCase(
                respuestaUsuario.trim()
        );
    }

    /**
     * Genera un código aleatorio con la longitud configurada.
     *
     * @return código CAPTCHA de seis caracteres.
     */
    private String generarCodigo() {

        StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);

        for (int i = 0; i < LONGITUD_CODIGO; i++) {

            // Elegir una posición aleatoria dentro del conjunto permitido.
            int indice = random.nextInt(CARACTERES.length());

            // Añadir el carácter elegido al código.
            codigo.append(CARACTERES.charAt(indice));
        }

        return codigo.toString();
    }

    /**
     * Dibuja el código sobre una imagen PNG y la codifica en Base64.
     *
     * @param codigo texto que debe aparecer en la imagen.
     * @return contenido PNG codificado en Base64.
     * @throws IllegalStateException si no es posible generar la imagen.
     */
    private String generarImagenBase64(String codigo) {

        // Dimensiones de la imagen del CAPTCHA.
        int ancho = 220;
        int alto = 80;

        // Crear una imagen RGB sobre la que se dibujará el código.
        BufferedImage imagen = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_INT_RGB
        );

        // Obtener el objeto que permite dibujar sobre la imagen.
        Graphics2D g = imagen.createGraphics();

        try {
            // Suavizar los bordes de las líneas y los caracteres.
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Establecer el fondo de la imagen.
            g.setColor(new Color(245, 245, 245));
            g.fillRect(0, 0, ancho, alto);

            /*
             * Dibujar líneas de distintos colores para dificultar
             * la lectura automática del código.
             */
            for (int i = 0; i < 7; i++) {

                g.setColor(new Color(
                        random.nextInt(180),
                        random.nextInt(180),
                        random.nextInt(180)
                ));

                g.drawLine(
                        random.nextInt(ancho),
                        random.nextInt(alto),
                        random.nextInt(ancho),
                        random.nextInt(alto)
                );
            }

            // Configurar la fuente utilizada para dibujar el código.
            g.setFont(new Font("SansSerif", Font.BOLD, 34));

            // Dibujar cada carácter en una posición ligeramente variable.
            for (int i = 0; i < codigo.length(); i++) {

                g.setColor(new Color(
                        random.nextInt(100),
                        random.nextInt(100),
                        random.nextInt(100)
                ));

                int x = 18 + i * 32;
                int y = 48 + random.nextInt(15) - 7;

                g.drawString(
                        String.valueOf(codigo.charAt(i)),
                        x,
                        y
                );
            }

        } finally {
            // Liberar los recursos gráficos utilizados.
            g.dispose();
        }

        /*
         * Escribir la imagen en memoria como PNG.
         * No se necesita crear un archivo físico en el disco.
         */
        try (ByteArrayOutputStream salida =
                     new ByteArrayOutputStream()) {

            ImageIO.write(imagen, "png", salida);

            // Convertir los bytes de la imagen a texto Base64.
            return Base64.getEncoder()
                    .encodeToString(salida.toByteArray());

        } catch (IOException e) {

            // Informar del fallo sin ocultar su causa original.
            throw new IllegalStateException(
                    "No se pudo generar la imagen del CAPTCHA.",
                    e
            );
        }
    }

    /**
     * Representa los datos internos de un CAPTCHA.
     *
     * El record es inmutable: una vez creado, su código y su
     * fecha de expiración no pueden modificarse.
     *
     * @param codigo respuesta correcta del CAPTCHA.
     * @param expiraEn instante en el que deja de ser válido.
     */
    private record CaptchaData(
            String codigo,
            Instant expiraEn
    ) {
    }
}
