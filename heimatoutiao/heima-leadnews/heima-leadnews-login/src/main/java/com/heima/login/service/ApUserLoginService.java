package com.heima.login.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.pojos.ApUser;

public interface ApUserLoginService {

    /**
     * 根据用户名和密码登录验证
     */
    ResponseResult loginAuth(ApUser user);

    /**
     * 用户登录验证V2
     * @param user
     * @return
     */
    ResponseResult loginAuthV2(ApUser user);
}
