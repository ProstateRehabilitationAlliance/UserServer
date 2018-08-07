package com.prostate.doctor.controller;

import com.prostate.doctor.cache.redis.RedisSerive;
import com.prostate.doctor.entity.Doctor;
import com.prostate.doctor.feignService.ThirdServer;
import com.prostate.doctor.param.DoctorRegisteParams;
import com.prostate.doctor.service.DoctorService;
import com.prostate.doctor.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: developerfengrui
 * @Description:
 * @Date: Created in 10:23 2018/4/19
 * @Modified By:
 */
@Slf4j
@RestController
@RequestMapping("doctor")
public class DoctorController extends BaseController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private JsonUtils<Doctor> jsonUtil;

    @Autowired
    private RedisSerive redisSerive;

    @Autowired
    private ThirdServer thirdServer;


    /**
     * 手机号 短信验证码 注册 接口
     * @param doctorRegisteParams
     * @return
     */
    @RequestMapping(value = "register", method = RequestMethod.POST)
    public Map registerDoctor(@Valid DoctorRegisteParams doctorRegisteParams) {

        String doctorPhone = doctorRegisteParams.getDoctorPhone();
        String smsCode = doctorRegisteParams.getSmsCode();
        String doctorPassword = doctorRegisteParams.getDoctorPassword();


        Doctor doctor = doctorService.selectByPhone(doctorPhone);
        if (doctor != null) {
            return registerFiledResponse("手机号码已注册过");
        }
        //短信验证码校验
        String ck = redisSerive.getSmsCode(smsCode);
        if (StringUtils.isEmpty(ck)) {
            return failedRequest("验证码已过期!");
        } else if (!doctorPhone.equals(ck)) {
            return failedRequest("手机号码不一致");
        }
        //手机号重复注册数据校验
        doctor = new Doctor();
        doctor.setDoctorPhone(doctorPhone);
        //生成盐
        String salt = RandomStringUtils.randomAlphanumeric(32).toLowerCase();
        //设置盐
        doctor.setSalt(salt);
        //md5密码加密
        doctor.setDoctorPassword(DigestUtils.md5DigestAsHex((doctorPassword + salt).getBytes()));

        //这里做一次数据检查

        int result = doctorService.insertSelective(doctor);
        if (result > 0) {
            return registerSuccseeResponse("注册成功");
        }
        return registerFiledResponse("注册失败,该手机号已被注册");
    }


    /**
     * 手机号 密码 登陆 接口
     * @param doctorPhone
     * @param doctorPassword
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public Map loginDoctor(String doctorPhone, String doctorPassword) {
        Doctor doctor = doctorService.selectByPhone(doctorPhone);
        if (doctor == null) {
            return loginFailedResponse("用户名或密码不正确");
        }
        String salt = doctor.getSalt();
        if (doctor.getDoctorPassword().equals(DigestUtils.md5DigestAsHex((doctorPassword + salt).getBytes()))) {
            String token = doctor.getId();
            redisSerive.insert(token, jsonUtil.objectToJsonStr(doctor));
            log.info("======登陆成功====");
            return loginSuccessResponse(token);
        } else {
            log.info("======登陆失败====");
            return loginFailedResponse("用户名或密码不正确");
        }
    }


    /**
     * 手机号 短信验证码 登陆 接口
     *
     * @param doctorPhone
     * @param smsCode
     */
    @RequestMapping(value = "smsLogin", method = RequestMethod.POST)
    public Map smsLogin(String doctorPhone, String smsCode) {

        //短信验证码校验
        String cachePhone = redisSerive.getSmsCode(smsCode);
        if (StringUtils.isEmpty(cachePhone)) {
            return failedRequest("验证码已过期!");
        } else if (!doctorPhone.equals(cachePhone)) {
            return failedRequest("手机号码错误");
        }
        Doctor doctor = doctorService.selectByPhone(doctorPhone);
        if (doctor == null) {
            return loginFailedResponse("手机号或验证码错误");
        }
        String token = doctor.getId();
        redisSerive.insert(token, jsonUtil.objectToJsonStr(doctor));

        return loginSuccessResponse(token);
    }

    /**
     * 重设 登陆密码
     * @param doctorPhone
     * @param smsCode
     * @param doctorPassword
     * @return
     */
    @RequestMapping(value = "passwordReset", method = RequestMethod.POST)
    public Map passwordReset(String doctorPhone, String smsCode, String doctorPassword) {
        //短信验证码校验
        String cachePhone = redisSerive.getSmsCode(smsCode);
        if (StringUtils.isEmpty(cachePhone)) {
            return failedRequest("验证码已过期!");
        } else if (!doctorPhone.equals(cachePhone)) {
            return failedRequest("手机号码错误");
        }

        Doctor doctor = doctorService.selectByPhone(doctorPhone);
        if (doctor == null) {
            return loginFailedResponse("手机号或验证码错误");
        }
        //生成盐
        String salt = RandomStringUtils.randomAlphanumeric(32).toLowerCase();
        //设置盐
        doctor.setSalt(salt);
        //md5密码加密
        doctor.setDoctorPassword(DigestUtils.md5DigestAsHex((doctorPassword + salt).getBytes()));

        int i = doctorService.updateSelective(doctor);
        if (i > 0) {
            return updateSuccseeResponse("密码重置成功");
        }
        return updateFailedResponse("密码重置失败");
    }
