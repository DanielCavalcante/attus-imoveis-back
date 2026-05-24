package com.example.demo.dtos;

public record UserDetailDTO (
    Long id,
    String fullname,
    String email,
    String phone,
    String photo
) {
    
}
