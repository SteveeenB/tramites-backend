package com.ufps.tramites.service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        restClient = RestClient.builder().build();
    }

    /**
     * Sube bytes al bucket. Si el objeto ya existe lo sobreescribe (x-upsert: true).
     * @param path ruta dentro del bucket, ej. "16/acta-grado-16.pdf"
     */
    public void subir(String path, byte[] bytes, String contentType) {
        String tipo = (contentType != null && !contentType.isBlank())
                ? contentType : "application/octet-stream";

        restClient.post()
                .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(tipo))
                .body(bytes)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Descarga un objeto del bucket. Devuelve null si no existe o hay error.
     */
    public byte[] descargar(String path) {
        try {
            return restClient.get()
                    .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Genera una URL firmada con expiración de 1 hora.
     * Funciona tanto con buckets públicos como privados.
     * Si la firma falla por cualquier motivo, cae al URL público como fallback.
     */
    @SuppressWarnings("unchecked")
    public String obtenerUrl(String path) {
        try {
            Map<String, Object> resp = restClient.post()
                    .uri(supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + path)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", 3600))
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("signedURL") != null) {
                return supabaseUrl + resp.get("signedURL");
            }
        } catch (Exception ignored) {}

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
