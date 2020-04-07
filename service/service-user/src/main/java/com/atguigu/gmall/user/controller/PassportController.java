package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * @date 2020/3/25 11:23
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    // 调用实现类
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 需要得到用户名，密码。页面在点击登录的时候，传递的是json 数据
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        UserInfo info = userService.login(userInfo);
        // 登录成功！
        if (info!=null){
            // 将用户信息存储在缓存
            String token = UUID.randomUUID().toString().replace("-","");
            // 需要声明一个map 集合
            HashMap<String, Object> map = new HashMap<>();
            // 存储用户昵称
            map.put("nickName",info.getNickName());
            // 用户的真实姓名
            map.put("name",info.getName());
            // 因为访问其他业务模块的时候，需要判断用户是否登录。必须从缓存中获取userKey 然后获取useId
            map.put("token",token);

            // 缓存中只存在userId 数据类型String
            // userKey = user:login:token
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            // value = userId
            redisTemplate.opsForValue().set(userKey,info.getId().toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            return Result.ok(map);
        }else {
            return Result.fail().message("用户名，密码不正确！");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        // 退出登录 本质：将缓存数据删除
        // 登录成功的时候，将token 放入cookie ，放入header。
        // 记住不能使用cookie 获取！spring boot + spring cloud 经过远程调用的时候，cookie 是无法携带数据的，用header存储并获取
        String token = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
        redisTemplate.delete(userKey);
        return Result.ok();
    }
}
