package br.com.monitorjudicial.notifications.web;

import br.com.monitorjudicial.notifications.application.NotificationService;
import br.com.monitorjudicial.notifications.application.NotificationView;
import br.com.monitorjudicial.notifications.domain.Channel;
import br.com.monitorjudicial.notifications.domain.NotificationStatus;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /notifications} substitui {@code POST /notifications/send} do PDF (ADR-0003). */
@Tag(name = "Notificações")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @Operation(summary = "Lista notificações (CUSTOMER vê apenas as suas)")
    @GetMapping
    public PageResult<NotificationView> list(@RequestParam(required = false) UUID processId,
            @RequestParam(required = false) Channel channel, @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CurrentUser user) {
        return notifications.search(processId, channel, status, PageQuery.of(page, size), user);
    }

    @Operation(summary = "Envia notificação manual ao cliente do processo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public NotificationView send(@Valid @RequestBody SendNotificationRequest request) {
        return notifications.sendManual(request.processId(), request.channel(), request.message());
    }
}
