# Quartz와 Spring Test Context DataSource 불일치 문제

## 이 이슈는 무엇인가요?

Spring Test Context가 ApplicationContext 재생성 시, 새로운 DataSource를 `LocalDataSourceJobStore`의 DataSource 필드에 업데이트 하지 않아서 발생하는 문제로 추정합니다.

DataSource 및 DB Connection 이 새롭게 생성된다면 Quartz 에서 Job 에 접근하기 위한 DataSource 도 업데이트 되어야 하지만, LocalDataSourceJobStore 의 DataSource 와 Autowired 된 DataSource 가 불일치 함을 확인했습니다.

---

## ⚠️ 중요: 전체 테스트 실행

**첫 전체 테스트에서 항상 재현되지 않을 수 있습니다.**

-   **전체 테스트 스위트 실행**: `./gradlew test`
-   **처음에 모든 테스트가 통과한다면**: 여러 번 실행하거나 다른 환경에서 테스트
-   **이유**: 이 문제는 Spring Test Context의 DataSource 초기화 타이밍과 ApplicationContext 생명주기에 따라 달라집니다

---

## 문제 분석

### 테스트 로그의 핵심 증거

**성공하는 테스트 (문제 없음)**:

```
Provider DataSource == Autowired DataSource: true
```

**실패하는 테스트 (Connection Closed 에러)**:

```
Provider DataSource == Autowired DataSource: false
```

### 발생 과정

1. **첫 번째 테스트**: LocalDataSourceJobStore가 DataSource A로 초기화
2. **두 번째 테스트**: Spring이 DataSource B로 새로운 ApplicationContext 생성
3. **문제**: LocalDataSourceJobStore가 여전히 DataSource A 참조 (Closed)
4. **결과**: Quartz가 DataSource A 사용 시도 시 "Connection is closed" 에러

## 테스트 구성

### 문제 재현 테스트

-   `QuartzConnectionIssueTest`: 기본 테스트 (컨텍스트 재사용으로 성공할 수 있음)
-   `QuartzConnectionIssueWithMockTest`: **가장 안정적인 재현** (새로운 ApplicationContext 강제 생성)
-   `QuartzConnectionClosedTest`: Connection Closed 문제 유도를 위한 추가 테스트 (ApplicationContext 재생성)

### 해결책 테스트

-   `QuartzConnectionFixedTest`: TestExecutionListener를 사용한 해결책 시연

## 예상 에러

```
java.sql.SQLException: Connection is closed
    at org.quartz.impl.jdbcjobstore.LockException:
    Failure obtaining db row lock: Connection is closed
```

## Solution

### 1. TestExecutionListener (권장)

```java
@ExtendWith(QuartzConnectionResetListener.class)
```

### 2. @DirtiesContext

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

## 기술 스택

-   Spring Boot 3.5.3
-   Quartz Scheduler
-   H2 In-Memory Database
-   JUnit 5

## 참고 자료

-   [원본 블로그 포스트](https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/)
-   [Spring Boot Quartz Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.scheduling.quartz)
