package com.duoc.cloudsumativa2.controller;

import com.duoc.cloudsumativa2.model.Usuario;
import com.duoc.cloudsumativa2.service.UsuarioService;
import com.duoc.cloudsumativa2.service.UsuarioService.RegistroUsuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody Usuario usuario, JwtAuthenticationToken autenticacion) {
        RegistroUsuario registro = service.crear(usuario, autenticacion);
        Usuario perfil = registro.usuario();
        return registro.creado()
                ? ResponseEntity.created(URI.create("/usuarios/" + perfil.getId())).body(perfil)
                : ResponseEntity.ok(perfil);
    }

    @GetMapping
    public List<Usuario> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        service.exigirTitular(id, service.idCliente(autenticacion));
        return service.buscarPorId(id);
    }
}
