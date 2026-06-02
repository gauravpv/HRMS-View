package com.app.model;


import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
 
@Entity
@Table(name = "roles")
@Data
public class Roles {
    @Id
    @Column(name = "ROLE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roleId;
    
    @Column(name = "NAME")
    private String roleName;
    
    @ManyToMany(mappedBy = "roles")
    private Set<Users> users = new HashSet<Users>();
	  
        
}