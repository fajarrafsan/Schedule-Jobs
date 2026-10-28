package com.contoh.test.service;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

@Service
public class SchedulerService {
    private static final JobKey JOB_KEY = JobKey.jobKey("batchProcessingJob", "batch");
    private static final TriggerKey TRIGGER_KEY = TriggerKey.triggerKey("batchProcessingTrigger", "batch");

    private final Scheduler scheduler;
    private final int intervalSeconds;

    public SchedulerService(Scheduler scheduler, int intervalSeconds) {
        this.scheduler = scheduler;
        this.intervalSeconds = intervalSeconds;
    }


}
