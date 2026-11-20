package br.com.monitorjudicial.deadlines.application;

public record DeadlineSummary(long overdue, long dueToday, long dueIn3Days, long dueIn7Days, long openTotal) {
}
