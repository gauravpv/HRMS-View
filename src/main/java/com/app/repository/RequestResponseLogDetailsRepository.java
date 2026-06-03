package com.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.model.RequestResponseLogDetails;

import java.util.Collection;
import java.util.List;

public interface RequestResponseLogDetailsRepository extends JpaRepository<RequestResponseLogDetails, Integer> {

    List<RequestResponseLogDetails> findTop100ByActionInOrderByIdDesc(Collection<String> actions);
}
