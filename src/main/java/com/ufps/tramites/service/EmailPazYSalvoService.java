package com.ufps.tramites.service;

import com.ufps.tramites.model.PazYSalvo;
import com.ufps.tramites.model.Usuario;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envía correos automáticos a las dependencias cuando se aprueba
 * la solicitud de GRADO de un estudiante.
 */
@Service
public class EmailPazYSalvoService {

    private static final Logger log = LoggerFactory.getLogger(EmailPazYSalvoService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.username:sin-configurar}")
    private String remitente;

    /**
     * Envía un correo a cada dependencia con su link de carga único.
     * Si el SMTP no está configurado, imprime el contenido en consola.
     *
     * @param pazYSalvos lista de registros (uno por dependencia)
     * @param estudiante datos del estudiante cuyo GRADO fue aprobado
     */
    public void notificarDependencias(List<PazYSalvo> pazYSalvos, Usuario estudiante) {
        for (PazYSalvo pys : pazYSalvos) {
            // El Director no recibe correo — sube desde su vista autenticada
            if ("DIRECTOR".equals(pys.getDependencia())) continue;

            String correoDestino = resolverCorreoDependencia(pys.getDependencia());
            if (correoDestino == null) {
                log.warn("No hay correo configurado para la dependencia: {}", pys.getDependencia());
                continue;
            }

            String linkCarga = frontendUrl + "/paz-y-salvo/subir?token=" + pys.getToken();
            String asunto    = construirAsunto(pys, estudiante);
            String cuerpo    = construirCuerpo(pys, estudiante, linkCarga);

            enviar(correoDestino, asunto, cuerpo);
        }
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(remitente);
                msg.setTo(destino);
                msg.setSubject(asunto);
                msg.setText(cuerpo);
                mailSender.send(msg);
                log.info("✅ Correo Paz y Salvo enviado a: {}", destino);
            } catch (Exception e) {
                log.error("❌ Fallo al enviar correo a {}: {}", destino, e.getMessage());
                // Fallback: imprimir en consola para no perder el link
                log.info("=== [FALLBACK CORREO] ===\nPara: {}\nAsunto: {}\n{}\n========================",
                        destino, asunto, cuerpo);
            }
        } else {
            // Modo simulación (SMTP no configurado)
            log.info("=== [SIMULACIÓN CORREO PAZ Y SALVO] ===\nPara: {}\nAsunto: {}\n{}\n========================================",
                    destino, asunto, cuerpo);
        }
    }

    private String construirAsunto(PazYSalvo pys, Usuario estudiante) {
        return "[UFPS Posgrados] Solicitud de Paz y Salvo — " + estudiante.getNombre();
    }

    private String construirCuerpo(PazYSalvo pys, Usuario estudiante, String link) {
        return "Estimado/a equipo de " + pys.getNombreDependencia() + ",\n\n"
            + "Por medio de este correo le informamos que el/la estudiante:\n\n"
            + "  Nombre:          " + estudiante.getNombre() + "\n"
            + "  Cédula:          " + estudiante.getCedula() + "\n"
            + "  Programa:        " + (estudiante.getProgramaAcademico() != null
                                        ? estudiante.getProgramaAcademico().getNombre() : "—") + "\n\n"
            + "Ha completado el proceso de pago de Derechos de Grado y requiere\n"
            + "el Paz y Salvo de su dependencia para continuar con el trámite de grado.\n\n"
            + "Por favor, cargue el documento de Paz y Salvo haciendo clic en el siguiente enlace:\n\n"
            + "  " + link + "\n\n"
            + "El enlace es de uso único para esta solicitud. Si tiene algún inconveniente,\n"
            + "comuníquese con la Dirección de Posgrados.\n\n"
            + "Atentamente,\n"
            + "Sistema de Trámites de Posgrado\n"
            + "Universidad Francisco de Paula Santander";
    }

    /**
     * Resuelve el correo de la dependencia leyendo las propiedades
     * inyectadas en application.properties.
     */
    @Value("${app.dependencias.biblioteca.correo}")
    private String correoBlioteca;

    @Value("${app.dependencias.tesoreria.correo}")
    private String correoTesoreria;

    @Value("${app.dependencias.bienestar.correo}")
    private String correoBienestar;

    private String resolverCorreoDependencia(String dependencia) {
        return switch (dependencia) {
            case "BIBLIOTECA" -> correoBlioteca;
            case "TESORERIA"  -> correoTesoreria;
            case "BIENESTAR"  -> correoBienestar;
            default           -> null;
        };
    }
}
