package com.rufeng2333.com.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.rufeng2333.gmall.bean.UmsMember;
import com.rufeng2333.gmall.service.UserService;
import com.rufeng2333.gmall.util.HttpclientUtil;
import com.rufeng2333.gmall.util.JwtUtil;
import io.jsonwebtoken.Jwt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code,HttpServletRequest request){

        //通过code获取access_token  , 同时会将用户Id等信息查询出来一并返回
        String s3 = "https://api.weibo.com/oauth2/access_token?client_id=3651434760&client_secret=c6f2758393390453b862121b29dbf844&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code="+ code;
        String access_token_json = HttpclientUtil.doPost(s3, null);
        Map<String , String> access_map = JSON.parseObject(access_token_json, Map.class);

        String uid = (String)access_map.get("uid");
        String access_token = (String) access_map.get("access_token");

        //用access_token查询用户信息
        String s4 = "https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        String user_json = HttpclientUtil.doGet(s4);
        Map<String , String> user_map = JSON.parseObject(user_json, Map.class);

        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String) user_map.get("idstr"));
        umsMember.setCity((String) user_map.get("location"));
        umsMember.setNickname(user_map.get("screen_name"));
        //除了男的都是女的
        String g = "0";
        String gender = user_map.get(("gender"));
        if(gender.equals("m")){
            g = "1";
        }

        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck);

        if(umsMemberCheck == null) {
            //mybatis主键返回策略不能跨rpc，UserServiceImpl可以直接拿到id，但这里不能直接拿，
            //故直接在UserServiceImpl添加方法中把umsMember对象返回
            umsMember = userService.addOauthUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        String token = "";
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();

        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);
        userMap.put("nickname",nickname);

        String ip = request.getHeader("x-forwarded-for");
        if(StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();
            //都没有的话应该进行异常处理，这里直接写死
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }
        //下面1和3参数要加密
        token = JwtUtil.encode("rufeng2333", userMap, ip);

        userService.addUserToken(token,memberId);

        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }

    @RequestMapping("verify")
    @ResponseBody
    //这里传入的ip和request的ip不一样
    public String verify(String token,String currentIp){

        Map<String,String> map = new HashMap<>();

        Map<String, Object> decode = JwtUtil.decode(token, "rufeng2333", currentIp);

        if(decode!=null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        }else {
            map.put("status", "fail");
        }
        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request){

        String token = "";

        UmsMember umsMemberLogin = userService.login(umsMember);

        if(umsMemberLogin != null){

            //用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            Map<String,Object> userMap = new HashMap<>();
            userMap.put("memberId",memberId);
            userMap.put("nickname",nickname);

            String ip = request.getHeader("x-forwarded-for");
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
                //都没有的话应该进行异常处理，这里直接写死
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            //下面1和3参数要加密
            token = JwtUtil.encode("rufeng2333", userMap, ip);

            userService.addUserToken(token,memberId);
        }else {
            token = "fail";
        }

        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        if(StringUtils.isNotBlank(ReturnUrl)) {
            map.put("ReturnUrl", ReturnUrl);
        }
        return "index";
    }

}
