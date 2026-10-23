package br.com.monitorjudicial.notifications.web;

import br.com.monitorjudicial.notifications.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendNotificationRequest(@NotNull UUID processId, @NotNull Channel channel,
        @NotBlank @Size(max = 2000) String message) {
}
