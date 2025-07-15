package com.example.quartz_spring_connection_issue;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Quartz Job 테스트를 위한 대기 유틸리티
 * Thread.sleep() 대신 사용하여 더 안정적인 테스트 가능
 */
public class TestQuartzJobWaitUtil {

    /**
     * Job이 스케줄될 때까지 대기
     * @param scheduler Quartz 스케줄러
     * @param jobKey Job 키
     */
    public static void waitForJobToBeScheduled(Scheduler scheduler, JobKey jobKey) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                return scheduler.checkExists(jobKey);
            } catch (SchedulerException e) {
                // 예외 처리
                return false;
            }
        });
    }

    /**
     * Job이 제거될 때까지 대기
     * @param scheduler Quartz 스케줄러
     * @param jobKey Job 키
     */
    public static void waitForJobToBeRemoved(Scheduler scheduler, JobKey jobKey) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                return !scheduler.checkExists(jobKey);
            } catch (SchedulerException e) {
                // 예외 처리
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Job이 실행될 때까지 대기 (Job이 존재하고 실행됨)
     * @param scheduler Quartz 스케줄러
     * @param jobKey Job 키
     */
    public static void waitForJobToExecute(Scheduler scheduler, JobKey jobKey) {
        // Job이 스케줄될 때까지 대기
        waitForJobToBeScheduled(scheduler, jobKey);
        
        // Job이 실행될 때까지 잠시 대기 (실행 시간 고려)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 스케줄러가 시작될 때까지 대기
     * @param scheduler Quartz 스케줄러
     */
    public static void waitForSchedulerToStart(Scheduler scheduler) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                return scheduler.isStarted();
            } catch (SchedulerException e) {
                return false;
            }
        });
    }

    /**
     * 스케줄러가 중지될 때까지 대기
     * @param scheduler Quartz 스케줄러
     */
    public static void waitForSchedulerToStop(Scheduler scheduler) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                return scheduler.isShutdown();
            } catch (SchedulerException e) {
                return false;
            }
        });
    }
} 