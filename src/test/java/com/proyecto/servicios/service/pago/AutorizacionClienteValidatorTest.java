package com.proyecto.servicios.service.pago;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.UnauthorizedAccessException;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizacionClienteValidator: identidad, titularidad y PIN")
class AutorizacionClienteValidatorTest {

    private static final String CORREO = "ana@correo.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AutorizacionClienteValidator validator;
    private Cliente cliente;
    private Usuario usuario;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        validator = new AutorizacionClienteValidator(usuarioRepository, cuentaRepository, passwordEncoder);
        cliente = Cliente.builder().id(10L).activo(true).bloqueado(false).build();
        usuario = Usuario.builder().id(5L).correo(CORREO).password("hash").activo(true).cliente(cliente).build();
        cuenta = Cuenta.builder().id(1L).numeroCuenta("0000000001").cliente(cliente).build();
    }

    @Test
    @DisplayName("Titular con PIN correcto: autorizado")
    void autorizado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        assertSame(cuenta, validator.autorizar(CORREO, "0000000001", "1234"));
    }

    @Test
    @DisplayName("PIN incorrecto -> 401 y sin fugas")
    void pinIncorrecto() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        when(passwordEncoder.matches("mal", "hash")).thenReturn(false);
        assertThrows(UnauthorizedAccessException.class, () -> validator.autorizar(CORREO, "0000000001", "mal"));
    }

    @Test
    @DisplayName("Cuenta de otro cliente (IDOR) -> acceso denegado, sin evaluar el PIN")
    void cuentaAjena() {
        Cliente otro = Cliente.builder().id(99L).build();
        cuenta.setCliente(otro);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", "1234"));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Cuenta inexistente y cuenta ajena producen exactamente el mismo mensaje")
    void noRevelaExistencia() {
        Cliente otro = Cliente.builder().id(99L).build();
        cuenta.setCliente(otro);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        String ajena = assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", null)).getMessage();
        String inexistente = assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "9999999999", null)).getMessage();
        assertEquals(ajena, inexistente);
    }

    @Test
    @DisplayName("Usuario inactivo, cliente inactivo o bloqueado, o sin cliente: denegado")
    void usuarioNoHabilitado() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));

        usuario.setActivo(false);
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", "1234"));
        usuario.setActivo(true);

        cliente.setBloqueado(true);
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", "1234"));
        cliente.setBloqueado(false);

        cliente.setActivo(false);
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", "1234"));
        cliente.setActivo(true);

        usuario.setCliente(null);
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(CORREO, "0000000001", "1234"));
    }

    @Test
    @DisplayName("Sin identidad autenticada o usuario desconocido: denegado")
    void sinIdentidad() {
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(null, "0000000001", "1234"));
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar(" ", "0000000001", "1234"));
        when(usuarioRepository.findByCorreo("x@y.com")).thenReturn(Optional.empty());
        assertThrows(AccesoDenegadoException.class, () -> validator.autorizar("x@y.com", "0000000001", "1234"));
    }

    @Test
    @DisplayName("Consulta (sin PIN): valida titularidad pero no pide PIN")
    void consultaSinPin() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("0000000001")).thenReturn(Optional.of(cuenta));
        assertSame(cuenta, validator.autorizar(CORREO, "0000000001", null));
        verifyNoInteractions(passwordEncoder);
    }
}