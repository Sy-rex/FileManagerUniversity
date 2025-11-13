package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.entity.FileEntity;
import com.sobolev.spring.filemanageruniversity.entity.Operation;
import com.sobolev.spring.filemanageruniversity.entity.OperationType;
import com.sobolev.spring.filemanageruniversity.entity.User;
import com.sobolev.spring.filemanageruniversity.repository.OperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
public class AuditService {

    private final OperationRepository operationRepository;

    @Autowired
    public AuditService(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    @Transactional
    public void logOperation(User user, OperationType operationType, FileEntity file, String details) {
        Operation operation = new Operation();
        operation.setTimestamp(LocalDateTime.now());
        operation.setOperationType(operationType);
        operation.setUser(user);
        operation.setFile(file);
        operation.setDetails(details);
        operationRepository.save(operation);
    }

    @Transactional
    public void logOperation(User user, OperationType operationType, String details) {
        logOperation(user, operationType, null, details);
    }
}

