package com.proyecto.servicios.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Generador y validador de códigos TOTP (RFC 6238) compatible con Google Authenticator.
 * No requiere librerías externas pesadas: usa criptografía estándar HMAC-SHA1 y codificación Base32.
 */
public class GoogleAuthenticatorUtil {

    private static final String ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /**
     * Genera una clave secreta aleatoria en Base32 para el usuario (16 caracteres / 80 bits).
     */
    public static String generarClaveSecreta() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[10];
        random.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Construye la URL estándar "otpauth://" que Google Authenticator lee desde un código QR.
     */
    public static String generarOtpAuthUrl(String cuenta, String claveSecreta, String emisor) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                emisor, cuenta, claveSecreta, emisor, DIGITS, TIME_STEP_SECONDS);
    }

    /**
     * Valida si el código de 6 dígitos ingresado coincide con el tiempo actual (con ventana de ±1 paso).
     */
    public static boolean validarCodigo(String claveSecreta, String codigoIngresado) {
        if (claveSecreta == null || codigoIngresado == null || codigoIngresado.trim().length() != DIGITS) {
            return false;
        }

        try {
            int code = Integer.parseInt(codigoIngresado.trim());
            long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

            // Ventana de tolerancia: -1 (hace 30s), 0 (actual), +1 (próximos 30s) por desajustes de reloj
            for (int i = -1; i <= 1; i++) {
                long hash = calculateCode(decodeBase32(claveSecreta), currentWindow + i);
                if (hash == code) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static int calculateCode(byte[] key, long time) throws GeneralSecurityException {
        byte[] data = ByteBuffer.allocate(8).putLong(time).array();
        SecretKeySpec signKey = new SecretKeySpec(key, ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= (long) Math.pow(10, DIGITS);

        return (int) truncatedHash;
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(BASE32_CHARS.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_CHARS.charAt(index));
        }
        return sb.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return Arrays.copyOf(result, count);
    }
}

