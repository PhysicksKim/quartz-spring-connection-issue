package com.example.quartz_spring_connection_issue.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "test_data")
public class TestData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "data_value")
    private String value;

    @Override
    public String toString() {
        return "TestData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TestData testData = (TestData) o;
        return Objects.equals(getId(), testData.getId()) && Objects.equals(getName(), testData.getName()) && Objects.equals(getValue(), testData.getValue());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getId());
        result = 31 * result + Objects.hashCode(getName());
        result = 31 * result + Objects.hashCode(getValue());
        return result;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}