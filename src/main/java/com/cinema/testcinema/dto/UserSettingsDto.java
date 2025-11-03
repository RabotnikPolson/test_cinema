// src/main/java/com/cinema/testcinema/dto/UserSettingsDto.java
package com.cinema.testcinema.dto;

import java.util.Map;

public class UserSettingsDto {
    private Long userId;
    private Map<String, Object> data;

    public UserSettingsDto() { }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
