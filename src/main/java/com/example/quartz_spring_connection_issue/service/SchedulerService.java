package com.example.quartz_spring_connection_issue.service;

import com.example.quartz_spring_connection_issue.job.TestJob;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class SchedulerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SchedulerService.class);

    private final Scheduler scheduler;

    public SchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public JobKey scheduleTestJob() throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("testJob", "testGroup");
        
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class)
                .withIdentity(jobKey)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("testTrigger", "testGroup")
                .startAt(Date.from(LocalDateTime.now().plusSeconds(2).toInstant(ZoneOffset.UTC)))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(5)
                        .withRepeatCount(2))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        log.info("TestJob scheduled to run in 2 seconds with jobKey: {}", jobKey);
        
        return jobKey;
    }

    public void clearJobs() throws SchedulerException {
        scheduler.clear();
        log.info("All jobs cleared");
    }
}