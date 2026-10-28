package com.contoh.test.scheduler;

import com.contoh.test.service.BatchProccessorService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class BatchProcessingJob extends QuartzJobBean {

    @Autowired
    private BatchProccessorService batchProccessorService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        batchProccessorService.proccessActiveJobs();
    }
}
