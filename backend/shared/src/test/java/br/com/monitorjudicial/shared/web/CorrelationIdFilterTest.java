package br.com.monitorjudicial.shared.web;

import br.com.monitorjudicial.shared.observability.CorrelationId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void propagatesWellFormedIdAndClearsMdc() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "abc-12345678");
        var response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> seen.set(CorrelationId.current().orElse(null)));
        assertThat(seen.get()).isEqualTo("abc-12345678");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("abc-12345678");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void replacesMaliciousOrMissingIds() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "bad\nheader injection");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> { });
        assertThat(response.getHeader(CorrelationId.HEADER)).matches("[0-9a-f-]{36}");
    }
}
