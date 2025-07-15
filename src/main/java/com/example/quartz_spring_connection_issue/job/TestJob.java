package com.example.quartz_spring_connection_issue.job;

import com.example.quartz_spring_connection_issue.service.TestService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class TestJob implements Job {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestJob.class);

    private final TestService testService;

    public TestJob(TestService testService) {
        this.testService = testService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("=== TestJob executing ===");

            // 트랜잭션 내에서 데이터 저장
            testService.saveData("job_data", "value_" + System.currentTimeMillis());

            // 데이터 개수 확인
            long count = testService.countData();
            log.info("Current data count: {}", count);

            log.info("=== TestJob completed ===");
        } catch (Exception e) {
            log.error("TestJob execution failed", e);
            throw new JobExecutionException(e);
        }
    }
}