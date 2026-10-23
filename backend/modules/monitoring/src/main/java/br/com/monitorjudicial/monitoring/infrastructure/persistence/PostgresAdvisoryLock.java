package br.com.monitorjudicial.monitoring.infrastructure.persistence;

import br.com.monitorjudicial.monitoring.domain.port.MonitoringLock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Advisory lock de sessão: a conexão fica reservada enquanto a execução roda; se a aplicação cair, o PostgreSQL libera
 * o lock com a conexão (ADR-0006).
 */
@Component
class PostgresAdvisoryLock implements MonitoringLock {

    static final long LOCK_KEY = 7_277_100_001L;
    private static final Logger log = LoggerFactory.getLogger(PostgresAdvisoryLock.class);

    private final DataSource dataSource;

    PostgresAdvisoryLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Handle> tryAcquire() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            if (execute(connection, "select pg_try_advisory_lock(?)")) {
                Connection held = connection;
                return Optional.of(() -> release(held));
            }
            connection.close();
            return Optional.empty();
        } catch (SQLException e) {
            closeQuietly(connection);
            throw new IllegalStateException("Não foi possível obter o lock de monitoramento", e);
        }
    }

    private static void release(Connection connection) {
        try {
            execute(connection, "select pg_advisory_unlock(?)");
        } catch (SQLException e) {
            log.warn("Falha ao liberar advisory lock; será liberado ao fechar a conexão: {}", e.getMessage());
        } finally {
            closeQuietly(connection);
        }
    }

    private static boolean execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, LOCK_KEY);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // conexão já inutilizável
            }
        }
    }
}
