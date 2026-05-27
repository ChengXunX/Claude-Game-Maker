package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() != User.UserStatus.APPROVED) {
            throw new UsernameNotFoundException("User not approved: " + username);
        }

        // 构建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));

            // 添加具体权限
            Set<String> permissions = user.getRole().getPermissions();
            for (String perm : permissions) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
            }
        }

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }

    public User register(String username, String password, String email, String nickname) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        Role defaultRole = roleService.getRoleByName("USER");
        if (defaultRole == null) {
            throw new RuntimeException("默认角色不存在，请联系管理员");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(defaultRole);
        user.setStatus(User.UserStatus.PENDING);

        User saved = userRepository.save(user);
        log.info("User registered: {} (status: PENDING, role: USER)", username);
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

    public User updateUserRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleService.getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        log.info("User {} role updated to {}", user.getUsername(), role.getName());
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
            Role adminRole = roleService.getRoleByName("ADMIN");
            if (adminRole == null) {
                log.error("ADMIN role not found, cannot create admin user");
                return;
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@game-maker.com");
            admin.setNickname("管理员");
            admin.setRole(adminRole);
            admin.setStatus(User.UserStatus.APPROVED);
            userRepository.save(admin);
            log.info("Default admin user created (username: admin, password: admin123)");
        }
    }
}
