package com.contoh.test.controllers;

import com.contoh.test.dto.SchedulerStatusResponse;
import com.contoh.test.service.SchedulerService;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/start")
    public SchedulerStatusResponse start() throws SchedulerException {
        return schedulerService.start();
    }

    @PostMapping("/stop")
    public SchedulerStatusResponse stop() throws SchedulerException {
        return schedulerService.stop();
    }

    @GetMapping("/status")
    public SchedulerStatusResponse status() throws SchedulerException {
        return schedulerService.status();
    }
}
