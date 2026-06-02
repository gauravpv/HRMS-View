package com.app.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.app.model.Users;

public interface UserRepository extends JpaRepository<Users, Integer>{
	
	@Query("select s from Users s where s.userName = ?1")
	Users findByUsername(String username);
	
	Optional<Users> findByEmail(String email);
	
	@Query("select s from Users s where s.userId = ?1")
	Users getUserById(Integer id);
	
	@Query("select s from Users s where s.isEnabled = 0")
	List<Users> activeUsers();
	
	@Query("select s from Users s where s.isEnabled = 1")
	List<Users> inActiveUsers();
	
	@Transactional
    @Modifying
    @Query("UPDATE Users u SET u.isActive = 1 WHERE u.lastLoginTime < :cutoffTime")
    void updateActiveStatusForInactiveUsers(Timestamp cutoffTime);

}
