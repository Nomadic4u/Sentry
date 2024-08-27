package org.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtils {

    // 用于给JWT令牌签名校验的秘钥
    @Value("${spring.security.jwt.key}")
    String key; // 读取秘钥

    // 令牌的有效时间
    @Value("${spring.security.jwt.expire}") //token持续时间
            int expire;

    // 为用户生成的JWT令牌的冷却时间
    @Value("${spring.security.jwt.limit.base}")
    private int limit_base;

    //用户如果继续恶意刷令牌，更严厉的封禁时间
    @Value("${spring.security.jwt.limit.upgrade}")
    private int limit_upgrade;

    // 判定用户在冷却时间内, 继续恶意刷令牌的次数
    @Value("${spring.security.jwt.limit.frequency}")
    private int limit_frequency;

    @Resource
    StringRedisTemplate template;

    @Resource
    FlowUtils utils;

    /**
     * 让指定Jwt令牌失效
     *
     * @param headerToken 请求头中携带的令牌
     * @return 是否操作成功
     */
    public boolean invalidateJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        Algorithm algorithm = Algorithm.HMAC256(key); // 加密方式
        JWTVerifier jwtVerifier = JWT.require(algorithm).build(); // 验证签名
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            return deleteToken(jwt.getId(), jwt.getExpiresAt());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 将Token列入Redis黑名单中
     *
     * @param uuid 令牌ID
     * @param time 过期时间
     * @return 是否操作成功
     */
    private boolean deleteToken(String uuid, Date time) {
        if (this.isInvalidToken(uuid))
            return false;
        Date now = new Date();
        long expire = Math.max(0, time.getTime() - now.getTime());
        template.opsForValue().set(Const.JWT_BLACK_LIST + uuid, "", expire, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * 验证Token是否被列入Redis黑名单
     *
     * @param uuid 令牌ID
     * @return 是否操作成功
     */
    private boolean isInvalidToken(String uuid) {
        return Boolean.TRUE.equals(template.hasKey(Const.JWT_BLACK_LIST + uuid));
    }

    /**
     * 解析Jwt令牌
     *
     * @param headerToken 请求头中携带的令牌
     * @return DecodedJWT
     */
    public DecodedJWT resolveJwt(String headerToken) {
        String token = this.convertToken(headerToken);
        if (token == null)
            return null;
        Algorithm algorithm = Algorithm.HMAC256(key); // 加密方式
        JWTVerifier jwtVerifier = JWT.require(algorithm).build(); // 验证签名
        try {
            DecodedJWT verify = jwtVerifier.verify(token); //验证token是否被篡改,然后解码 如果被篡改会抛出一个异常
            if (this.isInvalidToken(verify.getId())) // 判断token是否在黑名单中
                return null;
            if (this.isInvalidUser(verify.getClaim("id").asInt())) // 检验用户是否删除 即被uid被拉黑
                return null;
//            Date expiresAt = verify.getExpiresAt(); // 获取过期的日期
//            return new Date().after(expiresAt) ? null : verify; //判断是否过期, 如果没有过期, 则返回解析后的JWT
            Map<String, Claim> claims = verify.getClaims();
            return new Date().after(claims.get("exp").asDate()) ? null : verify;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    /**
     * 根据UserDetails生成对应的Jwt令牌
     *
     * @param details 用户信息
     * @return 令牌
     */
    public String createJwt(UserDetails details, int id, String username) {
        if (this.frequencyCheck(id)) {
            Algorithm algorithm = Algorithm.HMAC256(key); // 加密方式
            Date expire = this.expireTime();
            return JWT.create()
                    .withJWTId(UUID.randomUUID().toString())
                    .withClaim("id", id)
                    .withClaim("name", username)
                    .withClaim("authorities", details.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority).toList())
                    .withExpiresAt(expire) // 设置过期时间
                    .withIssuedAt(new Date()) // 颁发时间
                    .sign(algorithm); // 签名
        } else {
            return null;
        }
    }

    /**
     * 频率检测，防止用户高频申请Jwt令牌，并且采用阶段封禁机制
     * 如果已经提示无法登录的情况下用户还在刷，那么就封禁更长时间
     *
     * @param userId 用户ID
     * @return 是否通过频率检测
     */
    private boolean frequencyCheck(int userId) {
        String key = Const.JWT_FREQUENCY + userId;
        return utils.limitOnceUpdateCheck(key, limit_frequency, limit_base, limit_upgrade);
    }

    /**
     * 根据配置快速计算过期时间
     *
     * @return 过期时间
     */
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire * 24);
        return calendar.getTime();
    }

    /**
     * 将jwt对象中的内容封装为UserDetails
     *
     * @param jwt 已解析的Jwt对象
     * @return UserDetails
     */
    public UserDetails toUser(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("******")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    /**
     * 将jwt对象中的用户ID提取出来
     *
     * @param jwt 已解析的Jwt对象
     * @return 用户ID
     */
    public Integer toId(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }


    /**
     * 校验并转换请求头中的Token令牌
     *
     * @param headerToken 请求头中的Token
     * @return 转换后的令牌
     */
    private String convertToken(String headerToken) {
        if (headerToken == null)
            return null;
        return headerToken.substring(7);
    }


    /**
     * 将删除的用户拉入黑名单
     *
     * @param uid
     */
    private void deleteUser(int uid) {
        template.opsForValue().set(Const.USER_BLACK_LIST + uid, "", expire, TimeUnit.HOURS);
    }

    /**
     * 验证用户是否在黑名单中
     *
     * @param uid
     * @return
     */
    private boolean isInvalidUser(int uid) {
        return Boolean.TRUE.equals(template.hasKey(Const.USER_BLACK_LIST + uid));
    }
}
