package br.com.joeljunior.atelie.repositorio;

import br.com.joeljunior.atelie.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    java.util.List<Usuario> findAllByOrderByNomeAsc();

    long countByPerfilAndAtivoTrue(br.com.joeljunior.atelie.dominio.Enums.Perfil perfil);
}
