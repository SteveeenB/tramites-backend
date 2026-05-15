package com.ufps.tramites.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ufps.tramites.model.SolicitudCertificado;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.SolicitudCertificadoRepository;
import com.ufps.tramites.repository.UsuarioRepository;

@Service
public class CertificadoService {

    private static final double COSTO_CERTIFICADO = 50_000.0;

    private static final List<String> TIPOS_VALIDOS = List.of(
    "CONSTANCIA_REGISTRO_CALIFICADO",
    "CONSTANCIA_MATRICULA",
    "CONSTANCIA_BUENA_CONDUCTA"
);

    // Modalidades válidas de envío
    private static final List<String> MODALIDADES_VALIDAS = List.of(
    "FISICA", "DIGITAL"
    );

    @Autowired
    private SolicitudCertificadoRepository certificadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Map<String, Object> solicitarCertificado(Usuario estudiante,
                                                 String tipoCertificado,
                                                 String modalidadEnvio,
                                                 String destinatario) {
    // 1. Validar tipo
    if (!TIPOS_VALIDOS.contains(tipoCertificado)) {
        throw new IllegalStateException(
            "Tipo de certificado inválido: " + tipoCertificado
        );
    }

    // 2. Validar modalidad
    if (!MODALIDADES_VALIDAS.contains(modalidadEnvio)) {
        throw new IllegalStateException(
            "Modalidad de envío inválida: " + modalidadEnvio
        );
    }

    // 3. Validar duplicado — no puede tener una solicitud activa del mismo tipo
List<SolicitudCertificado> existentes = certificadoRepository
    .findByCedulaAndTipoCertificado(estudiante.getCedula(), tipoCertificado);
boolean tieneVigente = existentes.stream()
    .anyMatch(s -> "PENDIENTE_PAGO".equals(s.getEstado()));
if (tieneVigente) {
    throw new IllegalStateException(
        "Ya tienes una solicitud vigente de este tipo de certificado. " +
        "Debes pagar o cancelar la solicitud existente antes de generar una nueva."
    );
}

    // 4. Validar créditos
    if (estudiante.getCreditosAprobados() == null || estudiante.getCreditosAprobados() == 0) {
        throw new IllegalStateException(
            "No tiene créditos aprobados registrados para solicitar un certificado."
        );
    }

    // 5. Crear solicitud
    SolicitudCertificado solicitud = new SolicitudCertificado();
    solicitud.setCedula(estudiante.getCedula());
    solicitud.setTipoCertificado(tipoCertificado);
    solicitud.setModalidadEnvio(modalidadEnvio);
    solicitud.setEstado("PENDIENTE_PAGO");
    solicitud.setFechaSolicitud(LocalDate.now());
    solicitud.setCosto(COSTO_CERTIFICADO);
    solicitud.setDestinatario(destinatario);
    solicitud.setObservaciones("Solicitud de certificado registrada por el sistema.");

    certificadoRepository.save(solicitud);
    return construirRespuesta(solicitud, estudiante);
}

    public List<Map<String, Object>> obtenerCertificadosPorCedula(String cedula) {
        List<SolicitudCertificado> solicitudes = certificadoRepository.findByCedula(cedula);
        List<Map<String, Object>> resultado = new ArrayList<>();
        Usuario estudiante = usuarioRepository.findById(cedula).orElse(null);
        for (SolicitudCertificado s : solicitudes) {
            resultado.add(construirRespuesta(s, estudiante));
        }
        return resultado;
    }

    private Map<String, Object> construirRespuesta(SolicitudCertificado s, Usuario estudiante) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("tipoCertificado", s.getTipoCertificado());
        map.put("modalidadEnvio", s.getModalidadEnvio());
        map.put("estado", s.getEstado());
        map.put("fechaSolicitud", s.getFechaSolicitud() != null
                ? s.getFechaSolicitud().toString() : null);
        map.put("costo", s.getCosto());
        map.put("destinatario", s.getDestinatario());
        map.put("observaciones", s.getObservaciones());
        map.put("liquidacion", construirLiquidacion(s));

        if (estudiante != null) {
            Map<String, Object> est = new LinkedHashMap<>();
            est.put("nombre", estudiante.getNombre());
            est.put("cedula", estudiante.getCedula());
            est.put("programa", estudiante.getProgramaAcademico() != null
                    ? estudiante.getProgramaAcademico().getNombre() : null);
            map.put("estudiante", est);
        }
        return map;
    }

    private Map<String, Object> construirLiquidacion(SolicitudCertificado s) {
        Map<String, Object> liq = new LinkedHashMap<>();
        liq.put("concepto", "Certificado Académico — " + s.getTipoCertificado());
        liq.put("valor", s.getCosto());
        liq.put("fechaLimite", s.getFechaSolicitud() != null
                ? s.getFechaSolicitud().plusDays(3).toString() : null);
        liq.put("instrucciones",
                "Realiza el pago en la ventanilla de Tesorería o por PSE antes de la fecha límite.");
        return liq;
    }

    public Map<String, Object> simularPago(Long id, String cedula) {
    SolicitudCertificado solicitud = certificadoRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));

    // Validar que la solicitud pertenece al estudiante
    if (!cedula.equals(solicitud.getCedula())) {
        throw new IllegalStateException("Esta solicitud no pertenece al estudiante.");
    }

    // Validar que esté en estado correcto
    if (!"PENDIENTE_PAGO".equals(solicitud.getEstado())) {
        throw new IllegalStateException(
            "Esta solicitud no está pendiente de pago. Estado actual: " + solicitud.getEstado()
        );
    }

    // Simular pago
    solicitud.setEstado("PAGADO");
    solicitud.setObservaciones("Pago simulado registrado por el sistema.");
    certificadoRepository.save(solicitud);

    Usuario estudiante = usuarioRepository.findById(cedula).orElse(null);
    return construirRespuesta(solicitud, estudiante);
}

    
}