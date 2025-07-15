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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class QuartzConnectionClosedTest {

    private static final Logger log = LoggerFactory.getLogger(QuartzConnectionClosedTest.class);

    // Use Mock to create new ApplicationContext for test
    @MockitoBean
    private RestTemplate mockRestTemplate;

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

            // check ConnectionProviders
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

                    // extract DataSource of LocalDataSourceJobStore
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

                            // compare DataSource between provider and autowired DataSource
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
    void testConnectionClosedIssue() throws Exception {
        log.info("=== Connection Closed Issue Test ===");

        // Schedule job in first ApplicationContext
        testService.saveData("connectiontest", "value1");
        assertThat(testService.countData()).isEqualTo(1);

        // Schedule job
        schedulerService.scheduleTestJob();
        log.info("Job scheduled successfully");

        // Wait for job to execute
        Thread.sleep(2000);

        // Now to induce Connection is closed error
        // Force stop and restart scheduler
        log.info("Stopping scheduler...");
        scheduler.standby();
        
        log.info("Starting scheduler...");
        scheduler.start();

        // Create situation where Connection is closed error can occur
        // Schedule job multiple times to induce connection conflict
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

        // Perform multiple scheduler operations to induce connection conflict
        for (int i = 0; i < 10; i++) {
            try {
                log.info("Scheduler operation attempt {}", i + 1);
                
                // Save data
                testService.saveData("multitest" + i, "value" + i);
                
                // Schedule job
                schedulerService.scheduleTestJob();
                
                // Wait briefly
                Thread.sleep(200);
                
                // Check scheduler status
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