package com.app.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.model.RequestResponseLogDetails;
import com.app.repository.RequestResponseLogDetailsRepository;
import com.app.service.RequestResponseLogDetailsService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RequestResponseLogDetailsServiceImpl implements RequestResponseLogDetailsService {

    private final RequestResponseLogDetailsRepository logRepo;

    @Override
    public RequestResponseLogDetails saveLog(RequestResponseLogDetails log) {
        return logRepo.save(log);
    }

}
