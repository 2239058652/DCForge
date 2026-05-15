package com.forge.dc.modules.users.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserListVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer status;

    private List<String> roles;

    private LocalDateTime createdAt;
}
