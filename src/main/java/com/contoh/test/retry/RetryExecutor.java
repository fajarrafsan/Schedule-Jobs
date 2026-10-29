package com.contoh.test.retry;

import com.contoh.test.exception.RetryExhaustedException;
import com.contoh.test.exception.TransientProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RetryExecutor {
    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int maxAttempts;
    private final long initialDelayMs;
    private final double multiplier;

    public RetryExecutor(@Value("${batch.retry.max-attempts:3}") int maxAttempts,
                         @Value("${batch.retry.initial-delay-ms:1000}") long initialDelayMs,
                         @Value("${batch.retry.multiplier:2.0}") double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
    }

    public int execute(String taskName, Runnable task) {
        long delay = initialDelayMs;
        for (int attempt = 1; ; attempt++) {
            try {
                task.run();
                return attempt;
            } catch (TransientProcessingException ex) {
                if (attempt >= maxAttempts) {
                    throw new RetryExhaustedException(attempt, ex);
                }
                log.warn("{} gagal (percobaan {}/{}): {}. Retry dalam {} ms",
                        taskName, attempt, maxAttempts, ex.getMessage(), delay);
                sleep(delay);
                delay = (long) (delay * multiplier);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry dihentikan karena thread di-interrupt", e);
        }
    }
}
