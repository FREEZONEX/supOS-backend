package com.supos.common.service;

import com.supos.common.dto.auth.RoleDto;

import java.util.List;

public interface IRoleService {

    List<RoleDto> getRoleListByUserId(String userId);
}
