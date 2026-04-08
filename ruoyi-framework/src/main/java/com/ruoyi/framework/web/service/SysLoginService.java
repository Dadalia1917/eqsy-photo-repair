package com.ruoyi.framework.web.service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.BlackListException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserNotExistsException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 统一收敛 common 角色菜单范围，避免误开放系统管理相关权限
        ensureCommonRoleMenuScope();
        // 用户验证
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        correctWebRegisterRole(loginUser);
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    private void ensureCommonRoleMenuScope()
    {
        try
        {
            Long commonRoleId = jdbcTemplate.queryForObject(
                    "SELECT role_id FROM sys_role WHERE role_key = 'common' AND status = '0' AND del_flag = '0' LIMIT 1",
                    Long.class);
            if (commonRoleId == null)
            {
                return;
            }

            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ?",
                    Integer.class, commonRoleId);
            Integer outOfScopeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id NOT IN (2000, 2001, 2002, 2003)",
                    Integer.class, commonRoleId);

            if ((totalCount != null && totalCount == 4) && (outOfScopeCount == null || outOfScopeCount == 0))
            {
                return;
            }

            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", commonRoleId);
            jdbcTemplate.update("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(?, 2000)", commonRoleId);
            jdbcTemplate.update("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(?, 2001)", commonRoleId);
            jdbcTemplate.update("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(?, 2002)", commonRoleId);
            jdbcTemplate.update("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(?, 2003)", commonRoleId);
        }
        catch (Exception ignored)
        {
        }
    }

    private void correctWebRegisterRole(LoginUser loginUser)
    {
        try
        {
            SysUser user = loginUser.getUser();
            if (user == null || user.getUserId() == null)
            {
                return;
            }
            if ("wxLogin".equals(user.getCreateBy()) || "admin".equals(user.getUserName()))
            {
                return;
            }

            Integer hasCommonRole = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.role_id WHERE ur.user_id = ? AND r.role_key = 'common' AND r.del_flag = '0'",
                    Integer.class, user.getUserId());
            Integer hasStudentRole = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.role_id WHERE ur.user_id = ? AND r.role_key = 'repair_student' AND r.del_flag = '0'",
                    Integer.class, user.getUserId());

            if (hasCommonRole != null && hasCommonRole > 0 && (hasStudentRole == null || hasStudentRole == 0))
            {
                Long studentRoleId = jdbcTemplate.queryForObject(
                        "SELECT role_id FROM sys_role WHERE role_key = 'repair_student' AND status = '0' AND del_flag = '0' LIMIT 1",
                        Long.class);
                if (studentRoleId != null)
                {
                    jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", user.getUserId());
                    jdbcTemplate.update("INSERT INTO sys_user_role(user_id, role_id) VALUES(?, ?)", user.getUserId(), studentRoleId);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }
}
