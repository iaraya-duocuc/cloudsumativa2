package com.duoc.cloudsumativa2.controller;

import com.duoc.cloudsumativa2.model.Asset;
import com.duoc.cloudsumativa2.service.TicketService;
import com.duoc.cloudsumativa2.service.UsuarioService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/tickets/{id}/s3")
public class AwsController {
    private final TicketService tickets;

    private final UsuarioService usuarios;

    public AwsController(TicketService tickets, UsuarioService usuarios) {
        this.tickets = tickets;
        this.usuarios = usuarios;
    }

    @PostMapping
    public Asset subir(@PathVariable Long id) {
        return tickets.subirS3(id);
    }

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, JwtAuthenticationToken autenticacion) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("ticket" + id + ".txt").build().toString())
                .body(tickets.descargarS3(id, usuarios.idCliente(autenticacion)));
    }
}
