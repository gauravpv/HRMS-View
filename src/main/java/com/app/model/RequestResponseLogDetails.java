package com.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "REQUEST_RESPONSE_LOG_DETAILS")
public class RequestResponseLogDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;
    @Column(name = "PREVIOUS_STATE")
    private String previousState;
    @Column(name = "NEW_STATE")
    private String newState; 
    @Column(name = "TABLE_NAME")
    private String tableName;
    @Column(name = "UPDATED_BY")
    private String updatedBy;
    @Column(name = "ACTION")
    private String action;
    
    
	public RequestResponseLogDetails(String previousState, String newState, String tableName, String updatedBy,
			String action) {
		super();
		this.previousState = previousState;
		this.newState = newState;
		this.tableName = tableName;
		this.updatedBy = updatedBy;
		this.action = action;
	}
      
    
    
    
}
