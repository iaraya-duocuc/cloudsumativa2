package com.duoc.cloudsumativa2.controller;

import com.duoc.cloudsumativa2.dto.CrearTicketRequest;
import com.duoc.cloudsumativa2.dto.ModificarTicketRequest;
import com.duoc.cloudsumativa2.dto.TicketResponse;
import com.duoc.cloudsumativa2.service.TicketService;
import com.duoc.cloudsumativa2.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.nio.charset.StandardCharsets;

@RestController
public class TicketController {

    private final TicketService service;

    private final UsuarioService usuarios;

    public TicketController(TicketService service, UsuarioService usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @PostMapping("/eventos/{eventoId}/tickets")
    public ResponseEntity<TicketResponse> crear(@PathVariable Long eventoId,
                                               @Valid @RequestBody CrearTicketRequest request,
                                               JwtAuthenticationToken autenticacion) {
        TicketResponse ticket = service.crear(eventoId, request.usuarioId(), usuarios.idCliente(autenticacion));
        return ResponseEntity.created(URI.create("/tickets/" + ticket.id())).body(ticket);
    }

    @GetMapping("/tickets/{id}")
    public TicketResponse consultar(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        return service.consultar(id, usuarios.idCliente(autenticacion));
    }

    @GetMapping("/tickets/{id}/archivo")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("ticket" + id + ".txt").build().toString())
                .body(service.descargar(id, usuarios.idCliente(autenticacion)));
    }
    @GetMapping("/usuarios/{id}/tickets")
    public List<TicketResponse> listarPorUsuario(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        return service.listarPorUsuario(id, usuarios.idCliente(autenticacion));
    }

    @GetMapping("/eventos/{id}/tickets")
    public List<TicketResponse> listarPorEvento(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        return service.listarPorEvento(id, usuarios.idCliente(autenticacion));
    }

    @PatchMapping("/tickets/{id}")
    public TicketResponse modificar(@PathVariable Long id,
                                   @Valid @RequestBody ModificarTicketRequest request,
                                   JwtAuthenticationToken autenticacion) {
        return service.modificar(id, request.observacion(), usuarios.idCliente(autenticacion));
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        service.eliminar(id, usuarios.idCliente(autenticacion));
        return ResponseEntity.noContent().build();
    }
}
