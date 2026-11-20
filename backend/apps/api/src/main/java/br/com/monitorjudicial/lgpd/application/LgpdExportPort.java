package br.com.monitorjudicial.lgpd.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LgpdExportPort {

    Optional<ClientDataExport> loadClientExport(UUID clientId, Instant exportedAt);
}
