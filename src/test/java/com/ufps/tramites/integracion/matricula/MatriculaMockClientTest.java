package com.ufps.tramites.integracion.matricula;

import org.junit.jupiter.api.Test;

import com.ufps.tramites.integracion.matricula.ConsultaMatricula.Resultado;

import static org.assertj.core.api.Assertions.assertThat;

// TP-146: el mock debe cubrir los mismos caminos que el adaptador real para
// que TP-147 y TP-148 puedan avanzar sin el servicio de Matrícula.
class MatriculaMockClientTest {

    private final MatriculaMockClient client = new MatriculaMockClient();

    @Test
    void estudiantesDemo_conCreditosDeDataSql() {
        assertThat(client.consultarSituacion("1098765432").situacion().creditosAprobados()).isEqualTo(40);
        assertThat(client.consultarSituacion("1098765435").situacion().creditosAprobados()).isEqualTo(56);
        assertThat(client.consultarSituacion("1098765435").situacion().creditosRequeridos()).isEqualTo(56);
    }

    @Test
    void cedulasEspeciales_simulanLosFlujosAlternos() {
        assertThat(client.consultarSituacion(MatriculaMockClient.CEDULA_SERVICIO_CAIDO).resultado())
                .isEqualTo(Resultado.NO_DISPONIBLE);
        assertThat(client.consultarSituacion(MatriculaMockClient.CEDULA_RESPUESTA_INVALIDA).resultado())
                .isEqualTo(Resultado.RESPUESTA_INVALIDA);
        assertThat(client.consultarSituacion(MatriculaMockClient.CEDULA_SIN_CREDITOS_APROBADOS).situacion()
                .creditosAprobados()).isNull();
    }

    @Test
    void cedulaDesconocida_noEncontrada_yFormatoInvalido_rechazado() {
        assertThat(client.consultarSituacion("1234567890").resultado()).isEqualTo(Resultado.NO_ENCONTRADO);
        assertThat(client.consultarSituacion("abc").resultado()).isEqualTo(Resultado.CEDULA_INVALIDA);
    }
}
