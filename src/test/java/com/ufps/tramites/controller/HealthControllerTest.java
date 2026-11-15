package com.ufps.tramites.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    @Test
    void health_devuelve200ConStatusUp() {
        ResponseEntity<Map<String, String>> respuesta = new HealthController().health();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsEntry("status", "UP");
    }
}
