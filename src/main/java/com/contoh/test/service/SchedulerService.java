package com.contoh.test.service;

import com.contoh.test.dto.SchedulerStatusResponse;
import com.contoh.test.scheduler.BatchProcessingJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class SchedulerService {
    private static final JobKey JOB_KEY = JobKey.jobKey("batchProcessingJob", "batch");
    private static final TriggerKey TRIGGER_KEY = TriggerKey.triggerKey("batchProcessingTrigger", "batch");

    private final Scheduler scheduler;
    private final int intervalSeconds;

    public SchedulerService(Scheduler scheduler, @Value("${batch.scheduler.interval-seconds:60}") int intervalSeconds) {
        this.scheduler = scheduler;
        this.intervalSeconds = intervalSeconds;
    }

    public synchronized SchedulerStatusResponse start() throws SchedulerException {
        if (!scheduler.checkExists(JOB_KEY)) {
            JobDetail jobDetail = JobBuilder.newJob(BatchProcessingJob.class)
                    .withIdentity(JOB_KEY)
                    .withDescription("Proses BatchJob ACTIVE secara berkala")
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(TRIGGER_KEY)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(intervalSeconds)
                            .repeatForever())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        }
        return status();
    }

    public synchronized SchedulerStatusResponse stop() throws SchedulerException {
        scheduler.deleteJob(JOB_KEY);
        return status();
    }


    public SchedulerStatusResponse status() throws SchedulerException {
        Trigger trigger = scheduler.getTrigger(TRIGGER_KEY);
        if (trigger == null) {
            return new SchedulerStatusResponse("STOPPED", intervalSeconds, null, null);
        }

        return new SchedulerStatusResponse("RUNNING", intervalSeconds,
                toLocalDateTime(trigger.getPreviousFireTime()),
                toLocalDateTime(trigger.getNextFireTime()));
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
