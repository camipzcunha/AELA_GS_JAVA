package br.com.aela.repository;

import br.com.aela.model.AlertaFisiologico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITÓRIO: AlertaFisiologicoRepository
 *
 * Acesso ao banco Oracle para a entidade AlertaFisiologico.
 * Utilizado pelo endpoint SOAP para persistir e consultar alertas.
 */
@Repository
public interface AlertaFisiologicoRepository extends JpaRepository<AlertaFisiologico, Long> {

    /**
     * Lista todos os alertas de um operador, do mais recente para o mais antigo.
     * Usado na operação SOAP de consulta de histórico.
     */
    List<AlertaFisiologico> findByOperadorIdOrderByDataRegistroDesc(Long operadorId);

    /**
     * Lista alertas não resolvidos de um operador.
     * Útil para dashboard de monitoramento da missão.
     */
    List<AlertaFisiologico> findByOperadorIdAndResolvidoFalseOrderByDataRegistroDesc(Long operadorId);
}
