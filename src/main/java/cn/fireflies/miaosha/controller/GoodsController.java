package cn.fireflies.miaosha.controller;

import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.redis.GoodsKey;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.GoodsService;
import cn.fireflies.miaosha.service.MiaoshaUserService;
import cn.fireflies.miaosha.vo.GoodsDetailVo;
import cn.fireflies.miaosha.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 商品页面控制器
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {
    
//    private static Logger log = LoggerFactory.getLogger(GoodsController.class);
    
    @Autowired
    MiaoshaUserService userService;
    
    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    @Autowired
    ApplicationContext applicationContext;
    //优化前版本
//    @RequestMapping("/to_list")
//    public String toList(HttpServletResponse response, Model model,
//                         @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String cookieToken,
//                         @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false) String paramToken){
//        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)){
//            //均为空
//            return "login";
//        }
//        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;//优先级
//        //从redis中取得用户信息
//        MiaoshaUser user = userService.getByToken(response,token);
//        model.addAttribute("user",user);
//        return "goods_list";
//    }

    //优化后
    @RequestMapping(value = "/to_list")
    @ResponseBody
    public String toList(HttpServletRequest request,HttpServletResponse response, Model model, MiaoshaUser user){


//        return "goods_list";
        //取缓存
        String html = redisService.get(GoodsKey.getGoodsList,"",String.class);
//        System.out.println(html);
        if (!StringUtils.isEmpty(html)){
//            System.out.println("缓存读取到了");
            return html;
        }
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("user",user);
        model.addAttribute("goodsList",goodsList);

        SpringWebContext ctx = new SpringWebContext(request,response,
                request.getServletContext(),request.getLocale(),model.asMap(),applicationContext);
        //手动渲染
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list",ctx);
        if (!StringUtils.isEmpty(html)){
            redisService.set(GoodsKey.getGoodsList,"",html);
        }
//        System.out.println("缓存里没有");
        return html;
    }
    

    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> toDetail(HttpServletRequest request, HttpServletResponse response,
                                          Model model, MiaoshaUser user, @PathVariable("goodsId")long goodsId){

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;//秒杀状态
        int remainSeconds = 0;//倒计时
        if (now < startAt){
            //秒杀未开始,倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)(( startAt - now )/1000);
        }else if(now > endAt){
            //秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else{
            //秒杀进行中
            miaoshaStatus = 1;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }

    @RequestMapping(value = "/to_detail2/{goodsId}")
    @ResponseBody
    public String toDetail2(HttpServletRequest request,HttpServletResponse response,
                           Model model, MiaoshaUser user, @PathVariable("goodsId")long goodsId){
        model.addAttribute("user",user);
        //取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail,""+goodsId,String.class);
        if (!StringUtils.isEmpty(html)){
            return html;
        }

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods",goods);
        //
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;//秒杀状态
        int remainSeconds = 0;//倒计时
        if (now < startAt){
            //秒杀未开始,倒计时
            miaoshaStatus = 0;
            remainSeconds = (int)(( startAt - now )/1000);
        }else if(now > endAt){
            //秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = (int)(startAt-now);
        }else{
            //秒杀进行中
            miaoshaStatus = 1;
        }
        model.addAttribute("miaoshaStatus",miaoshaStatus);
        model.addAttribute("remainSeconds",remainSeconds);

        SpringWebContext ctx = new SpringWebContext(request,response,
                request.getServletContext(),request.getLocale(),model.asMap(),applicationContext);
        //手动渲染
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail",ctx);
        if (!StringUtils.isEmpty(html)){
            redisService.set(GoodsKey.getGoodsDetail,""+goodsId,html);
        }
        return html;
    }

}
