package com.example.quartz_spring_connection_issue.service;

import com.example.quartz_spring_connection_issue.entity.TestData;
import com.example.quartz_spring_connection_issue.repository.TestDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestService.class);

    private final TestDataRepository testDataRepository;

    public TestService(TestDataRepository testDataRepository) {
        this.testDataRepository = testDataRepository;
    }

    @Transactional
    public void saveData(String name, String value) {
        TestData data = new TestData();
        data.setName(name);
        data.setValue(value);
        testDataRepository.save(data);
        log.info("Saved data: {} = {}", name, value);
    }

    @Transactional(readOnly = true)
    public long countData() {
        long count = testDataRepository.count();
        log.info("Data count: {}", count);
        return count;
    }
}