//    /**
//     * @Author: feng
//     * @Description: 修改密码
//     * @Date: 16:45  2018/4/19
//     * @Params: * @param null
//     */
//
//    @RequestMapping(value = "updDoctorPassword", method = RequestMethod.POST)
//    public Map updDoctorPassword(String doctorPhone, String doctorPassword
//            , @RequestParam("newPassword") String newPassword) {
//        List<Doctor> list = doctorService.selectByPhone(doctorPhone);
//        resultMap = new LinkedHashMap<>();
//
//        if (list == null) {
//            resultMap.put("code", 20005);
//            resultMap.put("msg", "没有数据");
//            resultMap.put("result", null);
//        } else if (list.size() == 1) {
//            String salt = list.get(0).getSalt();
//            if (list.get(0).getDoctorPassword().equals(DigestUtils.md5DigestAsHex((doctorPassword + salt).getBytes()))) {
//                Doctor doctor = list.get(0);
//                log.info(doctorPhone + "手机号密码修改成功" + new Date());
//                System.out.println("===>" + newPassword);
//                doctor.setDoctorPassword(DigestUtils.md5DigestAsHex((newPassword + list.get(0).getSalt()).getBytes()));
//                doctorService.updDoctorPassword(doctor);
//
//                resultMap.put("code", 20000);
//                resultMap.put("msg", "密码修改成功");
//                resultMap.put("result", null);
//            } else {
//                resultMap.put("code", 20004);
//                resultMap.put("msg", "密码不正确");
//                resultMap.put("result", null);
//            }
//        }
//        return resultMap;
//    }


    /**
     * @param token
     * @return
     * @Author MaxCoder
     * @Description 用户 退出登陆 接口
     * @Date: 18:00 2018/4/24
     */
    @PostMapping(value = "logout")
    public Map<String, Object> logout(String token) {
        resultMap = new LinkedHashMap<>();
        boolean b = redisSerive.remove(token);
        if (b) {
            resultMap.put("code", 20000);
            resultMap.put("msg", "账号登出成功");
            resultMap.put("result", null);
        } else {
            resultMap.put("code", 20004);
            resultMap.put("msg", "账号登出失败");
            resultMap.put("result", null);
        }
        return resultMap;
    }


    /**
     * 获取注册 短信验证码
     *
     * @param registerPhone
     * @return
     */
    @GetMapping(value = "registerSms")
    public Map registerSms(String registerPhone) {

        Doctor doctor = doctorService.selectByPhone(registerPhone);

        if (doctor != null) {
            return registerFiledResponse("手机号码已注册过");
        }
        return thirdServer.sendRegisterCode(registerPhone);
    }


    /**
     * 获取 登陆 短信验证码
     *
     * @param loginPhone
     * @return
     */
    @GetMapping(value = "loginSms")
    public Map loginSms(String loginPhone) {

        Doctor doctor = doctorService.selectByPhone(loginPhone);

        if (doctor == null) {
            return loginFailedResponse("手机号码未注册!");
        }
        return thirdServer.sendLoginCode(loginPhone);
    }

    /**
     * 获取 修改密码 短信验证码
     *
     * @param passwordPhone
     * @return
     */
    @GetMapping(value = "passwordSms")
    public Map passwordSms(String passwordPhone) {

        Doctor doctor = doctorService.selectByPhone(passwordPhone);

        if (doctor == null) {
            return loginFailedResponse("手机号码未注册!");
        }
        return thirdServer.sendPasswordReplaceCode(passwordPhone);
    }
}
