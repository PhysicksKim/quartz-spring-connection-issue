package com.example.quartz_spring_connection_issue;

import com.example.quartz_spring_connection_issue.service.SchedulerService;
import com.example.quartz_spring_connection_issue.service.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class QuartzConnectionClosedTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuartzConnectionClosedTest.class);

    // @MockBean을 사용하여 새로운 ApplicationContext 생성
    @MockitoBean
    private org.springframework.web.client.RestTemplate mockRestTemplate;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestService testService;

    @Autowired
    private SchedulerService schedulerService;

    @BeforeEach
    void inspectQuartzConnectionProviders() {
        log.info("=== Quartz ConnectionProvider Inspection (ConnectionClosedTest - BeforeEach) ===");
        inspectDBConnectionManager();
        log.info("=== End ConnectionProvider Inspection ===");
    }

    @AfterEach
    void cleanup() throws SchedulerException {
        log.info("=== Quartz ConnectionProvider Inspection (ConnectionClosedTest - AfterEach) ===");
        inspectDBConnectionManager();
        log.info("=== End ConnectionProvider Inspection ===");

        schedulerService.clearJobs();
        scheduler.clear();
    }

    private void inspectDBConnectionManager() {
        try {
            DBConnectionManager connectionManager = DBConnectionManager.getInstance();

            log.info("DBConnectionManager instance: {} (hashCode: {})",
                    connectionManager.getClass().getName(), connectionManager.hashCode());

            // ConnectionProvider 목록 확인
            Map<String, ConnectionProvider> providers =
                    (Map<String, org.quartz.utils.ConnectionProvider>)
                            getFieldValue(connectionManager, "providers");

            if (providers != null) {
                log.info("ConnectionProvider count: {}", providers.size());
                for (String name : providers.keySet()) {
                    org.quartz.utils.ConnectionProvider provider = providers.get(name);
                    log.info("ConnectionProvider Name: {}, providerClass: {}",
                            name, provider.getClass().getName());
                    log.info("  - Provider: {} -> {} (hashCode: {})",
                            name, provider.getClass().getName(), provider.hashCode());

                    // LocalDataSourceJobStore의 DataSource 추출
                    if (provider.getClass().getName().contains("LocalDataSourceJobStore$")) {
                        try {
                            Field outerClassField = provider.getClass().getDeclaredField("this$0");
                            outerClassField.setAccessible(true);
                            Object localJobStore = outerClassField.get(provider);

                            log.info("    - Outer LocalDataSourceJobStore: {} (hashCode: {})",
                                    localJobStore.getClass().getName(), localJobStore.hashCode());

                            DataSource providerDataSource =
                                    (DataSource) getFieldValue(localJobStore, "dataSource");

                            log.info("    - Provider DataSource: {} (hashCode: {})",
                                    providerDataSource != null ? providerDataSource.getClass().getName() : "NULL",
                                    providerDataSource != null ? providerDataSource.hashCode() : "NULL");

                            // 현재 주입받은 DataSource와 비교
                            log.info("    - Provider DataSource == Autowired DataSource: {}",
                                    providerDataSource == dataSource);

                        } catch (Exception e) {
                            log.error("    - Failed to get provider DataSource: {}", e.getMessage());
                        }
                    }
                }
            }

            // 스케줄러 이름 확인
            String schedulerName = scheduler.getSchedulerName();
            log.info("Current Scheduler Name: {}", schedulerName);

        } catch (Exception e) {
            log.error("Failed to inspect DBConnectionManager: {}", e.getMessage(), e);
        }
    }

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    @Test
    void testConnectionClosedIssue() throws Exception {
        log.info("=== Connection Closed Issue Test ===");

        // 첫 번째 ApplicationContext에서 Job 스케줄링
        testService.saveData("connectiontest", "value1");
        assertThat(testService.countData()).isEqualTo(1);

        // Job 스케줄링
        schedulerService.scheduleTestJob();
        log.info("Job scheduled successfully");

        // 잠시 대기하여 Job이 실행되도록 함
        Thread.sleep(2000);

        // 이제 Connection is closed 에러를 유발하기 위해
        // 스케줄러를 강제로 중지하고 다시 시작
        log.info("Stopping scheduler...");
        scheduler.standby();
        
        log.info("Starting scheduler...");
        scheduler.start();

        // Connection is closed 에러가 발생할 수 있는 상황 생성
        // 여러 번 Job을 스케줄링하여 Connection 충돌 유발
        for (int i = 0; i < 5; i++) {
            try {
                log.info("Attempting to schedule job {} times", i + 1);
                schedulerService.scheduleTestJob();
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("Error during job scheduling attempt {}: {}", i + 1, e.getMessage());
                if (e.getMessage().contains("Connection is closed") || 
                    e.getMessage().contains("LockException")) {
                    log.error("*** CONNECTION CLOSED ERROR DETECTED ***");
                    throw e;
                }
            }
        }

        log.info("=== Connection Closed Issue Test Completed ===");
    }

    @Test
    void testMultipleSchedulerOperations() throws Exception {
        log.info("=== Multiple Scheduler Operations Test ===");

        // 여러 번의 스케줄러 작업을 수행하여 Connection 충돌 유발
        for (int i = 0; i < 10; i++) {
            try {
                log.info("Scheduler operation attempt {}", i + 1);
                
                // 데이터 저장
                testService.saveData("multitest" + i, "value" + i);
                
                // Job 스케줄링
                schedulerService.scheduleTestJob();
                
                // 잠시 대기
                Thread.sleep(200);
                
                // 스케줄러 상태 확인
                boolean isStarted = scheduler.isStarted();
                log.info("Scheduler is started: {}", isStarted);
                
            } catch (Exception e) {
                log.error("Error during scheduler operation {}: {}", i + 1, e.getMessage());
                if (e.getMessage().contains("Connection is closed") || 
                    e.getMessage().contains("LockException") ||
                    e.getMessage().contains("Failure obtaining db row lock")) {
                    log.error("*** CONNECTION CLOSED ERROR DETECTED ***");
                    throw e;
                }
            }
        }

        log.info("=== Multiple Scheduler Operations Test Completed ===");
    }
} 