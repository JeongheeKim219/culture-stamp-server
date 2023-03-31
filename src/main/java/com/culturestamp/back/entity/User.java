package com.culturestamp.back.entity;

import com.culturestamp.back.dto.UserServiceResponse;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Getter
@Setter
public class User extends BaseTimeEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // TODO: UNIQUE 설정. email을 의미할 수도 있음
    // @Column(length=64,nullable = true)
    private String loginId;

    @Column
    private String nickname;

    // TODO: added, needed to explain
    @Column
    private String providerType;

    @Column(nullable = false)
    private String email;

    // TODO: edited, needed to explain
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // TODO: 변경 설명(RESPONSE 까지)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginAt;

    @Column
    private int failCount;

//    @Builder
//    public User(String nickname, String loginId, ProviderType providerType, String email, String password, RoleType role, LocalDateTime lastLoginAt, int failCount) {
//        this.nickname = nickname;
//        this.loginId = loginId;
//        this.providerType = providerType;
//        //  this.providerId = providerId;
//        this.email = email;
//        this.password = password;
//        this.role = role;
//        this.lastLoginAt = lastLoginAt;
//        this.failCount = failCount;
//    }


    public User(UserServiceResponse userServiceResponse) {
        this.userId = userServiceResponse.getUserId();
        this.role = Role.USER;
        this.loginId = userServiceResponse.getLoginId();
        this.nickname = userServiceResponse.getNickname();
        this.providerType = userServiceResponse.getProviderType();
        this.email = userServiceResponse.getEmail();
        this.password = userServiceResponse.getPassword();
        this.lastLoginAt = userServiceResponse.getLastLoginAt();
        this.failCount = userServiceResponse.getFailCount();
    }


    public User(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        this.role = Role.USER;
    }


    public User update(String name) {
        this.nickname = name;
        return this;
    }

    public String getRole() {
        return this.role.getKey();
    }

}
