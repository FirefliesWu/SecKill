package cn.fireflies.miaosha.redis;

public class AccsessKey extends BasePrefix{
    public AccsessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }
    public static AccsessKey accessLimit = new AccsessKey(5,"access");//key过期时间5秒

    public static AccsessKey withExpire(int expireSeconds){
        return new AccsessKey(expireSeconds,"access");
    }
}
