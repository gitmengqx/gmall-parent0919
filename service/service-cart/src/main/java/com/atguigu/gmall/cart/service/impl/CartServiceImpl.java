package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author mqx
 * @date 2020/3/27 9:47
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    // 存储的是每个购物车的对象
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        1.  如果用是第一次购买商品 直接添加到购物车。
        2.  如果不是第一次购买，在购物车中有该商品了，商品数量应该相加
        3.  都应该将数据保存到数据库，同时应该放入缓存！

        -------------------------------
        购物车中是否有该商品
            true: 数量相加
            false: 直接添加

           insert-mysql
           insert-redis.
         */
        // 需要更新缓存
        // 确定用什么数据类型来存储购物车 Hash 数据结构：
        // hset(key,field,value) key=user:userId:cart field=skuId value=cartInfo.toString()
        // 获取cartKey
        String cartKey = getCartKey(userId);
        if (!redisTemplate.hasKey(cartKey)){
            // 加载数据库的数据到缓存
            loadCartCache(userId);
        }

        // 查询购物车是否有该商品 需要以用户Id为基准，还需要以商品Id为基准。
        // select * from cart_info where user_id = ? sku_id = ? || 或者登录 selectOne
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id",skuId).eq("user_id",userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        // 说明购物车中有该商品
        if (cartInfoExist!=null){
            // 商品数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 因为skuPrice 在数据库中不存在，它又表示实时价格，所以需要查询一下。
            // 实时价格 = skuInfo.price
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);
            // 放入数据库
            cartInfoMapper.updateById(cartInfoExist);
            // 需要更新缓存
        } else {
            // 购物车没有该商品
            // 获取当前的商品信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            // 将当前商品的信息赋值给cartInfo
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuPrice(skuInfo.getPrice()); // 开始添加购物车时，实时价格就是商品价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            // 放入数据库
            cartInfoMapper.insert(cartInfo);

            // 需要更新缓存
            cartInfoExist = cartInfo;
        }


        // 向缓存中放入数据
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);

        // 如果使用消息队列做成异步！ 发送一个 mq 消息。 消息消费者{得到把mysql数据查询来，直接放入缓存}
        // 还需要设置一个缓存中购物车的过期时间
        setCartKeyExpire(cartKey);

    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 无论是哪个用户Id ，数据都应该先查询缓存
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 用户未登录
        if (StringUtils.isEmpty(userId)){
            // 获取未登录购物车集合数据
            cartInfoList = getCartList(userTempId);
            // return cartInfoList;
        }
        // 用户登录情况
        if (!StringUtils.isEmpty(userId)){
            // 登录的时候合并购物车：
            // 获取未登录购物车数据
            List<CartInfo> cartList = getCartList(userTempId);
            // 未登录购物车数据不是空的
            if (!CollectionUtils.isEmpty(cartList)){
                // 此时开始合并购物车
                // 第一个参数未登录购物车数据，第二个参数是用户Id{可以通过用户Id 查询登录数据}
                cartInfoList = mergeToCartList(cartList,userId);
                // 删除未登录购物车数据
                deleteCartList(userTempId);
            }
            // 如果未登录购物车集合中的数据是空的，或者是说根本没有临时用户id
            if (CollectionUtils.isEmpty(cartList) || StringUtils.isEmpty(userTempId)){
                cartInfoList = getCartList(userId);
            }
        }
        // return cartInfoList;
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // 修改数据库
        // 第一个参数cartInfo 表示修改的内容，第二个参数表示根据什么条件修改
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        // update cart_info set is_checked = ? where user_id=userId and sku_id=skuId;
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,cartInfoQueryWrapper);

        // 修改缓存
        // 必须先获取到缓存的key
        String cartKey = getCartKey(userId);
        // 获取缓存中的所有数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())){
            // 获取缓存中的数据
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            // 修改数据
            cartInfoUpd.setIsChecked(isChecked);
            // 还需要将修改之后的对象再放入缓存
            boundHashOperations.put(skuId.toString(),cartInfoUpd);
            // 修改了缓存，那么我们就需要再次给一个过期时间
            setCartKeyExpire(cartKey);
        }

    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        /*
        1.  删除购物车从页面开始 购物车数据存储在mysql ，redis
        2.  处理两个位置 mysql{delete sql}，redis{delete 命令}
         */
        // delete from cart_info where user_id=userId and sku_id=skuId
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        // 构造删除条件的
        cartInfoQueryWrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.delete(cartInfoQueryWrapper);

        // 处理缓存 存储的hash 数据类型 key=user:userId:cart , field = skuId ,value = cartInfo
        // 先获取到缓存中存储购物车的key
        String cartKey = getCartKey(userId);
        // 获取cartKey 中所对应的数据 {获取单个商品的field呢? 还是获取所有的field ，然后判断购物车中是否有当前商品的field ？}
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())){
            // 删除购物车中所对应的商品
            boundHashOperations.delete(skuId.toString());
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 查询购物车中选中的商品！
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 去缓存查询 需不需要查询数据库？
        String cartKey = getCartKey(userId);
        List<CartInfo> cartInfoCheckedList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoCheckedList)){
            for (CartInfo cartInfo : cartInfoCheckedList) {
                // 选中商品 isChecked = 1
                if (cartInfo.getIsChecked().intValue()==1){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    // 删除购物车数据
    private void deleteCartList(String userTempId) {
        // 删除购物车 数据存在 mysql + redis
        // delete from cart_info where user_id = ?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userTempId);
        cartInfoMapper.delete(cartInfoQueryWrapper);

        // 删除缓存
        // 获取到购物车key
        String cartKey = getCartKey(userTempId);
        // 判断cartKey 在缓存是否存在
        if (redisTemplate.hasKey(cartKey)){
            // 删除数据即可！
            redisTemplate.delete(cartKey);
        }


    }

    /**
     * 合并购物车
     * @param cartList 未登录购物车数据
     * @param userId 用户Id
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartList, String userId) {
        /*
    demo1:
        登录：
            37 1
            38 1
        未登录：
            37 1
            38 1
            39 1
        合并之后的数据
            37 2
            38 2
            39 1
     demo2:
         未登录：
            37 1
            38 1
            39 1
            40 1
          合并之后的数据
            37 1
            38 1
            39 1
            40 1
     */

        // 1.获取到用户登录购物车数据
        List<CartInfo> cartListLogin = getCartList(userId);
        // 将cartListLogin 登录的用户数据集合转换成map集合map 中的key = skuId,value=cartInfo
        Map<Long, CartInfo> cartInfoMapLogin = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        // 需要根据商品Id 进行判断
        // 循环未登录的购物车
        for (CartInfo cartInfoNoLogin : cartList) {
            // 获取未登录购物车中的商品Id
            Long skuId = cartInfoNoLogin.getSkuId();
            // 判断登录中的购物车数据是否有相同的商品Id
            if (cartInfoMapLogin.containsKey(skuId)){ // 说明登录的购物车中有未登录购物车的数据
                // 开始做数量相加 获取登录的购物车对象
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                // 合并的时候，需要注意购物车选中状态！
                // 登录的时候，有选中，未选中，未登录也可以有选中，未选中！
                // 如果未登录购物车中的商品被选中了，那么我们需要将数据库中的状态改为选中
                if (cartInfoNoLogin.getIsChecked().intValue()==1){
                    cartInfoLogin.setIsChecked(1);
                }
                // 更新数据库
                cartInfoMapper.updateById(cartInfoLogin);
            } else {
                //  未登录购物车与登录购物车中的商品id 不一致。 未登录有这个商品，登录没有这个商品
                //  这种情况下应该直接添加到数据 未登录的用户Id 是临时的。应该将用户Id 变成登录的用户Id
                //  cartInfoNoLogin.setId(null); // 为了确保在插入数据的时候，能够让id 自动增长！
                cartInfoNoLogin.setUserId(userId);
                //  插入数据库
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        // 数据汇总：
        List<CartInfo> cartInfoList = loadCartCache(userId);

        return cartInfoList;
    }

    //  根据用户ID{登录，未登录} 获取购物车集合
    private List<CartInfo> getCartList(String userId) {
        // 声明一个集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 根据用户Id 获取缓存数据
        if (StringUtils.isEmpty(userId)) return cartInfoList;
        // 购物车数据应该先查询缓存 缓存中key 是如何定义的。
        // 获取购物车缓存的key user:userId:cart
        String cartKey = getCartKey(userId);
        // 根据cartKey 获取缓存数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)){
            // 商品列表展示的时候，实际上应该有个顺序，那么这个顺序是谁？按照什么排序的？ 按照update Time
            // 给集合做排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                // 模拟对商品的排序 按照Id 排序 | 实际项目按照update Time
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // Long ，String 比较方法
                    return o1.getId().compareTo(o2.getId());
                }
            });
            // 将从缓存中获取到的集合数据并排序 返回
            return cartInfoList;
        } else {
            // 如果缓存中没有数据了。那么我们可以从数据库中获取数据，并加载到缓存
            cartInfoList = loadCartCache(userId);
            // 返回集合数据
            return cartInfoList;
        }
    }

    // 表示根据用户Id 查询数据库，然后将数据放入缓存
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cart_info where  user_id = userId
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        // 根据业务需求将数据还要放入缓存
        if (CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        // 确定该集合中一定有数据，那么放入缓存

        // 获取购物车的key
        String cartKey = getCartKey(userId);
        // 声明一个map集合来存储数据
        HashMap<String, CartInfo> map = new HashMap<>();
        // map.put(field,value) field = skuId ,value =cartInfo.toString();
        for (CartInfo cartInfo : cartInfoList) {
            // 既然查询了数据库，那么说明缓存应该过期了。有可能会出现价格变动。
            // 查询最新的价格给当前的对象
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            // 将数据放入map
            map.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        // 放入缓存数据
        redisTemplate.opsForHash().putAll(cartKey,map);
        // 设置缓存的过期时间
        setCartKeyExpire(cartKey);
        // 从数据库中查询到数据并返回
        return cartInfoList;
    }

    // 设置缓存的过期时间
    private void setCartKeyExpire(String cartKey){
        redisTemplate.expire(cartKey,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    // 获取购物车的key
    public String getCartKey(String userId){
        // user:userId:cart
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
