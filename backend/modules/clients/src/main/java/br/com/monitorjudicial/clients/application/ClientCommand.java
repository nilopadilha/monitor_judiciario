package br.com.monitorjudicial.clients.application;

/** Dados de cadastro/alteração. O documento é ignorado na alteração (imutável). */
public record ClientCommand(String name, String document, String email, String phone, boolean notifyEmail,
        boolean notifyWhatsapp) {
}
