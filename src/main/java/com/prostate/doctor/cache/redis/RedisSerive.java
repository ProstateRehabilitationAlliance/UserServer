package com.prostate.doctor.cache.redis;

import com.prostate.doctor.entity.Doctor;
import com.prostate.doctor.entity.WeChatUser;
import com.prostate.doctor.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * @author MaxCoder
 *
 * @date 2017.04.09
 *
 * Redis 服务
 */
@Service
public class RedisSerive {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JsonUtils<Doctor> jsonUtil;

    public void insert(String key,String val) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
//        valueOperations.set(key,val);
        valueOperations.set(key,val,60*60*24, TimeUnit.SECONDS);
    }

    /**
     * @param key
     * @param val
     * @param l
     */
    public void insert(String key,String val,long l) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(key,val,l, TimeUnit.SECONDS);
    }

    public String get(String key) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String val = valueOperations.get(key);
        valueOperations.set(key,val,60*60*24, TimeUnit.SECONDS);
        return val;
    }

    public Doctor getDoctor(String key) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        return jsonUtil.jsonStrToObject(valueOperations.get(key));
    }
    public Doctor getDoctor() {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        String token = request.getHeader("token");
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        return jsonUtil.jsonStrToObject(valueOperations.get(token));
    }
    public boolean remove(String key) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        return stringRedisTemplate.delete(key);
    }

    public WeChatUser getWechatUser(String key) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        return jsonUtil.jsonStrToWechatUser(valueOperations.get(key));
    }

    public WeChatUser getWechatUser() {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        String token = request.getHeader("token");
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        return jsonUtil.jsonStrToWechatUser(valueOperations.get(token));
    }

    public String getSmsCode(String key) {
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();
        String val = valueOperations.get(key);
        return val;
    }
}
