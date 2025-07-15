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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.quartz.JobKey;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class QuartzConnectionIssueTest {

    private static final Logger log = LoggerFactory.getLogger(QuartzConnectionIssueTest.class);

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
        log.info("=== Quartz ConnectionProvider Inspection (BeforeEach) ===");
        inspectDBConnectionManager();
        log.info("=== End ConnectionProvider Inspection ===");
    }

    @AfterEach
    void cleanup() throws SchedulerException {
        log.info("=== Quartz ConnectionProvider Inspection (AfterEach) ===");
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

            // Check ConnectionProvider list
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

                    // Extract DataSource from LocalDataSourceJobStore
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

                            // Compare with currently injected DataSource
                            log.info("    - Provider DataSource == Autowired DataSource: {}",
                                    providerDataSource == dataSource);

                        } catch (Exception e) {
                            log.error("    - Failed to get provider DataSource: {}", e.getMessage());
                        }
                    }
                }
            }

            // Check scheduler name
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
    void test1_FirstTest() throws Exception {
        log.info("=== Test 1: First Test ===");

        // Save data
        testService.saveData("test1", "value1");
        assertThat(testService.countData()).isEqualTo(1);

        // Schedule job
        JobKey jobKey = schedulerService.scheduleTestJob();

        // Wait for job to execute
        Thread.sleep(1000);

        log.info("=== Test 1: Completed ===");
    }

    @Test
    void test2_SecondTest() throws Exception {
        log.info("=== Test 2: Second Test ===");

        // Save data
        testService.saveData("test2", "value2");
        assertThat(testService.countData()).isEqualTo(1);

        // Schedule job
        JobKey jobKey = schedulerService.scheduleTestJob();

        // Wait for job to execute
        Thread.sleep(1000);

        log.info("=== Test 2: Completed ===");
    }

    @Test
    void test3_ThirdTest() throws Exception {
        log.info("=== Test 3: Third Test ===");

        // Save data
        testService.saveData("test3", "value3");
        assertThat(testService.countData()).isEqualTo(1);

        // Schedule job
        JobKey jobKey = schedulerService.scheduleTestJob();

        // Wait for job to execute
        Thread.sleep(1000);

        log.info("=== Test 3: Completed ===");
    }
}