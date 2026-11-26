package com.duoc.cloudsumativa2.service;

import com.duoc.cloudsumativa2.model.Usuario;
import com.duoc.cloudsumativa2.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public RegistroUsuario crear(Usuario usuario, JwtAuthenticationToken autenticacion) {
        String identidad = esAdmin(autenticacion) ? null : identidadB2c(autenticacion.getToken());
        if (identidad != null) {
            Usuario existente = repository.findByIdentidadB2c(identidad).orElse(null);
            if (existente != null) {
                return new RegistroUsuario(existente, false);
            }
        }

        String correo = usuario.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(usuario.getNombre());
        nuevo.setCorreo(correo);
        nuevo.vincularIdentidadB2c(identidad);
        try {
            return new RegistroUsuario(repository.saveAndFlush(nuevo), true);
        } catch (DataIntegrityViolationException ex) {
            if (identidad == null) {
                throw ex;
            }
            return repository.findByIdentidadB2c(identidad)
                    .map(existente -> new RegistroUsuario(existente, false))
                    .orElseThrow(() -> ex);
        }
    }

    public Long idCliente(JwtAuthenticationToken autenticacion) {
        if (esAdmin(autenticacion)) {
            return null;
        }
        return repository.findByIdentidadB2c(identidadB2c(autenticacion.getToken()))
                .map(Usuario::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Registra tu perfil con POST /usuarios"));
    }

    public void exigirTitular(Long usuarioId, Long idCliente) {
        if (idCliente != null && !idCliente.equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La operación corresponde a otro usuario");
        }
    }

    private boolean esAdmin(JwtAuthenticationToken autenticacion) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String identidadB2c(Jwt token) {
        String sujeto = token.getSubject();
        if (token.getIssuer() == null || sujeto == null || sujeto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El token no identifica a un usuario");
        }
        // Una huella conserva la comparación exacta y permite un índice único corto en MySQL.
        String identidad = token.getIssuer() + "\n" + sujeto;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identidad.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    public List<Usuario> listar() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public record RegistroUsuario(Usuario usuario, boolean creado) {
    }
}
