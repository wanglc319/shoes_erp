package com.shoerp.dto;

import java.util.List;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private List<String> menus;

    public LoginResponse(String token, Long userId, String username, String realName, String role,
                         List<String> menus) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.menus = menus;
    }
}
