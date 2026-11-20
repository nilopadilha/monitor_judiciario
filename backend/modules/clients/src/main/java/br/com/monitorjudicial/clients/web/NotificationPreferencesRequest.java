package br.com.monitorjudicial.clients.web;

public record NotificationPreferencesRequest(boolean notifyEmail, boolean notifyWhatsapp) {
}
