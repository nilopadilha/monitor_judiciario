package br.com.monitorjudicial.tribunals.api;

import java.util.Optional;

public interface TribunalDirectory {

    Optional<TribunalSummary> findByCode(String code);
}
