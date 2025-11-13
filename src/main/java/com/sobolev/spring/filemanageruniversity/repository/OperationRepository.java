package com.sobolev.spring.filemanageruniversity.repository;

import com.sobolev.spring.filemanageruniversity.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {
}
