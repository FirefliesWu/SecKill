package cn.fireflies.miaosha.access;

import cn.fireflies.miaosha.domain.MiaoshaUser;

/**
 * 用于存储秒杀用户的上下文容器
 */
public class UserContext {

    private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();

    public static void setUser(MiaoshaUser user){
        userHolder.set(user);
    }

    public static MiaoshaUser getUser(){
        return userHolder.get();
    }
}
