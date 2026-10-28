package com.contoh.test.exception;

public class RetryExhaustedException extends RuntimeException{
    private final int attempts;
    public RetryExhaustedException(int attempts, Throwable throwable) {
        super("gagal " + attempts + "percobaan : " + throwable.getCause());
        this.attempts = attempts;
    }

    public int getAttempts() {
        return attempts;
    }
}
