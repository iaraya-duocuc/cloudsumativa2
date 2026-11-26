package com.duoc.cloudsumativa2.repository;

import com.duoc.cloudsumativa2.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByIdentidadB2c(String identidadB2c);
}
