package br.com.monitorjudicial.shared.web;

import br.com.monitorjudicial.shared.observability.CorrelationId;
import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import br.com.monitorjudicial.shared.domain.ConflictException;
import br.com.monitorjudicial.shared.domain.DomainException;
import br.com.monitorjudicial.shared.domain.ResourceNotFoundException;
import br.com.monitorjudicial.shared.domain.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduz exceções em RFC 9457 Problem Details, sem vazar SQL nem stack trace. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String PROBLEM_BASE = "https://monitorjudicial.app/problems/";
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex, "Recurso não encontrado");
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail unauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex, "Não autenticado");
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail businessRule(BusinessRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex, "Regra de negócio violada");
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex, "Conflito");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return base(HttpStatus.CONFLICT, "data-integrity", "Conflito",
                "A operação viola uma restrição de unicidade ou referência.");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ProblemDetail accessDenied(RuntimeException ex) {
        return base(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado",
                "Seu perfil não permite esta operação.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraint(ConstraintViolationException ex) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "validation", "Dados inválidos", "Parâmetros inválidos.");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException ex) {
        return base(HttpStatus.BAD_REQUEST, "validation", "Dados inválidos",
                "Valor inválido para o parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return base(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Erro interno",
                "Ocorreu um erro inesperado. Informe o correlationId ao suporte.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = base(HttpStatus.BAD_REQUEST, "validation", "Dados inválidos",
                "Um ou mais campos são inválidos.");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(base(HttpStatus.BAD_REQUEST, "malformed-body", "Corpo inválido",
                "O corpo da requisição não é um JSON válido para este recurso."));
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
            WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            CorrelationId.current().ifPresent(id -> pd.setProperty("correlationId", id));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problem(HttpStatus status, DomainException ex, String title) {
        return base(status, ex.code(), title, ex.getMessage());
    }

    private ProblemDetail base(HttpStatus status, String code, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(PROBLEM_BASE + code));
        pd.setTitle(title);
        CorrelationId.current().ifPresent(id -> pd.setProperty("correlationId", id));
        return pd;
    }
}
