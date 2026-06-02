package com.app.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.model.RequestResponseLogDetails;
import com.app.repository.RequestResponseLogDetailsRepo;
import com.app.service.RequestResponseLogDetailsService;

@Service
@Transactional
public class RequestResponseLogDetailsServiceImpl implements RequestResponseLogDetailsService{
    
    @Autowired
    private RequestResponseLogDetailsRepo logRepo;

    @Override
    public RequestResponseLogDetails saveLog(RequestResponseLogDetails log) {
        return logRepo.save(log);
    }

}
