package cn.fireflies.miaosha.controller;

import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.MiaoshaUserService;
import org.apache.ibatis.annotations.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    MiaoshaUserService userService;
    @Autowired
    RedisService redisService;

    @RequestMapping("/info")
    @ResponseBody
    public Result<MiaoshaUser> info(Model model, MiaoshaUser user){

        return Result.success(user);
    }
}
