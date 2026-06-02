package com.app.model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data
@Table(name="users")
public class Users {
	
	@Id
	@Column(name ="USER_ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer userId;
	
	@Column(name="USER_NAME", length=100)
	private String userName;
	
	@Column(name="PASSWORD", length=100)
	private String password;
	
	@Column(name="EMAIL", length=100)
	private String email;
	
	@Column(name="IS_ENABLED")
	private Integer isEnabled;
	
	@Column(name="IS_ACTIVE")
	private Integer isActive;
	
	@Column(name="LAST_LOGIN_TIME")
	private Timestamp lastLoginTime;
	
	@Column(name="SESSION_ID")
	private String sessionId;
	
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "USERS_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID")
            )
    private Set<Roles> roles = new HashSet<>();

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Integer isEnabled) {
		this.isEnabled = isEnabled;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public Timestamp getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Timestamp lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Set<Roles> getRoles() {
		return roles;
	}

	public void setRoles(Set<Roles> roles) {
		this.roles = roles;
	}

	public Users(Integer userId, String userName, String password, String email, Integer isEnabled, Integer isActive,
			Timestamp lastLoginTime, String sessionId, Set<Roles> roles) {
		super();
		this.userId = userId;
		this.userName = userName;
		this.password = password;
		this.email = email;
		this.isEnabled = isEnabled;
		this.isActive = isActive;
		this.lastLoginTime = lastLoginTime;
		this.sessionId = sessionId;
		this.roles = roles;
	}

}
