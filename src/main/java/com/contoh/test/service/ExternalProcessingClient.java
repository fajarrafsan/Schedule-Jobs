package com.contoh.test.service;

import com.contoh.test.exception.TransientProcessingException;
import com.contoh.test.model.Batchjob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExternalProcessingClient {
    private static final Logger log = LoggerFactory.getLogger(ExternalProcessingClient.class);

    public void process(Batchjob job) {
        if (ThreadLocalRandom.current().nextInt(100) < 30) {
            throw new TransientProcessingException(
                    "Timeout memanggil service eksternal untuk job id=" + job.getId());
        }
        log.info("BatchJob id={} ({}) berhasil diproses", job.getId(), job.getJobName());
    }
}
