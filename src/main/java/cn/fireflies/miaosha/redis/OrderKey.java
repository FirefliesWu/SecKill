package cn.fireflies.miaosha.redis;

public class OrderKey extends BasePrefix{
    public OrderKey(String prefix) {
        //订单缓存永久不过期
        super(prefix);
    }
    public static OrderKey getMiaoshaOrderByUidGid = new OrderKey("moug");
}
