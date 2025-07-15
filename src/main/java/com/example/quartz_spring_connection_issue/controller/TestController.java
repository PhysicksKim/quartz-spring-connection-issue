package com.example.quartz_spring_connection_issue.controller;

import com.example.quartz_spring_connection_issue.service.SchedulerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestController.class);

    private final SchedulerService schedulerService;

    public TestController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/schedule")
    public String scheduleJob() throws Exception {
        schedulerService.scheduleTestJob();
        return "Job scheduled";
    }

    @DeleteMapping("/clear")
    public String clearJobs() throws Exception {
        schedulerService.clearJobs();
        return "Jobs cleared";
    }
}