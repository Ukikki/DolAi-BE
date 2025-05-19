package com.dolai.backend.admin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id
    private String username;

    private String password; // bcrypt 해시 저장
    private String role = "ADMIN";
}