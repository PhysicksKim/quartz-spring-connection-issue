package com.example.quartz_spring_connection_issue;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Resets Quartz's DBConnectionManager with the correct DataSource before each test execution.
 * This listener is designed to solve connection closed errors in tests that include Quartz.
 *
 * @see <a href="https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/">A problem with Quartz and Spring Test Context caching</a>
 */
public class QuartzConnectionResetListener implements BeforeEachCallback {

    private static final Logger log = LoggerFactory.getLogger(QuartzConnectionResetListener.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        log.info("=== QuartzConnectionResetListener: Resetting Quartz Connection Providers ===");
        
        try {
            // Get required beans from Spring ApplicationContext
            var applicationContext = SpringExtension.getApplicationContext(context);
            if (applicationContext != null) {
                try {
                    Scheduler scheduler = applicationContext.getBean(Scheduler.class);
                    DataSource dataSource = applicationContext.getBean(DataSource.class);
                    
                    setQuartzConnectionProviders(scheduler, dataSource);
                    
                } catch (Exception e) {
                    log.error("Failed to reset Quartz connection providers: {}", e.getMessage(), e);
                }
            }
                    
        } catch (Exception e) {
            log.error("Failed to get ApplicationContext: {}", e.getMessage(), e);
        }
        
        log.info("=== QuartzConnectionResetListener: Reset completed ===");
    }

    private void setQuartzConnectionProviders(Scheduler scheduler, DataSource dataSource) throws SchedulerException {
        String schedulerName = scheduler.getSchedulerName();
        
        log.info("Setting Quartz connection providers for scheduler: {}", schedulerName);
        
        // Register springTxDataSource ConnectionProvider
        DBConnectionManager.getInstance()
                .addConnectionProvider("springTxDataSource." + schedulerName,
                        new ConnectionProvider() {
                            public Connection getConnection() throws SQLException {
                                return DataSourceUtils.doGetConnection(dataSource);
                            }

                            public void shutdown() {
                            }

                            public void initialize() {
                            }
                        });
        
        // Register springNonTxDataSource ConnectionProvider
        DBConnectionManager.getInstance()
                .addConnectionProvider("springNonTxDataSource." + schedulerName,
                        new ConnectionProvider() {
                            public Connection getConnection() throws SQLException {
                                return dataSource.getConnection();
                            }

                            public void shutdown() {
                            }

                            public void initialize() {
                            }
                        });
        
        log.info("Quartz connection providers set successfully for scheduler: {}", schedulerName);
    }
} 