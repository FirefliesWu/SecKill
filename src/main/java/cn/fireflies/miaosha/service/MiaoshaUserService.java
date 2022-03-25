package cn.fireflies.miaosha.service;

import cn.fireflies.miaosha.dao.MiaoshaUserDao;
import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.exception.GlobalException;
import cn.fireflies.miaosha.redis.MiaoshaUserKey;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.result.CodeMsg;
import cn.fireflies.miaosha.utils.MD5Util;
import cn.fireflies.miaosha.utils.UUIDUtil;
import cn.fireflies.miaosha.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Service
public class MiaoshaUserService {

    public static final String COOKIE_NAME_TOKEN = "token";

    @Autowired
    MiaoshaUserDao miaoshaUserDao;
    @Autowired
    RedisService redisService;

    public MiaoshaUser getById(long id){
        MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, "" + id, MiaoshaUser.class);
        //先查缓存
        if (user!=null){
            return user;
        }
        //取数据库
        user = miaoshaUserDao.getById(id);
        if (user!=null){
            //写入缓存
            redisService.set(MiaoshaUserKey.getById,""+id,user);
        }
        return user;
    }

    //修改密码
    public boolean updatePassword(String token,long id,String formPass){
        //取user
        MiaoshaUser user = getById(id);
        if (user == null){
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //更新数据库
        MiaoshaUser toBeUpdate = new MiaoshaUser();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass,user.getSalt()));
        miaoshaUserDao.update(toBeUpdate);
        //修改缓存
        redisService.delete(MiaoshaUserKey.getById,""+id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(MiaoshaUserKey.token,token,user);
        return true;
    }

    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo==null){
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        //判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user == null){
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);//返回用户不存在
        }
        //验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);

//        System.out.println("表单密码："+formPass);//表单密码：c75dc0342535653ceb6d410492d4f113
//        System.out.println("计算得到密码："+calcPass);//计算得到密码：a7a93ae37f4d4ec6ed2d2762282da6da
//        System.out.println("服务器密码："+dbPass);//服务器密码：c75dc0342535653ceb6d410492d4f113

        if (!calcPass.equals(dbPass)){
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);//密码错误
        }
        //登录成功生成cookie
        String token = UUIDUtil.uuid();
        addCookie(response,token,user);
        return token;
    }

    public MiaoshaUser getByToken(HttpServletResponse response,String token) {
        if (StringUtils.isEmpty(token)){
            return null;
        }
        MiaoshaUser user =  redisService.get(MiaoshaUserKey.token,token,MiaoshaUser.class);
        //延长token有效期
        if (user != null){
            addCookie(response,token,user);
        }
        return user;
    }

    private void addCookie(HttpServletResponse response,String token,MiaoshaUser user){
        redisService.set(MiaoshaUserKey.token,token,user);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN,token);
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
