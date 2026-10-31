package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class GestoPagoCryptoUtil {

    private final String secretKeyBase64;
    private final String ivBase64;

    public GestoPagoCryptoUtil(
            @Value("${gestopago.crypto.secret-key}") String secretKeyBase64,
            @Value("${gestopago.crypto.iv}") String ivBase64) {
        this.secretKeyBase64 = secretKeyBase64;
        this.ivBase64 = ivBase64;
    }

    public String encrypt(String plainText) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Error al cifrar payload con AES: {}", e.getMessage(), e);
            throw new RuntimeException("Error en el cifrado de la transaccion: " + e.getMessage(), e);
        }
    }

    public String decrypt(String cipherTextBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherTextBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar con AES: {}", e.getMessage(), e);
            throw new RuntimeException("Error en el descifrado: " + e.getMessage(), e);
        }
    }
}
