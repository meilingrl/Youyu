package com.campusmarket.backend.service.auth.impl;

import com.campusmarket.backend.common.api.ResultCode;
import com.campusmarket.backend.common.auth.AuthContextHolder;
import com.campusmarket.backend.common.auth.AuthUser;
import com.campusmarket.backend.common.exception.BusinessException;
import com.campusmarket.backend.controller.auth.dto.RegisterRequest;
import com.campusmarket.backend.mapper.user.UserMapper;
import com.campusmarket.backend.service.auth.AuthService;
import com.campusmarket.backend.service.auth.AuthTokenService;
import com.campusmarket.backend.service.auth.PasswordService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthTokenService authTokenService;
    private final UserMapper userMapper;
    private final PasswordService passwordService;

    public AuthServiceImpl(AuthTokenService authTokenService,
                           UserMapper userMapper,
                           PasswordService passwordService) {
        this.authTokenService = authTokenService;
        this.userMapper = userMapper;
        this.passwordService = passwordService;
    }

    @Override
    public Map<String, Object> unifiedLogin(String loginId, String password) {
        String idTrim = loginId == null ? "" : loginId.trim();
        if (idTrim.isEmpty() || password == null || password.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号和密码不能为空");
        }

        Optional<Map<String, Object>> userOpt = userMapper.findByLoginId(idTrim);
        if (userOpt.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }

        Map<String, Object> user = userOpt.get();
        Long uid = toLong(user.get("id"));
        String storedHash = Objects.toString(user.get("passwordHash"), Objects.toString(user.get("password"), ""));
        if (!passwordService.verifyAndMigrate(password, storedHash, uid, userMapper)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }
        if ("disabled".equalsIgnoreCase(String.valueOf(user.get("status")))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已禁用，请联系管理员");
        }
        userMapper.updateLastLoginAt(uid);
        Map<String, Object> userBrief = new LinkedHashMap<>();
        userBrief.put("id", String.valueOf(uid));
        userBrief.put("loginId", Objects.toString(user.get("username"), idTrim));
        userBrief.put("nickname", Objects.toString(user.get("nickname"), "用户"));
        userBrief.put("verificationStatus", Objects.toString(user.get("verificationStatus"), "unverified"));
        userBrief.put("privilege", userMapper.findPrivilegeProfile(uid).orElseGet(Map::of));

        Map<String, Object> result = new LinkedHashMap<>();
        String role = String.valueOf(user.getOrDefault("role", "USER"));
        result.put("token", authTokenService.generate(uid, role));
        result.put("role", role.toLowerCase());
        result.put("user", userBrief);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String nickname = trim(request.getNickname());
        String password = request.getPassword();
        if (isBlank(username) || isBlank(password) || isBlank(nickname)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名、密码和昵称不能为空");
        }
        if (userMapper.findByLoginId(username).isPresent() || (!isBlank(email) && userMapper.findByLoginId(email).isPresent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱已被使用");
        }

        try {
            Long userId = userMapper.insert(username, phone, email, passwordService.encode(password), nickname);
            userMapper.insertDefaultPrivilegeProfile(userId);
            Map<String, Object> user = userMapper.findById(userId)
                    .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "注册后读取用户失败"));
            return linkedMap(
                    "token", authTokenService.generate(userId, "USER"),
                    "role", "user",
                    "user", publicUserBrief(user),
                    "privilege", userMapper.findPrivilegeProfile(userId).orElseGet(Map::of)
            );
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或邮箱已被使用");
        }
    }

    private static Long toLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(raw));
    }

    @Override
    public Map<String, Object> currentUser() {
        AuthUser authUser = AuthContextHolder.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", authUser == null ? null : authUser.getUserId());
        result.put("role", authUser == null ? null : authUser.getRole());
        return result;
    }

    private Map<String, Object> publicUserBrief(Map<String, Object> user) {
        return linkedMap(
                "id", String.valueOf(user.get("id")),
                "loginId", user.get("username"),
                "nickname", user.get("nickname"),
                "verificationStatus", user.get("verificationStatus")
        );
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
