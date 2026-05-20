package com.ufps.tramites.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Firma digital RSA-2048 sobre el contenido de los PDFs.
 *
 * Persistencia del par de claves:
 *   - Si app.firma.clave-privada y app.firma.clave-publica están configuradas
 *     (vía variables de entorno APP_FIRMA_CLAVE_PRIVADA / APP_FIRMA_CLAVE_PUBLICA),
 *     las claves se cargan desde esas propiedades. Las firmas sobreviven reinicios.
 *   - Si no están configuradas, se genera un par nuevo en memoria y se imprime en
 *     el log con instrucciones para persistirlo. Válido para desarrollo local.
 *
 * Para producción (Render): configurar las dos variables de entorno con los
 * valores base64 que aparecen en el log la primera vez que arranca el servidor.
 */
@Service
public class FirmaDigitalService {

    private static final Logger log = LoggerFactory.getLogger(FirmaDigitalService.class);

    @Value("${app.firma.clave-privada:}")
    private String clavePrivadaBase64;

    @Value("${app.firma.clave-publica:}")
    private String clavePublicaBase64;

    private PrivateKey clavePrivada;
    private PublicKey  clavePublica;

    @PostConstruct
    public void init() {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            if (!clavePrivadaBase64.isBlank() && !clavePublicaBase64.isBlank()) {
                byte[] privBytes = Base64.getDecoder().decode(clavePrivadaBase64.trim());
                byte[] pubBytes  = Base64.getDecoder().decode(clavePublicaBase64.trim());
                clavePrivada = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                clavePublica = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
                log.info("[FIRMA] Par RSA-2048 cargado desde propiedades — firmas persisten entre reinicios.");
            } else {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048, new SecureRandom());
                KeyPair par = gen.generateKeyPair();
                clavePrivada = par.getPrivate();
                clavePublica = par.getPublic();

                String privB64 = Base64.getEncoder().encodeToString(clavePrivada.getEncoded());
                String pubB64  = Base64.getEncoder().encodeToString(clavePublica.getEncoded());

                log.warn("[FIRMA] *** Par RSA generado en memoria — las firmas se invalidan al reiniciar ***");
                log.warn("[FIRMA] Para producción, define estas variables de entorno en Render:");
                log.warn("[FIRMA] APP_FIRMA_CLAVE_PRIVADA={}", privB64);
                log.warn("[FIRMA] APP_FIRMA_CLAVE_PUBLICA={}", pubB64);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar FirmaDigitalService", e);
        }
    }

    /**
     * Firma los bytes del PDF con SHA256withRSA.
     * @return firma en Base64, o null si falla.
     */
    public String firmar(byte[] pdfBytes) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(clavePrivada);
            sig.update(pdfBytes);
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            log.error("[FIRMA] Error al firmar PDF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica que firmaBase64 corresponde al contenido actual de pdfBytes.
     * Retorna false si la firma es nula, malformada o no coincide.
     */
    public boolean verificar(byte[] pdfBytes, String firmaBase64) {
        if (firmaBase64 == null || firmaBase64.isBlank()) return false;
        try {
            byte[] firmaBytes = Base64.getDecoder().decode(firmaBase64);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clavePublica);
            sig.update(pdfBytes);
            return sig.verify(firmaBytes);
        } catch (Exception e) {
            log.warn("[FIRMA] Error al verificar firma: {}", e.getMessage());
            return false;
        }
    }

    /** Clave pública en Base64 (DER/X.509) — se incluye en la respuesta de verificación. */
    public String getClavePublicaBase64() {
        return Base64.getEncoder().encodeToString(clavePublica.getEncoded());
    }
}
