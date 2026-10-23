package br.com.monitorjudicial.clients.api;

import java.util.UUID;

/** Visão de cliente exposta a outros módulos. */
public record ClientSummary(UUID id, String name, String email, String phone, boolean notifyEmail,
        boolean notifyWhatsapp, boolean active) {
}
