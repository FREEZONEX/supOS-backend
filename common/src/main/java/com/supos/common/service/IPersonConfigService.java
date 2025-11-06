package com.supos.common.service;

import com.supos.common.vo.PersonConfigVo;

public interface IPersonConfigService {

    PersonConfigVo getByUserId(String userId);
}
