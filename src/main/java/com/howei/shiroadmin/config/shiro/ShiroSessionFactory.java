package com.howei.shiroadmin.config.shiro;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.web.session.mgt.DefaultWebSessionContext;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class ShiroSessionFactory implements SessionFactory {
    @Override
    public Session createSession(SessionContext sessionContext) {
        ShiroSession session = new ShiroSession();
        HttpServletRequest request = (HttpServletRequest) sessionContext.get(DefaultWebSessionContext.class.getName() + ".SERVLET_REQUEST");

        session.setHost(getIpAddress(request));
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        String localIP="127.0.0.1";
        String ip=request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || (ip.equalsIgnoreCase(localIP)) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
