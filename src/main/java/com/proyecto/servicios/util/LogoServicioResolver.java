package com.proyecto.servicios.util;

/**
 * Mapeador centralizado de logotipos e iconos oficiales para servicios de GestoPago.
 * Construye la URL oficial del proveedor:
 * https://gestopago.portalventas.net/sistema/images/gestopago/servicios/{idServicio}.png
 */
public final class LogoServicioResolver {

    private static final String GESTOPAGO_IMAGE_BASE_URL = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/";
    private static final String DEFAULT_LOGO = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/79.png";

    private LogoServicioResolver() {
        throw new UnsupportedOperationException();
    }

    /**
     * Resuelve el logotipo oficial del servicio utilizando el idServicio de GestoPago.
     * Ejemplo: idServicio 79 -> https://gestopago.portalventas.net/sistema/images/gestopago/servicios/79.png
     */
    public static String resolverLogo(String idServicio) {
        if (idServicio == null || idServicio.isBlank()) {
            return "/gestopago/logo/79";
        }
        return "/gestopago/logo/" + idServicio.trim();
    }

    /**
     * Sobrecarga de compatibilidad en caso de recibir servicio e idServicio.
     */
    public static String resolverLogo(String idServicio, String nombreServicio) {
        if (idServicio != null && !idServicio.isBlank()) {
            return resolverLogo(idServicio);
        }
        return DEFAULT_LOGO;
    }
}

