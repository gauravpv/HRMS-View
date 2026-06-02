package com.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(catalog = "decision_rules_hrmsbre", name = "table_details")
public class TableDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="ID")
	private Integer id;
	@Column(name="TABLE_NAME", length=100)
	private String tableName;
	@Column(name="COLUMN_DETAIL", length=1000)
	private String columnDetail;
	@Column(name="TABLE_TYPE", length=100)
	private String tableType;
	@Column(name="MAIN_TABLE_NAME", length=100)
	private String mainTableName;

}
