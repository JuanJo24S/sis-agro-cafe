package com.sisagrodev.auth.repository;

import com.sisagrodev.auth.domain.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	@Query("""
			SELECT u FROM Usuario u
			WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
			   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :buscar, '%'))
			""")
	Page<Usuario> buscarPorNombreOEmail(@Param("buscar") String buscar, Pageable paginacion);
}
