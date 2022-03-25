package cn.fireflies.miaosha.rabbitmq;

import cn.fireflies.miaosha.domain.MiaoshaOrder;
import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.result.CodeMsg;
import cn.fireflies.miaosha.result.Result;
import cn.fireflies.miaosha.service.GoodsService;
import cn.fireflies.miaosha.service.MiaoshaService;
import cn.fireflies.miaosha.service.MiaoshaUserService;
import cn.fireflies.miaosha.service.OrderService;
import cn.fireflies.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {


    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);


    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receive(String message){
//        log.info("receive message:"+message);
        MiaoshaMessage mm = RedisService.stringToBean(message, MiaoshaMessage.class);
        MiaoshaUser user = mm.getUser();
        long goodsId = mm.getGoodsId();

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if (stock <=0 ){
            return;
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if (order != null){
            return;
        }
        //减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user,goods);

    }

//    /**
//     * Direct模式
//     * @param message
//     */
//    @RabbitListener(queues = MQConfig.QUEUE)
//    public void receive(String message){
//        log.info("receive message:"+message);
//    }
//
//    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
//    public void receiveTopic1(String message){
//        log.info("topic queue1 receive message:"+message);
//    }
//    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
//    public void receiveTopic2(String message){
//        log.info("topic queue2 receive message:"+message);
//    }
}
