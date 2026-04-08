package com.ruoyi.web.controller.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
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
    private static final String DEFAULT_WX_PASSWORD = "123456";
    private static final String WX_ACCESS_TOKEN_CACHE_KEY = "wx:access_token";

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
        String phoneCode = body.get("phoneCode");
        String nickName = body.get("nickName");
        String avatar = body.get("avatar");
        if (StringUtils.isEmpty(code))
        {
            return AjaxResult.error("微信登录 code 不能为空");
        }
        if (StringUtils.isEmpty(phoneCode))
        {
            return AjaxResult.error("请先授权手机号");
        }

        String phoneNumber = resolveWxPhoneNumber(phoneCode);
        if (StringUtils.isEmpty(phoneNumber))
        {
            return AjaxResult.error("获取手机号失败，请重试");
        }
        if (StringUtils.isEmpty(nickName))
        {
            nickName = "微信用户";
        }
        nickName = normalizeNickName(nickName);
        avatar = normalizeAvatar(avatar);

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
        Long wxDefaultRoleId = getWxDefaultRoleId();
        boolean created = false;
        if (StringUtils.isNull(user))
        {
            user = new SysUser();
            user.setUserName(generateNextWxUserName());
            user.setNickName(nickName);
            user.setAvatar(avatar);
            if (phoneCanBindToUser(null, phoneNumber))
            {
                user.setPhonenumber(phoneNumber);
            }
            user.setWxOpenId(openId);
            user.setStatus("0");
            user.setPassword(SecurityUtils.encryptPassword(DEFAULT_WX_PASSWORD));
            user.setCreateBy("wxLogin");
            if (wxDefaultRoleId != null)
            {
                user.setRoleIds(new Long[] { wxDefaultRoleId });
            }
            boolean registered = userService.registerUser(user);
            if (!registered)
            {
                return AjaxResult.error("自动注册失败，请联系管理员");
            }
            created = true;
        }
        else
        {
            boolean needUpdate = false;
            // 迁移旧版 wx_ 前缀账号为 yonghuN 格式
            if (user.getUserName() != null && user.getUserName().startsWith("wx_"))
            {
                String newUserName = generateNextWxUserName();
                jdbcTemplate.update("UPDATE sys_user SET user_name = ? WHERE user_id = ?",
                        newUserName, user.getUserId());
                user.setUserName(newUserName);
            }
            if (StringUtils.isNotEmpty(nickName) && !nickName.equals(user.getNickName()))
            {
                user.setNickName(nickName);
                needUpdate = true;
            }
            String currentAvatar = StringUtils.isEmpty(user.getAvatar()) ? "" : user.getAvatar();
            if (!avatar.equals(currentAvatar))
            {
                user.setAvatar(avatar);
                needUpdate = true;
            }
            if (StringUtils.isNotEmpty(phoneNumber) && phoneCanBindToUser(user.getUserId(), phoneNumber)
                    && !phoneNumber.equals(user.getPhonenumber()))
            {
                user.setPhonenumber(phoneNumber);
                needUpdate = true;
            }
            if (needUpdate)
            {
                user.setUpdateBy("wxLogin");
                userService.updateUserProfile(user);
            }
        }
        if (wxDefaultRoleId != null && "wxLogin".equals(user.getCreateBy()))
        {
            ensureWxUserDefaultRole(user.getUserId(), wxDefaultRoleId);
        }

        // 3. 生成 JWT token
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user,
                permissionService.getMenuPermission(user));
        String token = tokenService.createToken(loginUser);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        if (created)
        {
            ajax.put("defaultUserName", user.getUserName());
            ajax.put("defaultPassword", DEFAULT_WX_PASSWORD);
        }
        return ajax;
    }

    private String generateNextWxUserName()
    {
        Integer maxIndex = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(user_name, 7) AS UNSIGNED)), 0) FROM sys_user WHERE user_name REGEXP '^yonghu[0-9]+$'",
                Integer.class);
        int nextIndex = (maxIndex == null ? 0 : maxIndex) + 1;
        String userName = "yonghu" + nextIndex;
        while (StringUtils.isNotNull(userService.selectUserByUserName(userName)))
        {
            nextIndex++;
            userName = "yonghu" + nextIndex;
        }
        return userName;
    }

    private boolean phoneCanBindToUser(Long userId, String phoneNumber)
    {
        if (StringUtils.isEmpty(phoneNumber))
        {
            return false;
        }
        SysUser checkUser = new SysUser();
        checkUser.setUserId(userId);
        checkUser.setPhonenumber(phoneNumber);
        return userService.checkPhoneUnique(checkUser);
    }

    private String resolveWxPhoneNumber(String phoneCode)
    {
        if (StringUtils.isEmpty(phoneCode))
        {
            return null;
        }
        try
        {
            String accessToken = getWxAccessToken();
            String phoneNumber = requestWxPhoneNumber(accessToken, phoneCode);
            if (StringUtils.isNotEmpty(phoneNumber))
            {
                return phoneNumber;
            }
            // access_token 可能失效，清缓存后重试一次
            redisCache.deleteObject(WX_ACCESS_TOKEN_CACHE_KEY);
            accessToken = getWxAccessToken();
            return requestWxPhoneNumber(accessToken, phoneCode);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String getWxAccessToken() throws Exception
    {
        String accessToken = redisCache.getCacheObject(WX_ACCESS_TOKEN_CACHE_KEY);
        if (StringUtils.isNotEmpty(accessToken))
        {
            return accessToken;
        }

        String apiUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + wxAppId + "&secret=" + wxSecret;
        URLConnection conn = new URL(apiUrl).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }
        }

        JsonNode node = new ObjectMapper().readTree(sb.toString());
        if (node.has("errcode") && node.get("errcode").asInt() != 0)
        {
            return null;
        }

        accessToken = node.path("access_token").asText();
        if (StringUtils.isEmpty(accessToken))
        {
            return null;
        }
        int expiresIn = node.path("expires_in").asInt(7200);
        redisCache.setCacheObject(WX_ACCESS_TOKEN_CACHE_KEY, accessToken, Math.max(60, expiresIn - 300), TimeUnit.SECONDS);
        return accessToken;
    }

    private String requestWxPhoneNumber(String accessToken, String phoneCode) throws Exception
    {
        if (StringUtils.isEmpty(accessToken) || StringUtils.isEmpty(phoneCode))
        {
            return null;
        }

        String apiUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        String payload = "{\"code\":\"" + phoneCode + "\"}";
        try (OutputStream os = conn.getOutputStream())
        {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }
        }

        JsonNode node = new ObjectMapper().readTree(sb.toString());
        if (node.has("errcode") && node.get("errcode").asInt() != 0)
        {
            return null;
        }
        String phoneNumber = node.path("phone_info").path("phoneNumber").asText();
        return StringUtils.isEmpty(phoneNumber) ? null : phoneNumber;
    }

    private Long getWxDefaultRoleId()
    {
        try
        {
            return jdbcTemplate.queryForObject(
                    "SELECT role_id FROM sys_role WHERE role_key = 'common' AND status = '0' AND del_flag = '0' LIMIT 1",
                    Long.class);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void ensureWxUserDefaultRole(Long userId, Long roleId)
    {
        if (userId == null || roleId == null)
        {
            return;
        }
        Integer hasRole = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                Integer.class, userId, roleId);
        if (hasRole != null && hasRole > 0)
        {
            return;
        }
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        jdbcTemplate.update("INSERT INTO sys_user_role(user_id, role_id) VALUES(?, ?)", userId, roleId);
    }

    private String normalizeNickName(String nickName)
    {
        if (StringUtils.isEmpty(nickName))
        {
            return nickName;
        }
        return nickName.length() > 30 ? nickName.substring(0, 30) : nickName;
    }

    private String normalizeAvatar(String avatar)
    {
        if (StringUtils.isEmpty(avatar))
        {
            return "";
        }
        return avatar.length() > 255 ? avatar.substring(0, 255) : avatar;
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

        Integer avatarLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'avatar'",
                Integer.class);
        if (avatarLength != null && avatarLength < 255)
        {
            jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN avatar VARCHAR(255) DEFAULT '' COMMENT '头像地址'");
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
