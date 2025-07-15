# Quartz와 Spring Test Context 캐싱 문제 재현 프로젝트

이 프로젝트는 [블로그 포스트](https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/)에서 설명한 Quartz와 Spring Test Context 캐싱 문제를 재현하고 해결하는 방법을 보여줍니다.

## 문제 원인

1. **Quartz의 static DBConnectionManager**: Quartz는 static 클래스를 사용하여 데이터베이스 연결을 관리
2. **Spring Test Context 재사용**: 테스트 간 ApplicationContext가 재사용됨
3. **DataSource 변경**: 새로운 ApplicationContext가 생성될 때 DataSource가 변경됨
4. **ConnectionProvider 캐시**: Quartz의 ConnectionProvider가 이전 DataSource 참조를 유지

## 테스트 구성

### 1. 기본 테스트 (문제 재현)

-   `QuartzConnectionIssueTest`: 기본 테스트 클래스
-   `QuartzConnectionIssueWithMockTest`: @MockBean을 사용하여 새로운 ApplicationContext 생성
-   `QuartzConnectionClosedTest`: Connection is closed 에러를 강제로 발생시키는 테스트

### 2. 해결방법 테스트

-   `QuartzConnectionFixedTest`: QuartzConnectionResetListener를 사용하여 문제 해결

### 3. 해결방법 구현

-   `QuartzConnectionResetListener`: 블로그 포스트에서 제안한 해결방법 구현

## 테스트 실행 방법

### 1. 문제 재현 테스트 실행

```bash
# 기본 테스트 (문제가 발생할 수 있음)
./gradlew test --tests QuartzConnectionIssueTest

# MockBean을 사용한 테스트 (문제 발생 가능성 높음)
./gradlew test --tests QuartzConnectionIssueWithMockTest

# Connection is closed 에러 강제 발생 테스트
./gradlew test --tests QuartzConnectionClosedTest
```

### 2. 해결방법 테스트 실행

```bash
# 해결방법이 적용된 테스트
./gradlew test --tests QuartzConnectionFixedTest
```

### 3. 전체 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test
```

## 예상 결과

### 문제가 발생하는 경우

다음과 같은 에러가 발생할 수 있습니다:

```
Caused by: org.quartz.impl.jdbcjobstore.LockException:
Failure obtaining db row lock: Connection is closed
[See nested exception: java.sql.SQLException: Connection is closed]
```

### 로그에서 확인할 수 있는 내용

-   `Provider DataSource == Autowired DataSource: false` - DataSource 불일치
-   `DBConnectionManager instance` - static 인스턴스 확인
-   `ConnectionProvider count` - Provider 개수 확인

## 해결방법

### 1. @DirtiesContext 사용

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

### 2. TestExecutionListener 사용 (권장)

```java
@ExtendWith(QuartzConnectionResetListener.class)
```

### 3. 수동 ConnectionProvider 재설정

```java
private void setQuartzConnectionProviders(Scheduler scheduler, DataSource dataSource) {
    String schedulerName = scheduler.getSchedulerName();

    DBConnectionManager.getInstance()
        .addConnectionProvider("springTxDataSource." + schedulerName,
            new ConnectionProvider() {
                public Connection getConnection() throws SQLException {
                    return DataSourceUtils.doGetConnection(dataSource);
                }
                // ... 기타 메서드
            });
}
```

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/example/quartz_spring_connection_issue/
│   │       ├── controller/
│   │       ├── entity/
│   │       ├── job/
│   │       ├── repository/
│   │       ├── service/
│   │       └── QuartzSpringConnectionIssueApplication.java
│   └── resources/
│       └── application.yml
└── test/
    ├── java/
    │   └── com/example/quartz_spring_connection_issue/
    │       ├── QuartzConnectionIssueTest.java
    │       ├── QuartzConnectionIssueWithMockTest.java
    │       ├── QuartzConnectionClosedTest.java
    │       ├── QuartzConnectionFixedTest.java
    │       └── QuartzConnectionResetListener.java
    └── resources/
        └── application.yml
```

## 기술 스택

-   Spring Boot 2.7+
-   Quartz Scheduler
-   H2 In-Memory Database
-   JUnit 5
-   Gradle

## 참고 자료

-   [A problem with Quartz and Spring Test Context caching](https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/)
-   [Spring Boot Quartz Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.scheduling.quartz)
-   [Quartz Documentation](https://www.quartz-scheduler.org/documentation/)
