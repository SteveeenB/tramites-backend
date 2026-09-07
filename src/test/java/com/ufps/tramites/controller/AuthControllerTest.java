package com.ufps.tramites.controller;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ufps.tramites.dto.LoginResponseDTO;
import com.ufps.tramites.model.Usuario;
import com.ufps.tramites.repository.AdminRepository;
import com.ufps.tramites.repository.EstudianteRepository;
import com.ufps.tramites.repository.RolRepository;
import com.ufps.tramites.repository.UsuarioRepository;
import com.ufps.tramites.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private RolRepository rolRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // @Value no lo resuelve Spring en un test unitario puro
        ReflectionTestUtils.setField(authController, "demoAuthEnabled", false);
    }

    @Test
    void demoAuthDeshabilitado_devuelve404YNoConsultaRepositorios() {
        ResponseEntity<?> respuesta = authController.loginDemo("123456789");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        verify(usuarioRepository, never()).findByCedula(any());
        verify(adminRepository, never()).findByCodigo(any());
        verify(jwtService, never()).generateToken(any(Usuario.class), any());
    }

    @Test
    void demoAuthHabilitado_conCedulaExistente_devuelve200ConToken() {
        ReflectionTestUtils.setField(authController, "demoAuthEnabled", true);

        Usuario usuario = new Usuario();
        usuario.setCedula("123456789");
        usuario.setNombreCompleto("Juan Perez");

        when(usuarioRepository.findByCedula("123456789")).thenReturn(Optional.of(usuario));
        when(estudianteRepository.findByUsuario(usuario)).thenReturn(Optional.empty());
        when(jwtService.generateToken(usuario, Optional.empty())).thenReturn("jwt-demo-token");

        ResponseEntity<?> respuesta = authController.loginDemo("123456789");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseDTO body = (LoginResponseDTO) respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getToken()).isEqualTo("jwt-demo-token");
        assertThat(body.getCedula()).isEqualTo("123456789");

        verify(adminRepository, never()).findByCodigo(any());
    }
}
