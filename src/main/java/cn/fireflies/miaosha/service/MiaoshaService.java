package cn.fireflies.miaosha.service;

import cn.fireflies.miaosha.dao.GoodsDao;
import cn.fireflies.miaosha.domain.Goods;
import cn.fireflies.miaosha.domain.MiaoshaOrder;
import cn.fireflies.miaosha.domain.MiaoshaUser;
import cn.fireflies.miaosha.domain.OrderInfo;
import cn.fireflies.miaosha.redis.MiaoshaKey;
import cn.fireflies.miaosha.redis.RedisService;
import cn.fireflies.miaosha.utils.MD5Util;
import cn.fireflies.miaosha.utils.UUIDUtil;
import cn.fireflies.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

@Service
public class MiaoshaService {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    @Transactional
    public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
        //减库存 下订单 写入秒杀订单
//        if(goodsService.reduceStock(goods)){
//            //当多个线程同时请求数据库减库存时，会超卖，需要在sql语句加上大于零判断条件
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//        }

        boolean success = goodsService.reduceStock(goods);
        if (success){
            //order_info  miaosha_order
            return orderService.createOrder(user,goods);
        }else {
            setGoodsOver(goods.getId());//内存标记,商品已经秒杀完了
            return null;
        }
    }

    private void setGoodsOver(Long goodsId) {
        redisService.set(MiaoshaKey.isGoodsOver,""+goodsId,true);//卖完了在redis中记录为true

    }
    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(MiaoshaKey.isGoodsOver,""+goodsId);
    }

    public long getMiaoshaResult(Long userId, long goodsId) {
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);

//        System.out.println(order);

        if (order != null){
            //秒杀成功
            return order.getOrderId();
        }else {
            boolean isOver = getGoodsOver(goodsId);//判断商品是否卖完了
            if (isOver){
                //卖完了，秒杀失败
                return -1;
            }else {
                return 0;//商品没有卖完，继续轮询
            }
        }
    }


    public String createMiaoshaPath(MiaoshaUser user,long goodsId) {
        if (user==null || goodsId<=0){
            return null;
        }
        String str = MD5Util.md5(UUIDUtil.uuid())+"123456";
        //保存到redis中
        redisService.set(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,str);
        return str;
    }

    public boolean checkPath(long goodsId, MiaoshaUser user, String path) {
        if (user==null || path==null){
            return false;
        }
        String pathOld = redisService.get(MiaoshaKey.getMiaoshaPath,""+user.getId()+"_"+goodsId,String.class);
        return path.equals(pathOld);
    }

    public BufferedImage createMiaoshaVerifyCode(MiaoshaUser user, long goodsId) {
        if (user==null || goodsId<=0){
            return null;
        }
        int width = 80;
        int height = 32;
        BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(new Color(0xDCDCDC));
        g.fillRect(0,0,width,height);
        g.setColor(Color.BLACK);
        g.drawRect(0,0,width-1,height-1);
        Random rdm = new Random();
        for (int i=0;i<50;i++){
            int x = rdm.nextInt(width);
            int y =rdm.nextInt(height);
            g.drawOval(x,y,0,0);
        }
        String verifyCode = generateVerifyCode(rdm);
        g.setColor(new Color(0,100,0));
        g.setFont(new Font("Candara",Font.BOLD,24));
        g.drawString(verifyCode,8,24);
        g.dispose();
        int rnd = calc(verifyCode);
        redisService.set(MiaoshaKey.getMiaoshaVerifyCode,user.getId()+","+goodsId,rnd);
        return image;
    }

    //计算表达式结果
    private int calc(String exp) {
        try{
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer) engine.eval(exp);
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    //生成表达式
    private static char[] ops = new char[]{'+','-','*'};
    private String generateVerifyCode(Random random) {
        int num1 = random.nextInt(10);
        int num2 = random.nextInt(10);
        int num3 = random.nextInt(10);
        char op1 = ops[random.nextInt(3)];
        char op2 = ops[random.nextInt(3)];
        String exp = ""+ num1 + op1 + num2 + op2 + num3;
        return exp;
    }

    public boolean checkVerifyCode(MiaoshaUser user, long goodsId, int verifyCode) {
        if (user==null || goodsId<=0){
            return false;
        }
        Integer codeOld = redisService.get(MiaoshaKey.getMiaoshaVerifyCode,user.getId()+","+goodsId,Integer.class);
        if (codeOld == null || codeOld-verifyCode!=0){
            return false;
        }
        //验证成功之后删掉验证码
        redisService.delete(MiaoshaKey.getMiaoshaVerifyCode,user.getId()+","+goodsId);
        return true;
    }
}
