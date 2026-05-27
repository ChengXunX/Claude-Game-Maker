package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() != User.UserStatus.APPROVED) {
            throw new UsernameNotFoundException("User not approved: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public User register(String username, String password, String email, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.PENDING);

        User saved = userRepository.save(user);
        log.info("User registered: {} (status: PENDING)", username);
        return saved;
    }

    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.APPROVED);
        User saved = userRepository.save(user);
        log.info("User approved: {}", user.getUsername());
        return saved;
    }

    public User rejectUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.REJECTED);
        User saved = userRepository.save(user);
        log.info("User rejected: {}", user.getUsername());
        return saved;
    }

    public User disableUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.DISABLED);
        User saved = userRepository.save(user);
        log.info("User disabled: {}", user.getUsername());
        return saved;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getPendingUsers() {
        return userRepository.findByStatus(User.UserStatus.PENDING);
    }

    public long getPendingCount() {
        return userRepository.countByStatus(User.UserStatus.PENDING);
    }

    public void initAdminUser() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@game-maker.com");
            admin.setNickname("管理员");
            admin.setRole(User.UserRole.ADMIN);
            admin.setStatus(User.UserStatus.APPROVED);
            userRepository.save(admin);
            log.info("Default admin user created (username: admin, password: admin123)");
        }
    }
}
