package cn.fireflies.miaosha.service;

import cn.fireflies.miaosha.dao.GoodsDao;
import cn.fireflies.miaosha.dao.OrderDao;
import cn.fireflies.miaosha.domain.MiaoshaOrder;
import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.domain.OrderInfo;
import cn.fireflies.miaosha.redis.OrderKey;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {
    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(Long id, long goodsId) {

        //return orderDao.getMiaoshaOrderByUserIdGoodsId(id,goodsId);
        return redisService.get(OrderKey.getMiaoshaOrderByUidGid,""+id+"_"+goodsId,MiaoshaOrder.class);
    }

    @Transactional
    public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
        //生成订单后写入redis缓存
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);//状态码：0新建未支付  1已支付   2已发货    3已收货
        orderInfo.setUserId(user.getId());
        orderDao.insert(orderInfo);
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(user.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);

        redisService.set(OrderKey.getMiaoshaOrderByUidGid,""+user.getId()+"_"+goods.getId(),miaoshaOrder);
        return orderInfo;
    }

    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }
}
