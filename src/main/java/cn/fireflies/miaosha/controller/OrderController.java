package cn.fireflies.miaosha.controller;

import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.domain.OrderInfo;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.result.CodeMsg;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.GoodsService;
import cn.fireflies.miaosha.service.MiaoshaService;
import cn.fireflies.miaosha.service.MiaoshaUserService;
import cn.fireflies.miaosha.service.OrderService;
import cn.fireflies.miaosha.vo.GoodsVo;
import cn.fireflies.miaosha.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {

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

    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model, MiaoshaUser user,
                                      @RequestParam("orderId")long orderId){
        if (user==null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        OrderInfo order = orderService.getOrderById(orderId);
        if (order==null){
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }
        Long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(order);
        vo.setGoods(goods);
        return Result.success(vo);
    }
}
