package com.supos.common.utils;

import cn.hutool.system.SystemUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.supos.common.vo.UserInfoVo;

public class UserContext {

    private static final TransmittableThreadLocal<UserInfoVo> USER_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置上下文用户信息
     *
     * @param user 用户信息
     */
    public static void set(UserInfoVo user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取上下文用户信息
     */
    public static UserInfoVo get() {
        UserInfoVo userInfo = USER_HOLDER.get();
        String authEnable = SystemUtil.get("SYS_OS_AUTH_ENABLE", "false");
        //免登环境下  mock  guest用户
        if ("false".equals(authEnable) && userInfo == null){
           return UserInfoVo.guest();
        }
        return userInfo;
    }

    /**
     * 清除上下文用户信息
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}