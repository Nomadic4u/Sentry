package org.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RestBean;
import org.example.entity.dto.Account;
import org.example.entity.dto.Client;
import org.example.service.AccountService;
import org.example.service.ClientService;
import org.example.utils.Const;
import org.example.utils.JwtUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 用于对请求头中Jwt令牌进行校验的工具，为当前请求添加用户验证信息
 * 并将用户的ID存放在请求对象属性中，方便后续使用
 */
@Component
public class JWTAuthorizeFilter extends OncePerRequestFilter {

    @Resource
    JwtUtils utils;

    @Resource
    ClientService clientService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization"); // 获取请求头中的信息
        String uri = request.getRequestURI();
        if (uri.startsWith("/monitor")) {
            // 如果不是客户端的register, 直接通过token查询到client, 然后将client放入request中
            if (!uri.endsWith("/register")) {
                Client client = clientService.findClientByToken(authorization);
                if (client == null) {
                    response.setStatus(401);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(RestBean.failure(401, "未注册").asJsonString());
                    return ;
                } else {
                    request.setAttribute(Const.ATTR_CLIENT, client); // 在HTTP请求中去设置一个键值对
                }
            }
        } else { // 前端的请求
            DecodedJWT jwt = utils.resolveJwt(authorization);
            if(jwt != null) {
                UserDetails user = utils.toUser(jwt);
                // 认证成功的令牌. 拿到存贮用户认证信息的类
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                // 将认定信息绑定到当前的HTTP请求中
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 将认证对象设置到上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(Const.ATTR_USER_ID, utils.toId(jwt));
                request.setAttribute(Const.ATTR_USER_ROLE, new ArrayList<>(user.getAuthorities()).get(0).getAuthority());
                // terminal用于ssh链接, 判断当前用户是否有权限去ssh连接
                if(request.getRequestURI().startsWith("/terminal/") && !accessShell(
                        (int) request.getAttribute(Const.ATTR_USER_ID),
                        (String) request.getAttribute(Const.ATTR_USER_ROLE),
                        Integer.parseInt(request.getRequestURI().substring(10)))) {
                    response.setStatus(401);
                    response.setCharacterEncoding("utf-8");
                    response.getWriter().write(RestBean.failure(401, "无权访问").asJsonString());
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    @Resource
    AccountService accountService;

    private boolean accessShell(int userId, String userRole, int clientId) {
        if(Const.ROLE_ADMIN.equals(userRole.substring(5))) {
            return true;
        } else {
            Account account = accountService.getById(userId);
            return account.getClientList().contains(clientId);
        }
    }
}










