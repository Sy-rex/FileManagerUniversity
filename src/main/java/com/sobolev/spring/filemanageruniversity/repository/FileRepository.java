package com.sobolev.spring.filemanageruniversity.repository;

import com.sobolev.spring.filemanageruniversity.entity.FileEntity;
import com.sobolev.spring.filemanageruniversity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByLocation(String location);
    List<FileEntity> findByOwner(User owner);
}
