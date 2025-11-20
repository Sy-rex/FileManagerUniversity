package com.sobolev.spring.filemanageruniversity.service;

import com.sobolev.spring.filemanageruniversity.config.FileManagerConstants;
import com.sobolev.spring.filemanageruniversity.entity.User;
import com.sobolev.spring.filemanageruniversity.exception.ValidationException;
import com.sobolev.spring.filemanageruniversity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Имя пользователя не может быть пустым");
        }
        if (username.trim().length() < FileManagerConstants.MIN_USERNAME_LENGTH) {
            throw new ValidationException("Имя пользователя должно содержать минимум " 
                + FileManagerConstants.MIN_USERNAME_LENGTH + " символа");
        }
        if (username.trim().length() > FileManagerConstants.MAX_USERNAME_LENGTH) {
            throw new ValidationException("Имя пользователя не должно превышать " 
                + FileManagerConstants.MAX_USERNAME_LENGTH + " символов");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Пароль не может быть пустым");
        }
        if (password.length() < FileManagerConstants.MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Пароль должен содержать минимум " 
                + FileManagerConstants.MIN_PASSWORD_LENGTH + " символ");
        }

        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            throw new ValidationException("Пользователь с таким именем уже существует");
        }

        String passwordHash = passwordEncoder.encode(password);

        User user = new User(username.trim(), passwordHash);
        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}

