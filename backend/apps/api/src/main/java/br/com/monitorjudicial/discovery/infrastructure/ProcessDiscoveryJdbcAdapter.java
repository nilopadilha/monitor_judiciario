package br.com.monitorjudicial.discovery.infrastructure;

import br.com.monitorjudicial.discovery.application.ProcessDiscoveryPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class ProcessDiscoveryJdbcAdapter implements ProcessDiscoveryPort {

    private final JdbcClient jdbc;
    private final Clock clock;

    @Override
    public Optional<ExistingProcess> findByDigits(String processDigits) {
        return jdbc.sql("select id, client_id, lawyer_id, process_number from processes where process_number = :num")
                .param("num", processDigits)
                .query(ProcessDiscoveryJdbcAdapter::mapProcess)
                .optional();
    }

    @Override
    @Transactional
    public void assignLawyer(UUID processId, UUID lawyerId, String practiceArea) {
        jdbc.sql("""
                update processes
                   set lawyer_id = :lawyerId,
                       practice_area = coalesce(practice_area, :area),
                       updated_at = :now
                 where id = :id
                """)
                .param("lawyerId", lawyerId)
                .param("area", practiceArea)
                .param("now", Timestamp.from(clock.instant()))
                .param("id", processId)
                .update();
    }

    @Override
    public List<ActiveClient> findActiveClients() {
        return jdbc.sql("select id, name from clients where status = 'ACTIVE'")
                .query((rs, rowNum) -> new ActiveClient(rs.getObject("id", UUID.class), rs.getString("name")))
                .list();
    }

    private static ExistingProcess mapProcess(ResultSet rs, int rowNum) throws SQLException {
        String d = rs.getString("process_number");
        String formatted = d != null && d.length() == 20
                ? d.substring(0, 7) + "-" + d.substring(7, 9) + "." + d.substring(9, 13) + "."
                        + d.charAt(13) + "." + d.substring(14, 16) + "." + d.substring(16)
                : d;
        return new ExistingProcess(
                rs.getObject("id", UUID.class),
                rs.getObject("client_id", UUID.class),
                rs.getObject("lawyer_id", UUID.class),
                formatted);
    }
}
