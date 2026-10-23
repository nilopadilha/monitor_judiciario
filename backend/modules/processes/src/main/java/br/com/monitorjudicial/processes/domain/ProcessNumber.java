package br.com.monitorjudicial.processes.domain;

import br.com.monitorjudicial.shared.domain.BusinessRuleViolationException;
import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Número único de processo (Resolução CNJ 65/2008): NNNNNNN-DD.AAAA.J.TR.OOOO.
 * O dígito verificador DD é calculado pelo algoritmo ISO 7064 módulo 97-10.
 */
public record ProcessNumber(String digits) {

    private static final Pattern FORMAT = Pattern.compile("^(\\d{7})(\\d{2})(\\d{4})(\\d)(\\d{2})(\\d{4})$");
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);
    private static final int STATE_JUSTICE = 8;

    /** Código TR da Justiça Estadual → sigla do Tribunal de Justiça. */
    private static final Map<String, String> STATE_COURTS = Map.ofEntries(
            Map.entry("01", "TJAC"), Map.entry("02", "TJAL"), Map.entry("03", "TJAP"), Map.entry("04", "TJAM"),
            Map.entry("05", "TJBA"), Map.entry("06", "TJCE"), Map.entry("07", "TJDFT"), Map.entry("08", "TJES"),
            Map.entry("09", "TJGO"), Map.entry("10", "TJMA"), Map.entry("11", "TJMT"), Map.entry("12", "TJMS"),
            Map.entry("13", "TJMG"), Map.entry("14", "TJPA"), Map.entry("15", "TJPB"), Map.entry("16", "TJPR"),
            Map.entry("17", "TJPE"), Map.entry("18", "TJPI"), Map.entry("19", "TJRJ"), Map.entry("20", "TJRN"),
            Map.entry("21", "TJRS"), Map.entry("22", "TJRO"), Map.entry("23", "TJRR"), Map.entry("24", "TJSC"),
            Map.entry("25", "TJSE"), Map.entry("26", "TJSP"), Map.entry("27", "TJTO"));

    public ProcessNumber {
        String clean = digits == null ? "" : digits.replaceAll("\\D", "");
        Matcher m = FORMAT.matcher(clean);
        if (!m.matches()) {
            throw new BusinessRuleViolationException("invalid-process-number",
                    "O número do processo deve ter 20 dígitos no formato NNNNNNN-DD.AAAA.J.TR.OOOO.");
        }
        String expected = checkDigits(m.group(1), m.group(3), m.group(4), m.group(5), m.group(6));
        if (!expected.equals(m.group(2))) {
            throw new BusinessRuleViolationException("invalid-process-number",
                    "O dígito verificador " + m.group(2) + " não confere; o esperado é " + expected + ".");
        }
        digits = clean;
    }

    public static ProcessNumber parse(String value) {
        return new ProcessNumber(value);
    }

    /** Calcula DD = 98 − (NNNNNNN AAAA J TR OOOO 00 mod 97). */
    public static String checkDigits(String sequence, String year, String segment, String court, String origin) {
        BigInteger base = new BigInteger(sequence + year + segment + court + origin + "00");
        int dd = 98 - base.mod(NINETY_SEVEN).intValue();
        return String.format("%02d", dd);
    }

    public int segment() {
        return digits.charAt(13) - '0';
    }

    public String courtSegmentCode() {
        return digits.substring(14, 16);
    }

    public int year() {
        return Integer.parseInt(digits.substring(9, 13));
    }

    /** Sigla do tribunal derivada do número; disponível apenas para a Justiça Estadual (premissa P2). */
    public String courtCode() {
        if (segment() != STATE_JUSTICE || !STATE_COURTS.containsKey(courtSegmentCode())) {
            throw new BusinessRuleViolationException("unsupported-court",
                    "O MVP monitora apenas a Justiça Estadual (segmento 8); o número informado pertence ao segmento "
                            + segment() + ".");
        }
        return STATE_COURTS.get(courtSegmentCode());
    }

    public String formatted() {
        return digits.substring(0, 7) + "-" + digits.substring(7, 9) + "." + digits.substring(9, 13) + "."
                + digits.charAt(13) + "." + digits.substring(14, 16) + "." + digits.substring(16);
    }

    @Override
    public String toString() {
        return formatted();
    }
}
