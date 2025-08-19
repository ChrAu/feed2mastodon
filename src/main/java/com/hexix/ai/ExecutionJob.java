package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class ExecutionJob extends PanacheEntity {

    public String schedulerName;
    public LocalDateTime executionTime;
    public boolean completed;

}
