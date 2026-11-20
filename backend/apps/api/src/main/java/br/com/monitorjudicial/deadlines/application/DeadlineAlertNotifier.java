package br.com.monitorjudicial.deadlines.application;

public interface DeadlineAlertNotifier {

    void notifyLawyer(DeadlinePort.DueDeadlineAlert alert, long daysRemaining);
}
