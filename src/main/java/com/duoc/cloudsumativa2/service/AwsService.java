package com.duoc.cloudsumativa2.service;

import com.duoc.cloudsumativa2.model.Asset;
import com.duoc.cloudsumativa2.model.Ticket;

public interface AwsService {
    boolean habilitado();
    Asset subir(Ticket ticket, byte[] contenido);
    byte[] descargar(Ticket ticket);
    byte[] respaldo(Ticket ticket);
    void eliminar(Ticket ticket);
    void restaurar(Ticket ticket, byte[] contenido);
}
