package com.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.app.model.Users;

public interface UserRepository extends JpaRepository<Users, Integer>{
	
	@Query("select s from Users s where s.userName = ?1")
	Users findByUsername(String username);

	@Query("select s from Users s where lower(s.userName) = lower(?1) or lower(s.email) = lower(?1)")
	Users findByUsernameOrEmailIgnoreCase(String login);
	
	Optional<Users> findByEmail(String email);
	
	@Query("select s from Users s where s.userId = ?1")
	Users getUserById(Integer id);
	
	@Query("select s from Users s where s.isEnabled = 0")
	List<Users> findActiveUsers();
	
	@Query("select s from Users s where s.isEnabled = 1")
	List<Users> findInactiveUsers();

}
