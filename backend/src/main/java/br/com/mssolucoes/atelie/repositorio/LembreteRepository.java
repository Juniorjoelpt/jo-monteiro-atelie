package br.com.mssolucoes.atelie.repositorio;

import br.com.mssolucoes.atelie.dominio.Lembrete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LembreteRepository extends JpaRepository<Lembrete, Long> {

    Optional<Lembrete> findByChave(String chave);

    List<Lembrete> findByChaveIn(Collection<String> chaves);
}
