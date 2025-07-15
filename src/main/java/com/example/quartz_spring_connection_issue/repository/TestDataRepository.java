package com.example.quartz_spring_connection_issue.repository;

import com.example.quartz_spring_connection_issue.entity.TestData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestDataRepository extends JpaRepository<TestData, Long> {
}