package com.howei.shiroadmin.config.shiro;

import com.howei.shiroadmin.model.Permission;
import com.howei.shiroadmin.model.Role;
import com.howei.shiroadmin.model.User;
import com.howei.shiroadmin.service.PermissionService;
import com.howei.shiroadmin.service.RoleService;
import com.howei.shiroadmin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Security;
import java.util.List;

/**
 * 在shiro中，最终通过Realm来获取应用程序中的用户、角色以及权限信息的
 * 在Realm中会直接从我们的数据源中获取市容需要额验证信息，可以说Realm是专用于安全框架的Dao
 */
@Slf4j
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PermissionService permissionService;


    /**
     * 认证
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) authenticationToken;
        String username = usernamePasswordToken.getUsername();
        String password = new String(usernamePasswordToken.getPassword());
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new UnknownAccountException("no_user");
        }
        if (!password.equals(user.getPassword())) {
            throw new IncorrectCredentialsException("error_password");
        }

        if ("1".equals(user.getState())) {
            throw new LockedAccountException("lock_account");
        }
        //调用 CredentialsMatcher 校验 还需要创建一个类 继承CredentialsMatcher  如果在上面校验了,这个就不需要了
        //配置自定义权限登录器 参考博客：
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPassword(), getName());
        return info;
    }

    /**
     * 授权
     * 授权的方法在碰到<shiro:hasPermission name=""></shiro:hasPermission>的时候调用
     * 它会去jianceshiro框架的权限（这里的permissions）是否包含该标签的name值，如果有，先视内容
     * 如果没有，里面的内容不显示，
     * <p>
     * shiro的权限授权是通过继承AuthorizingRealm抽象类，重写DoGetAuthorizationInfo方法
     * 当方位到页面的时候，连接配置了相应的权限或者shiro标签才会执行此方法，否则不执行
     * 所以如果只是简单的身份认证，没有权限控制的话，那么这个方法可以不进行实现，直接返回null
     * <p>
     * 在这个方法主要使用类:SimpleAuthenticationInfo进行角色的添加和权限的天机
     * authorizationInfo.addRole(role.getRole);authorizationInfo.addPermision(permission.getPermission)
     * <p>
     * 当然也可以添加Set集合：roles是从数据库中查询的当前用户的角色，stringPermission是当前用户的权限
     * authorizationInfo.setRoles(roles); authorizationInfo.setStringPermissions(stringPermissions);
     * <p>
     * 如果在shiro配置文件中添加了filterChainDefinitionMap.put("/add", "roles[100002]，perms[权限添加]");
     * * 就说明访问/add这个链接必须要有 "权限添加" 这个权限和具有 "100002" 这个角色才可以访问
     *
     * @param principalCollection
     * @return
     */

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        log.info("开始查询权限");
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        Integer uid = user.getUid();
        List<Role> roles = roleService.getByUserId(uid);
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (roles != null && roles.size() > 0) {
            for (Role role : roles) {
                //添加角色
                authorizationInfo.addRole(role.getRole());
                List<Permission> permissions = permissionService.getByRoleId(role.getId());
                if (permissions != null && permissions.size() > 0) {
                    //添加权限
                    for (Permission permission : permissions) {
                        authorizationInfo.addStringPermission(permission.getPermission());
                    }
                }
            }
        }

        return authorizationInfo;
    }


    /**
     * 清除当前用户的授权缓存
     *
     * @param principals
     */
    @Override
    protected void clearCachedAuthorizationInfo(PrincipalCollection principals) {
        super.clearCachedAuthorizationInfo(principals);
    }

    /**
     * 重写方法，清除当前用户的 认证缓存
     *
     * @param principals
     */
    @Override
    protected void clearCachedAuthenticationInfo(PrincipalCollection principals) {
        super.clearCachedAuthenticationInfo(principals);
    }

    @Override
    protected void clearCache(PrincipalCollection principals) {
        super.clearCache(principals);
    }

    /**
     * 自定义方法：清除所有 授权缓存
     */
    public void clearAllCachedAuthorizationInfo() {
        getAuthorizationCache().clear();
    }

    /**
     * 自定义方法：清除所有 认证缓存
     */
    public void clearAllCachedAuthenticationInfo() {
        getAuthenticationCache().clear();
    }

    /**
     * 自定义方法：清除所有的  认证缓存  和 授权缓存
     */
    public void clearAllCache() {
        clearAllCachedAuthenticationInfo();
        clearAllCachedAuthorizationInfo();
    }
}
