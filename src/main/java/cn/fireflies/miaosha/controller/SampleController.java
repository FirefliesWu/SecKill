package cn.fireflies.miaosha.controller;

import cn.fireflies.miaosha.mapper.User;
import cn.fireflies.miaosha.rabbitmq.MQSender;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.redis.UserKey;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender sender;

//    @RequestMapping("/mq")
//    @ResponseBody
//    public Result<String> mq(){
//        sender.send("hell0!");
//        return Result.success("Hello");
//    }
//
//    @RequestMapping("/mq/topic")
//    @ResponseBody
//    public Result<String> topic(){
//        sender.sendTopic("hell0!");
//        return Result.success("Hello");
//    }

    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model){
        model.addAttribute("name","fireflies");
        return "hello1";
    }

    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet(){
        User v1 = redisService.get(UserKey.getById,""+1,User.class);
        return Result.success(v1);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet(){
        User user = new User();
        user.setId(1);
        user.setName("11111");
        redisService.set(UserKey.getById,""+1,user);
        return Result.success(true);
    }

    @RequestMapping("/tx")
    @ResponseBody
    public boolean tx(){
        userService.tx();
        return true;
    }
}
