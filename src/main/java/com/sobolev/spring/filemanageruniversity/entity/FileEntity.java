package com.sobolev.spring.filemanageruniversity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя файла не может быть пустым")
    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PositiveOrZero(message = "Размер файла не может быть отрицательным")
    @Column(name = "size")
    private Long size;

    @NotBlank(message = "Расположение файла не может быть пустым")
    @Column(name = "location", nullable = false)
    private String location;

    @NotNull(message = "Владелец файла не может быть null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @Column(name = "checksum")
    private String checksum;

    public FileEntity(String filename, String location, User owner) {
        this.filename = filename;
        this.location = location;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }
}
