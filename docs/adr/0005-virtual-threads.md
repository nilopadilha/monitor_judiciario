# ADR-0005 — Virtual Threads com concorrência limitada no monitoramento

- **Status:** aceito · **Data:** 2026-10-23

## Contexto
O monitoramento faz uma chamada HTTP ao DJEN por processo. O trabalho é dominado por I/O. Um pool de threads de plataforma limita a vazão ou consome muita memória; paralelismo ilimitado provoca `429` no DJEN.

## Decisão
Cada processo é verificado em uma Virtual Thread (`Executors.newVirtualThreadPerTaskExecutor()`), com `Semaphore(monitoring.max-concurrency)` (padrão 8) controlando quantas chamadas simultâneas chegam ao DJEN. Tomcat também usa Virtual Threads (`spring.threads.virtual.enabled`).

## Consequências
+ Código imperativo simples, alta vazão de I/O. − Evitar `synchronized` em caminhos com I/O (pinning) — usamos `ReentrantLock`/estruturas concorrentes.
