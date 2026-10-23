package br.com.monitorjudicial.authentication.infrastructure.security;

import br.com.monitorjudicial.shared.observability.CorrelationId;
import br.com.monitorjudicial.shared.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Escreve Problem Details direto no response, para erros gerados nos filtros de segurança. */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(ObjectMapper mapper, HttpServletResponse response, HttpStatus status, String code, String title,
            String detail) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(GlobalExceptionHandler.PROBLEM_BASE + code));
        pd.setTitle(title);
        CorrelationId.current().ifPresent(id -> pd.setProperty("correlationId", id));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), pd);
    }
}
