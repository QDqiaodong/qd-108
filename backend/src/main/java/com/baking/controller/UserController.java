package com.baking.controller;

import com.baking.common.Result;
import com.baking.entity.User;
import com.baking.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        try {
            User registered = userService.register(user);
            registered.setPassword(null);
            return Result.success(registered);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<User> updateUser(@RequestBody User user) {
        User updated = userService.updateUser(user);
        updated.setPassword(null);
        return Result.success(updated);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
    }
}
