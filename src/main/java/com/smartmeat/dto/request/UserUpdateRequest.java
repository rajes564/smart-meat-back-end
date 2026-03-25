package com.smartmeat.dto.request;


import lombok.*;

@Data public class UserUpdateRequest {
    String name;
    String email;
    String password;
    boolean active;
}
