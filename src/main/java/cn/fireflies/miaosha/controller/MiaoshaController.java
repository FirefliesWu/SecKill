package cn.fireflies.miaosha.controller;

import cn.fireflies.miaosha.access.AccessLimit;
import cn.fireflies.miaosha.domain.MiaoshaOrder;
import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.domain.OrderInfo;
import cn.fireflies.miaosha.rabbitmq.MQSender;
import cn.fireflies.miaosha.rabbitmq.MiaoshaMessage;
import cn.fireflies.miaosha.redis.*;
import cn.fireflies.miaosha.result.CodeMsg;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.GoodsService;
import cn.fireflies.miaosha.service.MiaoshaService;
import cn.fireflies.miaosha.service.MiaoshaUserService;
import cn.fireflies.miaosha.service.OrderService;
import cn.fireflies.miaosha.utils.MD5Util;
import cn.fireflies.miaosha.utils.UUIDUtil;
import cn.fireflies.miaosha.vo.GoodsVo;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sun.awt.image.BufferedImageDevice;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;


    //基于令牌桶算法的限流实现类
    RateLimiter rateLimiter = RateLimiter.create(10);

    private Map<Long,Boolean> localOverMap = new HashMap<Long,Boolean>();

    /**
     * 系统初始化
     * 库存加载到缓存中
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (goodsList == null) {
            return;
        }
        for (GoodsVo goods : goodsList){
            redisService.set(GoodsKey.getMiaoshaGoodsStock,""+goods.getId(),goods.getStockCount());
            localOverMap.put(goods.getId(), false);
        }
    }

    /**
     * 压测结果
     *  用户   请求次数   库存    最终库存     QPS
     * 5000 *   10      10       -36     438.6/秒
     *
     * GET 和 POST有什么区别？
     * GET是幂等的
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/{path}/do_miaosha",method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                       @RequestParam("goodsId")long goodsId,
                                   @PathVariable("path")String path){

        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
            return  Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
        }

        if (user == null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        model.addAttribute("user",user);
        //验证path路径
        boolean check = miaoshaService.checkPath(goodsId,user,path);
        if (!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //先从本地map中查看对应商品的状态
        Boolean over = localOverMap.get(goodsId);
        if (over){
            //如果商品已经无库存，就不用预减库存，直接返回失败
            return Result.error(CodeMsg.SECKILL_OVER);
        }


        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
        if (stock < 0){
            //标记商品已没有库存了
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.SECKILL_OVER);
        }
        //判断是否已经秒杀
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if (order!=null){
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }
        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);//排队中

//        //判断商品库存
//        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
//        //考虑场景，同一个用户同一时间秒杀两件商品，重复秒杀。可以通过在数据库秒杀订单表给用户id和订单id加唯一索引，一个用户只有一个订单
//        int stock = goods.getStockCount();
//        if (stock <= 0){
//            return Result.error(CodeMsg.SECKILL_OVER);
//        }
//        //判断是否已经秒杀到了
//        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
//        if (order != null){
//            return Result.error(CodeMsg.REPEATE_SECKILL);
//        }
//        //开始秒杀流程
//        //1.减库存  2.下订单  3.写入秒杀订单   原子操作
//        OrderInfo orderInfo = miaoshaService.miaosha(user,goods);
//        return Result.success(orderInfo);
    }

    /**
     * orderId：成功
     * -1：库存不足，秒杀失败
     * 0：排队中
     * @param model
     * @param user
     * @param goodsId
     * @return
     */
    @AccessLimit(seconds=5, maxCount=10, needLogin=true)
    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result = miaoshaService.getMiaoshaResult(user.getId(),goodsId);
//        System.out.println("result====>"+result);
        return Result.success(result);
    }

//    //还原接口
//    @RequestMapping(value = "/reset",method = RequestMethod.GET)
//    @ResponseBody
//    public Result<Boolean> reset(Model model){
//        List<GoodsVo> goodsList = goodsService.listGoodsVo();
//        for (GoodsVo goods:goodsList){
//            goods.setStockCount(10);
//            redisService.set(GoodsKey.getMiaoshaGoodsStock,""+goods.getId(),10);
//            localOverMap.put(goods.getId(),false);
//        }
//        redisService.delete(OrderKey.getMiaoshaOrderByUidGid.getPrefix());
//        redisService.delete(MiaoshaKey.isGoodsOver.getPrefix());
//        miaoshaService.reset(goodsList);
//        return Result.success(true);
//    }

    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value = "/path",method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam(value="verifyCode",defaultValue = "0")int verifyCode) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //拦截器取代下列代码
//        //查询访问次数
//        String uri = request.getRequestURI();
//        String key = uri + "_" + user.getId();
//        Integer count = redisService.get(AccsessKey.accessLimit, key, Integer.class);
//        //五秒内访问5次超出限制
//        if (count == null){
//            redisService.set(AccsessKey.accessLimit,key,1);
//        }else if (count < 5){
//            redisService.incr(AccsessKey.accessLimit,key);
//        }else {
//            return Result.error(CodeMsg.ACCESS_LIMIT);
//        }

        //验证码校验
        boolean check = miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if (!check){
            return Result.error(CodeMsg.VERIFY_ERROR);
        }
        String path = miaoshaService.createMiaoshaPath(user,goodsId);
        return Result.success(path);
    }

    //生成图片验证码
    @RequestMapping(value = "/verifyCode",method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response, MiaoshaUser user,
                                               @RequestParam("goodsId")long goodsId) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try{
            BufferedImage image = miaoshaService.createMiaoshaVerifyCode(user,goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image,"JPEG",out);
            out.flush();
            out.close();
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }
}
