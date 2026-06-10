package com.arogya.cafe.inventory.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A vendor from whom ingredients are purchased (e.g. Sharma Dairy). */
@Entity
@Table(name = "supplier")
public class Supplier extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contact;

    protected Supplier() {
    }

    public Supplier(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
