package br.com.monitorjudicial.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.monitorjudicial.authentication.domain.User;
import br.com.monitorjudicial.authentication.domain.port.PasswordHasher;
import br.com.monitorjudicial.authentication.domain.port.UserRepository;
import br.com.monitorjudicial.clients.api.ClientDirectory;
import br.com.monitorjudicial.clients.api.ClientSummary;
import br.com.monitorjudicial.shared.application.PageQuery;
import br.com.monitorjudicial.shared.application.PageResult;
import br.com.monitorjudicial.shared.audit.Auditor;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.Email;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final ClientDirectory clients = mock(ClientDirectory.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
    private final UserService service = new UserService(users, hasher, clients, mock(Auditor.class), clock);

    @Test
    void createsCustomerLinkedToExistingClient() {
        UUID client = UUID.randomUUID();
        when(clients.findById(client)).thenReturn(Optional.of(
                new ClientSummary(client, "C", "c@x.com", null, true, false, true)));
        when(hasher.hash("senha-forte-123")).thenReturn("bcrypt");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        UserView view = service.create(new CreateUserCommand("Cliente", "c@x.com", "senha-forte-123", Role.CUSTOMER,
                client));

        assertThat(view.role()).isEqualTo(Role.CUSTOMER);
        assertThat(view.clientId()).isEqualTo(client);
    }

    @Test
    void rejectsDuplicateEmailAndUnknownClient() {
        when(users.existsByEmail(new Email("dup@x.com"))).thenReturn(true);
        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("D", "dup@x.com", "senha-forte-123", Role.ADMIN, null)))
                .isInstanceOf(ConflictException.class);

        UUID missing = UUID.randomUUID();
        when(clients.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("C", "c@x.com", "senha-forte-123", Role.CUSTOMER, missing)))
                .extracting("code").isEqualTo("client-not-found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"curta1", "somenteletrasaqui", "123456789012345"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> UserService.validatePassword(password)).extracting("code").isEqualTo("weak-password");
    }

    @Test
    void getAndList() {
        User user = User.create("A", new Email("a@x.com"), "h", Role.ADMIN, null, clock.instant());
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(users.findAll(PageQuery.of(0, 20))).thenReturn(PageResult.of(List.of(user), 0, 20, 1));
        assertThat(service.get(user.id()).email()).isEqualTo("a@x.com");
        assertThat(service.list(PageQuery.of(0, 20)).totalElements()).isEqualTo(1);
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
