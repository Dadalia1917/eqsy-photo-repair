package com.ruoyi.web.controller.system;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    private static final String SMS_CODE_KEY = "sms_codes:";

    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }

        if (StringUtils.isEmpty(user.getPhonenumber()) || user.getPhonenumber().length() != 11)
        {
            return error("请输入正确的11位手机号");
        }
        if (StringUtils.isEmpty(user.getSmsCode()))
        {
            return error("请输入短信验证码");
        }

        String smsCode = redisCache.getCacheObject(SMS_CODE_KEY + user.getPhonenumber());
        if (StringUtils.isEmpty(smsCode))
        {
            return error("短信验证码已过期，请重新获取");
        }
        if (!Objects.equals(smsCode, user.getSmsCode()))
        {
            return error("短信验证码错误");
        }
        redisCache.deleteObject(SMS_CODE_KEY + user.getPhonenumber());

        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
