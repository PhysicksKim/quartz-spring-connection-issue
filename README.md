# Quartz and Spring Test Context DataSource Mismatch Issue

## What is this issue?

This issue is presumed to occur when Spring Test Context recreates ApplicationContext but does not update the DataSource field in `LocalDataSourceJobStore`.

When DataSource and DB Connection are newly created, the DataSource used by Quartz for Job access should also be updated, but we confirmed that the DataSource in `LocalDataSourceJobStore` and the Autowired DataSource are mismatched.

---

## ⚠️ Important: Run Full Test Suite

**This issue may not always reproduce on the first full test run.**

-   **Run the ALL test suite**: `./gradlew test`
-   **If all tests pass initially**: Run multiple times or test on different environments
-   **Why this happens**: The issue depends on Spring Test Context's DataSource initialization timing and ApplicationContext lifecycle

---

## Problem Analysis

### Key Evidence from Test Logs

**Successful Test (No Issues)**:

```
Provider DataSource == Autowired DataSource: true
```

**Failing Test (Connection Closed Error)**:

```
Provider DataSource == Autowired DataSource: false
```

### What Happens

1. **First Test**: LocalDataSourceJobStore initializes with DataSource A
2. **Second Test**: Spring creates new ApplicationContext with DataSource B
3. **Problem**: LocalDataSourceJobStore still references DataSource A (Closed)
4. **Result**: "Connection is closed" error when Quartz tries to use DataSource A

## Test Configuration

### Problem Reproduction Tests

-   `QuartzConnectionIssueTest`: Basic test (may succeed due to context reuse)
-   `QuartzConnectionIssueWithMockTest`: **Most reliable reproduction** (forces new ApplicationContext)
-   `QuartzConnectionClosedTest`: Additional test to induce Connection Closed issues (ApplicationContext recreation)

### Solution Tests

-   `QuartzConnectionFixedTest`: Demonstrates the fix using TestExecutionListener

## Expected Error

```
java.sql.SQLException: Connection is closed
    at org.quartz.impl.jdbcjobstore.LockException:
    Failure obtaining db row lock: Connection is closed
```

## Solution

### 1. TestExecutionListener (Recommended)

```java
@ExtendWith(QuartzConnectionResetListener.class)
```

### 2. @DirtiesContext

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

## Technology Stack

-   Spring Boot 3.5.3
-   Quartz Scheduler
-   H2 In-Memory Database
-   JUnit 5

## References

-   [Original Blog Post](https://mickaelb.com/post/problem-with-quartz-and-spring-test-context-caching/)
-   [Spring Boot Quartz Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.scheduling.quartz)
