package com.proyecto.servicios.exception;

public class SaldoInsuficienteException extends OperacionNoProcesableException {
    public SaldoInsuficienteException() {
        super("Saldo insuficiente para realizar la operacion");
    }
}