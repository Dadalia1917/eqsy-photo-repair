package com.ruoyi.web.controller.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 登录验证
 * 
 * @author ruoyi
 */
@RestController
public class SysLoginController
{
    private static final String SMS_CODE_KEY = "sms_codes:";

    @Value("${wechat.appid}")
    private String wxAppId;

    @Value("${wechat.secret}")
    private String wxSecret;

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 发送短信验证码（开发环境模拟发送）
     */
    @PostMapping("/sendSmsCode")
    public AjaxResult sendSmsCode(@RequestBody Map<String, String> body)
    {
        String phone = body.get("phone");
        if (StringUtils.isEmpty(phone) || phone.length() != 11)
        {
            return AjaxResult.error("请输入正确的11位手机号");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        redisCache.setCacheObject(SMS_CODE_KEY + phone, code, 5, TimeUnit.MINUTES);

        AjaxResult ajax = AjaxResult.success("验证码发送成功");
        // 社区本地部署阶段，直接回传验证码方便调试；上线后请删除此字段。
        ajax.put("smsCode", code);
        return ajax;
    }

    /**
     * 短信验证码登录
     */
    @PostMapping("/smsLogin")
    public AjaxResult smsLogin(@RequestBody Map<String, String> body)
    {
        String phone = body.get("phone");
        String smsCode = body.get("smsCode");

        if (StringUtils.isEmpty(phone) || phone.length() != 11)
        {
            return AjaxResult.error("请输入正确的11位手机号");
        }
        if (StringUtils.isEmpty(smsCode))
        {
            return AjaxResult.error("请输入短信验证码");
        }

        String cacheCode = redisCache.getCacheObject(SMS_CODE_KEY + phone);
        if (StringUtils.isEmpty(cacheCode))
        {
            return AjaxResult.error("短信验证码已过期，请重新获取");
        }
        if (!Objects.equals(cacheCode, smsCode))
        {
            return AjaxResult.error("短信验证码错误");
        }
        redisCache.deleteObject(SMS_CODE_KEY + phone);

        SysUser user = userService.selectUserByUserName(phone);
        if (StringUtils.isNull(user))
        {
            return AjaxResult.error("该手机号尚未注册，请先注册");
        }

        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
        String token = tokenService.createToken(loginUser);

        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
    
    /**
     * 微信小程序一键登录（code2session），未注册用户自动创建账号
     */
    @PostMapping("/wxLogin")
    public AjaxResult wxLogin(@RequestBody Map<String, String> body)
    {
        try
        {
            ensureWxOpenIdColumn();
        }
        catch (Exception e)
        {
            return AjaxResult.error("数据库缺少微信登录字段，且自动修复失败，请联系管理员");
        }

        String code = body.get("code");
        if (StringUtils.isEmpty(code))
        {
            return AjaxResult.error("微信登录 code 不能为空");
        }

        // 1. 调用微信 code2session 接口获取 openid
        String openId;
        try
        {
            String apiUrl = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppId
                    + "&secret=" + wxSecret
                    + "&js_code=" + code
                    + "&grant_type=authorization_code";
            URLConnection conn = new URL(apiUrl).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")))
            {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            JsonNode node = new ObjectMapper().readTree(sb.toString());
            if (node.has("errcode") && node.get("errcode").asInt() != 0)
            {
                return AjaxResult.error("微信授权失败: " + node.path("errmsg").asText());
            }
            openId = node.path("openid").asText();
            if (StringUtils.isEmpty(openId))
            {
                return AjaxResult.error("获取微信 openid 失败");
            }
        }
        catch (Exception e)
        {
            return AjaxResult.error("请求微信服务失败，请稍后重试");
        }

        // 2. 查找或自动注册用户
        SysUser user = userService.selectUserByWxOpenId(openId);
        if (StringUtils.isNull(user))
        {
            user = new SysUser();
            user.setUserName("wx_" + openId.substring(0, Math.min(openId.length(), 20)));
            user.setNickName("微信用户");
            user.setWxOpenId(openId);
            user.setStatus("0");
            user.setPassword(SecurityUtils.encryptPassword(UUID.randomUUID().toString()));
            user.setCreateBy("wxLogin");
            boolean registered = userService.registerUser(user);
            if (!registered)
            {
                return AjaxResult.error("自动注册失败，请联系管理员");
            }
        }

        // 3. 生成 JWT token
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                permissionService.getMenuPermission(user));
        String token = tokenService.createToken(loginUser);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    private void ensureWxOpenIdColumn()
    {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'wx_openid'",
                Integer.class);
        if (columnCount != null && columnCount == 0)
        {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN wx_openid VARCHAR(64) DEFAULT NULL COMMENT '微信OpenID'");
        }

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'idx_sys_user_wx_openid'",
                Integer.class);
        if (indexCount != null && indexCount == 0)
        {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD UNIQUE INDEX idx_sys_user_wx_openid(wx_openid)");
        }
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
