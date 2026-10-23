package br.com.monitorjudicial.tribunals.api;

public record TribunalSummary(String code, String name, String state, boolean active) implements java.io.Serializable {
}
