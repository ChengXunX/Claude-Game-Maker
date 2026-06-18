package com.chengxun.gamemaker.web.service;

import com.chengxun.gamemaker.web.entity.Role;
import com.chengxun.gamemaker.web.entity.User;
import com.chengxun.gamemaker.web.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final SystemConfigService configService;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RoleService roleService, SystemConfigService configService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.configService = configService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() != User.UserStatus.APPROVED) {
            // 根据不同状态抛出不同的错误信息，便于前端展示具体原因
            String statusMsg = switch (user.getStatus()) {
                case PENDING -> "PENDING";
                case REJECTED -> "REJECTED";
                case DISABLED -> "DISABLED";
                default -> "NOT_APPROVED";
            };
            throw new UsernameNotFoundException(statusMsg + ":" + username);
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

            // 如果有通配符权限 *，添加所有具体权限
            if (permissions.contains("*")) {
                addAllPermissions(authorities);
            }
        }

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }

    /**
     * 添加所有具体权限
     * 当用户拥有通配符权限 * 时，需要添加所有具体权限以支持 hasAuthority 检查
     * 从数据库读取所有角色的权限，确保管理员拥有所有已定义的权限
     */
    private void addAllPermissions(List<SimpleGrantedAuthority> authorities) {
        // 从数据库读取所有角色的权限集合
        java.util.Set<String> allPerms = roleService.getAllPermissionsFromDatabase();
        for (String perm : allPerms) {
            String auth = "PERM_" + perm;
            if (!authorities.contains(new SimpleGrantedAuthority(auth))) {
                authorities.add(new SimpleGrantedAuthority(auth));
            }
        }
    }

    public User register(String username, String password, String email, String nickname) {
        // 验证用户名格式
        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new RuntimeException("用户名长度必须在3-50个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("用户名只能包含字母、数字和下划线");
        }

        // 验证密码强度（至少8位，包含大小写字母和数字）
        if (password == null || password.length() < 8) {
            throw new RuntimeException("密码长度不能少于8个字符");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("密码必须包含至少一个大写字母");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("密码必须包含至少一个小写字母");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("密码必须包含至少一个数字");
        }

        // 验证邮箱格式（如果提供）
        if (email != null && !email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 从配置中心读取默认注册角色，管理员可在配置中心修改
        String defaultRoleName = configService.getString("user.default.role", "USER");
        Role defaultRole = roleService.getRoleByName(defaultRoleName);
        if (defaultRole == null) {
            // 回退到USER角色
            defaultRole = roleService.getRoleByName("USER");
            if (defaultRole == null) {
                throw new RuntimeException("默认角色不存在，请联系管理员");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(defaultRole);
        user.setStatus(User.UserStatus.PENDING);

        User saved = userRepository.save(user);
        log.info("User registered: {} (status: PENDING, role: {})", username, defaultRole.getName());
        return saved;
    }

    /**
     * 审批用户
     * 审批成功后清除用户缓存
     *
     * @param userId 用户ID
     * @return 审批后的用户
     * @throws RuntimeException 当用户不存在时抛出
     */
    @CacheEvict(value = "users", allEntries = true)
    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.APPROVED);
        User saved = userRepository.save(user);
        log.info("User approved: {}", user.getUsername());
        return saved;
    }

    /**
     * 拒绝用户
     * 拒绝后清除用户缓存
     *
     * @param userId 用户ID
     * @return 拒绝后的用户
     * @throws RuntimeException 当用户不存在时抛出
     */
    @CacheEvict(value = "users", allEntries = true)
    public User rejectUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(User.UserStatus.REJECTED);
        User saved = userRepository.save(user);
        log.info("User rejected: {}", user.getUsername());
        return saved;
    }

    /**
     * 禁用用户
     * 禁用后清除用户缓存
     *
     * @param userId 用户ID
     * @return 禁用后的用户
     * @throws RuntimeException 当用户不存在时抛出
     */
    @CacheEvict(value = "users", allEntries = true)
    public User disableUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != null && "ADMIN".equals(user.getRole().getName())) {
            throw new RuntimeException("管理员账号不可禁用");
        }
        user.setStatus(User.UserStatus.DISABLED);
        User saved = userRepository.save(user);
        log.info("User disabled: {}", user.getUsername());
        return saved;
    }

    /**
     * 更新用户角色
     * 更新后清除用户缓存
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 更新后的用户
     * @throws RuntimeException 当用户或角色不存在时抛出
     */
    @CacheEvict(value = "users", allEntries = true)
    public User updateUserRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != null && "ADMIN".equals(user.getRole().getName())) {
            throw new RuntimeException("管理员账号不可修改角色");
        }

        Role role = roleService.getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        log.info("User {} role updated to {}", user.getUsername(), role.getName());
        return saved;
    }

    /**
     * 根据用户名获取用户
     * 使用缓存提高查询效率
     *
     * @param username 用户名
     * @return 用户信息，不存在返回null
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#username")
    public User getUserByUsername(String username) {
        User user = userRepository.findByUsernameWithRole(username).orElse(null);
        if (user != null) {
            // 强制初始化懒加载的代理对象，确保缓存序列化时不会出错
            Hibernate.initialize(user.getRole());
            if (user.getRole() != null) {
                Hibernate.initialize(user.getRole().getPermissions());
                // 创建一个新的Set来存储permissions，避免代理对象问题
                user.getRole().setPermissions(new java.util.HashSet<>(user.getRole().getPermissions()));
            }
        }
        return user;
    }

    /**
     * 根据ID获取用户
     * 使用缓存提高查询效率
     *
     * @param id 用户ID
     * @return 用户信息，不存在返回null
     */
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findByIdWithRole(id).orElse(null);
    }

    /**
     * 获取第一个管理员用户
     */
    public User getFirstAdmin() {
        return userRepository.findFirstAdmin();
    }

    /**
     * 更新用户资料
     * 更新后清除用户缓存
     *
     * @param userId 用户ID
     * @param nickname 昵称
     * @param email 邮箱
     * @param avatar 头像URL
     * @return 更新后的用户
     * @throws RuntimeException 当用户不存在时抛出
     */
    @CacheEvict(value = "users", allEntries = true)
    public User updateProfile(Long userId, String nickname, String email, String avatar) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        User saved = userRepository.save(user);
        log.info("User profile updated: {}", user.getUsername());
        return saved;
    }

    @CacheEvict(value = "users", allEntries = true)
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("当前密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * 保存用户信息
     */
    @CacheEvict(value = "users", allEntries = true)
    public User saveUser(User user) {
        return userRepository.save(user);
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

    /**
     * 生成随机密码（16位，包含大小写字母、数字和特殊字符）
     */
    public static String generateRandomPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;
        SecureRandom random = new SecureRandom();

        // 确保至少包含每种字符
        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // 填充剩余长度
        for (int i = 4; i < 16; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        // 打乱顺序
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * 管理员创建用户
     * 管理员可以直接创建已批准的用户，并指定角色
     *
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param nickname 昵称
     * @param roleId 角色ID
     * @return 创建的用户
     */
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(String username, String password, String email, String nickname, Long roleId) {
        // 验证用户名
        if (username == null || username.length() < 3 || username.length() > 50) {
            throw new RuntimeException("用户名长度必须在3-50个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("用户名只能包含字母、数字和下划线");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 验证密码
        if (password == null || password.length() < 8) {
            throw new RuntimeException("密码长度不能少于8个字符");
        }

        // 验证邮箱（如果提供）
        if (email != null && !email.isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
        }

        // 获取角色
        Role role;
        if (roleId != null) {
            role = roleService.getRoleById(roleId);
            if (role == null) {
                throw new RuntimeException("指定的角色不存在");
            }
        } else {
            role = roleService.getRoleByName("USER");
            if (role == null) {
                throw new RuntimeException("默认角色不存在");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(role);
        user.setStatus(User.UserStatus.APPROVED); // 管理员创建的用户直接批准
        user.setMustChangePassword(false);

        User saved = userRepository.save(user);
        log.info("User created by admin: {} (role: {})", username, role.getName());
        return saved;
    }

    /**
     * 管理员更新用户信息
     *
     * @param userId 用户ID
     * @param updates 更新字段
     * @return 更新后的用户
     */
    @CacheEvict(value = "users", allEntries = true)
    public User updateUser(Long userId, Map<String, Object> updates) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (updates.containsKey("nickname")) {
            user.setNickname((String) updates.get("nickname"));
        }
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("roleId")) {
            Long roleId = ((Number) updates.get("roleId")).longValue();
            Role role = roleService.getRoleById(roleId);
            if (role != null) {
                user.setRole(role);
            }
        }
        if (updates.containsKey("password")) {
            String password = (String) updates.get("password");
            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
            }
        }

        User saved = userRepository.save(user);
        log.info("User updated: {}", user.getUsername());
        return saved;
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    /**
     * 初始化默认管理员账号
     * 首次启动时自动创建管理员，密码随机生成并打印到日志，首次登录必须修改密码
     */
    public String initAdminUser() {
        if (userRepository.count() == 0) {
            Role adminRole = roleService.getRoleByName("ADMIN");
            if (adminRole == null) {
                log.error("ADMIN role not found, cannot create admin user");
                return null;
            }

            String randomPassword = generateRandomPassword();

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(randomPassword));
            admin.setEmail("admin@game-maker.com");
            admin.setNickname("管理员");
            admin.setRole(adminRole);
            admin.setStatus(User.UserStatus.APPROVED);
            admin.setMustChangePassword(true); // 首次登录必须修改密码
            userRepository.save(admin);

            log.warn("==========================================================");
            log.warn("默认管理员账号已创建:");
            log.warn("  用户名: admin");
            log.warn("  密码: {}", randomPassword);
            log.warn("  首次登录后必须修改密码！");
            log.warn("==========================================================");

            return randomPassword;
        }
        return null;
    }
}
