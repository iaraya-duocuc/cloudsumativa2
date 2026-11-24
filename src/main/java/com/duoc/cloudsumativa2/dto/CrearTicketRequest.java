package com.duoc.cloudsumativa2.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearTicketRequest(@NotNull @Positive Long usuarioId) {
}
