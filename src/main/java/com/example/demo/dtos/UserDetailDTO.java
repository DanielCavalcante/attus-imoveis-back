package com.example.demo.dtos;

import com.example.demo.enums.AccountStatus;
import com.example.demo.enums.AccountType;

public record UserDetailDTO (
    Long id,
    String fullname,
    String email,
    String phone,
    String photo,
    AccountType accountType,
    AccountStatus accountStatus,
    String cnpj
) {

}
