package com.example.quartz_spring_connection_issue;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 각 테스트 실행 전에 Quartz의 DBConnectionManager를 올바른 DataSource로 재설정 해줍니다.
 * Quartz 가 포함된 테스트에서 connection closed 에러가 발생하는 문제를 해결하기 위한 리스너입니다.
 *
 * @see <a href="https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/">A problem with Quartz and Spring Test Context caching</a>
 */
public class QuartzConnectionResetListener implements BeforeEachCallback {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuartzConnectionResetListener.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        log.info("=== QuartzConnectionResetListener: Resetting Quartz Connection Providers ===");
        
        try {
            // Spring ApplicationContext에서 필요한 빈들을 가져옴
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
        
        // springTxDataSource ConnectionProvider 등록
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
        
        // springNonTxDataSource ConnectionProvider 등록
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