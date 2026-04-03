package com.example.project_backend04.dto.response.Membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipUserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatar;
}
