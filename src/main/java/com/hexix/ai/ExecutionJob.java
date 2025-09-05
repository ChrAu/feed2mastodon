package com.hexix.ai;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_jobs")
public class ExecutionJob extends PanacheEntityBase {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "execution_jobs_id_seq", // WICHTIG: Passe dies an den Namen deiner DB-Sequenz an
            allocationSize = 1
    )
    @Column(name = "id")
    public Long id;

    @Column(name = "scheduler_name", columnDefinition = "TEXT")
    public String schedulerName;
    @Column(name = "execution_time")
    public LocalDateTime executionTime;
    @Column(name = "is_completed", columnDefinition = "BOOLEAN DEFAULT false")
    public boolean completed;


    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(final String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "ExecutionJob{" +
                "id=" + id +
                ", schedulerName='" + schedulerName + '\'' +
                ", executionTime=" + executionTime +
                ", completed=" + completed +
                '}';
    }
}
