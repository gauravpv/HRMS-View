package com.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.app.model.Roles;

public interface RoleRepository extends JpaRepository<Roles, Integer>{
	
    @Query(value = "SELECT R.NAME\r\n"
    		+ "FROM roles R\r\n"
    		+ "JOIN users_roles UR ON R.ROLE_ID = UR.ROLE_ID\r\n"
    		+ "JOIN users U ON UR.USER_ID = U.USER_ID\r\n"
    		+ "WHERE U.USER_ID = :userId", nativeQuery = true)
    List<String> findRolebyUserId(@Param("userId") Integer userId);

}
