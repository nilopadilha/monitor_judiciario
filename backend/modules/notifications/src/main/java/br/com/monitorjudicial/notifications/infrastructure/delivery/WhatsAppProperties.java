package br.com.monitorjudicial.notifications.infrastructure.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** WhatsApp Cloud API (Meta). Sem token, os envios falham com motivo explícito — nunca simulam sucesso. */
@ConfigurationProperties(prefix = "notifications.whatsapp")
public record WhatsAppProperties(String baseUrl, String apiVersion, String phoneNumberId, String accessToken,
        String templateName, String templateLanguage) {

    public WhatsAppProperties {
        baseUrl = baseUrl == null ? "https://graph.facebook.com" : baseUrl;
        apiVersion = apiVersion == null ? "v21.0" : apiVersion;
        templateName = templateName == null ? "nova_publicacao" : templateName;
        templateLanguage = templateLanguage == null ? "pt_BR" : templateLanguage;
    }

    boolean configured() {
        return phoneNumberId != null && !phoneNumberId.isBlank() && accessToken != null && !accessToken.isBlank();
    }
}
