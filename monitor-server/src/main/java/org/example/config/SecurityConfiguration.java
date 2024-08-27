package org.example.config;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RestBean;
import org.example.entity.dto.Account;
import org.example.entity.vo.response.AuthorizeVO;
import org.example.filter.JWTAuthorizeFilter;
import org.example.filter.RequestLogFilter;
import org.example.service.AccountService;
import org.example.utils.Const;
import org.example.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfiguration {

    @Resource
    JwtUtils utils;

    @Resource
    JWTAuthorizeFilter jwtAuthorizeFilter;

    @Resource
    AccountService accountService;

    @Resource
    RequestLogFilter requestLogFilter;

    /**
     * 针对于SpringSecurity 6 的新版配置方法
     *
     * @param http 配置器
     * @return 自动构建的内置过滤器链
     * @throws Exception 可能的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(conf -> conf
                                .requestMatchers("api/auth/**", "/error").permitAll() //放行
                                .requestMatchers("/terminal/**").permitAll() //放行
                                .requestMatchers("/monitor/**").permitAll() //放行
//                                .requestMatchers("api/monitor/**").permitAll() //放行
                                .requestMatchers("api/user/sub/**").hasRole(Const.ROLE_ADMIN) // 创建子用户必须是管理员
                                .anyRequest() // 任何请求都不允许通过
                                .hasAnyRole(Const.ROLE_ADMIN, Const.ROLE_NORMAL)
//                               .authenticated() // 验证之后才能通过
                )
                .formLogin(conf -> conf
                        .loginProcessingUrl("/api/auth/login") //登录的路径 登录用户名默认为username
                        .failureHandler(this::handleProcess)
                        .successHandler(this::handleProcess) // 登录成功处理器
                        .permitAll()
                )
                .logout(conf -> conf
                        .logoutUrl("/api/auth/logout") //退出的路径
                        .logoutSuccessHandler(this::onLogoutSuccess) //退出成功处理器
                )
                .exceptionHandling(conf -> conf
                        .accessDeniedHandler(this::handleProcess) // 访问被拒绝处理器
                        .authenticationEntryPoint(this::handleProcess) //
                )
                .csrf(AbstractHttpConfigurer::disable) // 禁用csrf防护
                .sessionManagement(conf -> conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 会话管理设置为无状态
                .addFilterBefore(requestLogFilter, UsernamePasswordAuthenticationFilter.class) // 设置日志过滤器
                .addFilterBefore(jwtAuthorizeFilter, RequestLogFilter.class) // 设置JWT认证过滤器
                .build();
    }

    /**
     * 将多种类型的Handler整合到同一个方法中，包含：
     * - 登录成功
     * - 登录失败
     * - 未登录拦截/无权限拦截
     * @param request 请求
     * @param response 响应
     * @param exceptionOrAuthentication 异常或是验证实体
     * @throws IOException 可能的异常
     */
    private void handleProcess(HttpServletRequest request,
                               HttpServletResponse response,
                               Object exceptionOrAuthentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        if(exceptionOrAuthentication instanceof AccessDeniedException exception) {
            writer.write(RestBean
                    .forbidden(exception.getMessage()).asJsonString());
        } else if(exceptionOrAuthentication instanceof Exception exception) {
            writer.write(RestBean
                    .unauthorized(exception.getMessage()).asJsonString());
        } else if(exceptionOrAuthentication instanceof Authentication authentication){
            User user = (User) authentication.getPrincipal();
            Account account = accountService.findAccountByNameOrEmail(user.getUsername());
            String jwt = utils.createJwt(user, account.getId(), account.getUsername());
            if(jwt == null) {
                writer.write(RestBean.forbidden("登录验证频繁，请稍后再试").asJsonString());
            } else {
                AuthorizeVO vo = account.asViewObject(AuthorizeVO.class, o -> o.setToken(jwt));
                vo.setExpire(utils.expireTime());
                writer.write(RestBean.success(vo).asJsonString());
            }
        }
    }


    /**
     * 退出登录处理，将对应的Jwt令牌列入黑名单不再使用
     * @param request 请求
     * @param response 响应
     * @param authentication 验证实体
     * @throws IOException 可能的异常
     */
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if (utils.invalidateJwt(authorization)) {
            writer.write(RestBean.success("退出登录成功").asJsonString());
        } else {
            writer.write(RestBean.failure(400, "退出登录失败").asJsonString());
        }
    }




//    public void onAccessDeny(HttpServletRequest request,
//                             HttpServletResponse response,
//                             AccessDeniedException accessDeniedException) throws IOException {
//        response.setContentType("application/json");
//        response.getWriter().write(RestBean.forbidden(accessDeniedException.getMessage()).asJsonString());
//    }
//
//
//    public void onUnauthorized(HttpServletRequest request,
//                               HttpServletResponse response,
//                               AuthenticationException exception) throws IOException {
//        response.setContentType("application/json");
//        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
//    }
//
//
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException, ServletException {
//        response.setContentType("application/json"); // 告诉前端对应的格式
//        response.setCharacterEncoding("utf-8");
//        User user = (User) authentication.getPrincipal(); // 这里是SpringSecurity的user
//        Account account = accountService.findAccountByNameOrEmail(user.getUsername());
//        String token = utils.createJwt(user, account.getId(), account.getUsername());
//        AuthorizeVO vo = account.asViewObject(AuthorizeVO.class, v -> {
//            v.setExpire(utils.expireTime());
//            v.setToken(token);
//        });
////        vo.setUsername("小明");
////        BeanUtils.copyProperties(account, vo);
//        response.getWriter().write(RestBean.success(vo).asJsonString());
//    }
//
//    public void onAuthenticationFailure(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        AuthenticationException exception) throws IOException, ServletException {
//        response.setContentType("application/json");
//        response.setCharacterEncoding("utf-8");
//        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
//    }

}



















