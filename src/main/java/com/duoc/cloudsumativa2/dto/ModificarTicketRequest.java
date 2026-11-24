package com.duoc.cloudsumativa2.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModificarTicketRequest(@NotNull @Size(max = 500) String observacion) {
}
