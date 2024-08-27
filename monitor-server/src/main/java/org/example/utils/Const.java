package org.example.utils;

public class Const {
    // jwt令牌
    public static final String JWT_BLACK_LIST = "jwt:blacklist:";
    public final static String JWT_FREQUENCY = "jwt:frequency";

    // 用户
    public final static String USER_BLACK_LIST = "user:blacklist:";

    // 邮件验证码
    public static final String VERIFY_EMAIL_LIMIT = "verify:email:limit";
    public static final String VERIFY_EMAIL_DATA = "verify:email:data";

    // 过滤器优先级
    public static final int ORDER_CORS = -103;
    public static final int ORDER_LIMIT = -101;

    // 请求限制频率
    public static final String FLOW_LIMIT_COUNTER = "flow:counter"; // 限流中ip的计数器
    public static final String FLOW_LIMIT_BLOCK = "flow:block"; // 限流中ip的阻塞队列

    // 请求自定义属性
    public final static String ATTR_USER_ID = "userId";
    public final static String ATTR_USER_ROLE = "userRole";
    public final static String ATTR_CLIENT = "client";

    // 消息队列
    public final String MQ_MAIL = "mail";

    // 用户角色
    public final static String ROLE_ADMIN = "admin";
    public final static String ROLE_NORMAL = "user";
}
