package com.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.model.RequestResponseLogDetails;

import java.util.List;

public interface RequestResponseLogDetailsRepository extends JpaRepository<RequestResponseLogDetails, Integer> {

    List<RequestResponseLogDetails> findTop10ByOrderByIdDesc();
}
