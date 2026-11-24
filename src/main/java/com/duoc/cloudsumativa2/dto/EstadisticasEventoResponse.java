package com.duoc.cloudsumativa2.dto;

import java.math.BigDecimal;

public record EstadisticasEventoResponse(Long eventoId, String nombreEvento,
                                         long totalReservas, int cuposDisponibles,
                                         BigDecimal montoTotalReservado) {
}
