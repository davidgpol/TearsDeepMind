package com.tearsdeepmind.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "templates", schema = "analysis")
public class TemplateEntity {

    @Id
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public TemplateEntity() {}

    public TemplateEntity(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
