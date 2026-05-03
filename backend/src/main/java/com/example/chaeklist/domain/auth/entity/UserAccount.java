package com.example.chaeklist.domain.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, unique = true, length = 50)
	private String nickname;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(nullable = false, length = 20)
	private String role;

	@Column(name = "onboarding_completed", nullable = false)
	private boolean onboardingCompleted;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected UserAccount() {
	}

	public UserAccount(String email, String nickname, String passwordHash, String status) {
		this.email = email;
		this.nickname = nickname;
		this.passwordHash = passwordHash;
		this.status = status;
		this.role = "USER";
		this.onboardingCompleted = false;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public long id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String nickname() {
		return nickname;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public String status() {
		return status;
	}

	public String role() {
		return role;
	}

	public boolean onboardingCompleted() {
		return onboardingCompleted;
	}

	public LocalDateTime createdAt() {
		return createdAt;
	}

	public LocalDateTime updatedAt() {
		return updatedAt;
	}
}
