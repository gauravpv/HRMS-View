package com.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.model.RequestResponseLogDetails;

public interface RequestResponseLogDetailsRepo extends JpaRepository<RequestResponseLogDetails, Integer> {

}
