package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author mqx
 * @date 2020/3/13 14:13
 */
@Service // spring 注解
@Slf4j
public class ManageServiceImpl implements ManageService {

    // 服务层 调用 数据访问层
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;



    // 使用mybatis-plus
    @Override
    public List<BaseCategory1> getCategory1() {
        // select * from tname ;
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        // select * from BaseCategory2 where category1Id = ?
        // 方法查询结果返回的是哪个实体类，则我们就选择哪个mapper。
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        // 第一个参数 {实体类的属性名，数据库表中的字段名}
        // 如果提示column 则一定是数据库的字段名，如果提示property 它一定的实体类的属性名称
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        List<BaseCategory2> category2List = baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
        return category2List;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        // select * from BaseCategory3 where category2Id = ?
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
        return baseCategory3List;
    }

    // 通过分类Id 查询数据
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        // 自定义mapper.xml 通过mybatis的动态标签查询
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 什么时候是修改？什么时候是添加？
        if (baseAttrInfo.getId()!=null){
            // 此时是修改操作 两个操作{修改baseAttrInfo , baseAttrValue}
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else {
            // 新增
            // 两张表：base_attr_info
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        // 在修改的时候，先删除平台属性值 然后在插入数据。
        // 打破原来的思想：修改不只是 update 的操作！ { 把原来的先删除了，然后在新增}
        // baseAttrInfo.getId() = baseAttrValue.getAttrId();

        // delete from base_attr_value where attr_id = base_attr_info.id
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        // 平台属性值添加 base_attr_value
        // 先获取到平台属性值集合数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        // 循环遍历
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 细节：给平台属性值对象添加attr_id  应该是baseAttrInfo.id
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
        // 细节？多表插入数据的时候，必须要做事务处理！
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        // attrId = base_attr_info.id
        // select * from base_attr_info where id = attrId
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        // 给attrValueList 赋值 平台属性值集合
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {

        // 第一个参数：Page 当前页， 每页显示的条数
        // 第二个参数：查询条件是什么
        // 根据三级分类Id 查询
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        // 还需要做个排序
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(pageParam,spuInfoQueryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        // select * from base_sale_attr
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
//        四张表插入 -----
//        spuInfo
//        spuImage
//        spuSaleAttr
//        spuSaleAttrValue
        spuInfoMapper.insert(spuInfo);

//      spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        //   if ( spuImageList.size()>0 && spuImageList!=null ){ 不能防止空指针！
        if (spuImageList!=null && spuImageList.size()>0){
            // 循环添加数据
            for (SpuImage spuImage : spuImageList) {
                // 细节：
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }
//      spuSaleAttr 销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            // 循环插入数据
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                // 获取销售属性值集合数据
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    // 循环插入数据
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        // spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        // 处理销售属性值中的name属性
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());

                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        // select * from spu_image where spu_id = spuId
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(spuImageQueryWrapper);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        // 一个是销售属性，一个是销售属性值 在两张表中
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
//        skuInfo
//        skuAttrValue
//        skuSaleAttrValue
//        skuImage
        skuInfoMapper.insert(skuInfo);

        // skuAttrValue 商品与平台属性关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            // 循环遍历
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        // skuSaleAttrValue 商品与销售属性关系
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
            // 循环遍历
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                // spuId 页面提交了，给skuInfo 了，所以，我们直接从spuInfo 中获取spuId

                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
        // skuImage 商品图片列表
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            // 循环遍历
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        // 发送消息实现商品的上架操作
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam) {
        // select * from sku_info order by id desc
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(pageParam,skuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        // 商品上架：update skuInfo set is_sale = 1 where id = skuId
        // 商品上架：update skuInfo set is_sale = 1 where id = 18
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        // 发送消息实现商品的上架操作
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);

    }

    @Override
    public void cancelSale(Long skuId) {
        // 商品下架：update skuInfo set is_sale = 0 where id = skuId
        // 商品下架：update skuInfo set is_sale = 0 where id = 18
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
        // 商品的下架操作
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX) // sku:
    public SkuInfo getSkuInfo(Long skuId) {
        // 使用框架redisson解决分布式锁！
        // return getSkuInfoRedisson(skuId);

        // return getSkuInfoRedis(skuId);
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            // 缓存存储数据：key-value
            // 定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 获取里面的数据？ redis 有五种数据类型 那么我们存储商品详情 使用哪种数据类型？
            // 获取缓存数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 如果从缓存中获取的数据是空
            if (skuInfo==null){
                // 直接获取数据库中的数据，可能会造成缓存击穿。所以在这个位置，应该添加锁。
                // 第二种：redisson
                // 定义锁的key sku:skuId:lock  set k1 v1 px 10000 nx
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                /*
                第一种： lock.lock();
                第二种:  lock.lock(10,TimeUnit.SECONDS);
                第三种： lock.tryLock(100,10,TimeUnit.SECONDS);
                 */
                // 尝试加锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res){
                    try {
                        // 处理业务逻辑 获取数据库中的数据
                        // 真正获取数据库中的数据 {数据库中到底有没有这个数据 = 防止缓存穿透}
                        skuInfo = getSkuInfoDB(skuId);
                        // 从数据库中获取的数据就是空
                        if (skuInfo==null){
                            // 为了避免缓存穿透 应该给空的对象放入缓存
                            SkuInfo skuInfo1 = new SkuInfo(); //对象的地址
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        // 查询数据库的时候，有值
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);

                        // 使用redis 用的是lua 脚本删除 ，但是现在用么？ lock.unlock
                        return skuInfo;

                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        // 解锁：
                        lock.unlock();
                    }
                }else {
                    // 其他线程等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else {
                // 如果用户查询的数据在数据库中根本不存在的时候第一次会将一个空对象直接放入缓存。
                // 那么第二次查询的时候，缓存中有一个空对象 防止缓存穿透
                if (null==skuInfo.getId()){
                    return null;
                }
                // 缓存数据不为空
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 为了防止缓存宕机：从数据库中获取数据
        return getSkuInfoDB(skuId);
    }

    // 使用redis' 做分布式锁
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            // 缓存存储数据：key-value
            // 定义key sku:skuId:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 获取里面的数据？ redis 有五种数据类型 那么我们存储商品详情 使用哪种数据类型？
            // 获取缓存数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 如果从缓存中获取的数据是空
            if (skuInfo==null){
                // 直接获取数据库中的数据，可能会造成缓存击穿。所以在这个位置，应该添加锁。
                // 第一种：redis ，第二种：redisson
                // 定义锁的key sku:skuId:lock  set k1 v1 px 10000 nx
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                // 定义锁的值
                String uuid = UUID.randomUUID().toString().replace("-","");
                // 上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (isExist){
                    // 执行成功的话，则上锁。
                    System.out.println("获取到分布式锁！");
                    // 真正获取数据库中的数据 {数据库中到底有没有这个数据 = 防止缓存穿透}
                    skuInfo = getSkuInfoDB(skuId);
                    // 从数据库中获取的数据就是空
                    if (skuInfo==null){
                        // 为了避免缓存穿透 应该给空的对象放入缓存
                        SkuInfo skuInfo1 = new SkuInfo(); //对象的地址
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    // 查询数据库的时候，有值
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    // 解锁：使用lua 脚本解锁
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // 设置lua脚本返回的数据类型
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // 设置lua脚本返回类型为Long
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // 删除key 所对应的 value
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);

                    return skuInfo;
                }else {
                    // 其他线程等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else {
                // 如果用户查询的数据在数据库中根本不存在的时候第一次会将一个空对象直接放入缓存。
                // 那么第二次查询的时候，缓存中有一个空对象 防止缓存穿透
                if (null==skuInfo.getId()){
                    return null;
                }
                // 缓存数据不为空
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 为了防止缓存宕机：从数据库中获取数据
        return getSkuInfoDB(skuId);
    }

    // 以下代码都是来自数据库 单独提取一个方法 ctrl+alt+m || 坑！
    private SkuInfo getSkuInfoDB(Long skuId) {
        // select * from skuInfo where id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo!=null){
            // select * from skuImage where sku_id = skuId
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            // 空对象调用方法 必然会引起一个异常!
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    @GmallCache(prefix = "CategoryView")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "SkuPrice")
    public BigDecimal getSkuPrice(Long skuId) {
        // select price from skuInfo where id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo!=null){
            return skuInfo.getPrice();
        }
        return new BigDecimal(0);
    }

    @Override
    @GmallCache(prefix = "SpuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    @GmallCache(prefix = "skuValueIdsMap")
    public Map getSkuValueIdsMap(Long spuId) {
        // 声明一个map 集合来存储数据
        HashMap<Object, Object> hashMap = new HashMap<>();
        // 使用哪个mapper？ 看哪张表种有我们需要的字段，就用哪个mapper
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList!=null && mapList.size()>0){
            // 循环取值
            for (Map map : mapList) {
//                String key = (String) map.get("value_ids");
//                String value = (String) map.get("sku_id");
//                Object key = map.get("value_ids");
//                Object sku_id = map.get("sku_id");
                // 将key ，value 放入map
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }
        return hashMap;
    }

    @Override
    @GmallCache(prefix = "category")
    public List<JSONObject> getBaseCategoryList() {
        // 声明Json集合对象
        List<JSONObject> list = new ArrayList<>();
        // 1.   先查询所有的分类数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 按照一级分类Id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 开始准备构建
        int index=1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            // 初始化一级分类对象 categoryId,categoryName 等数据进行初始化
            // 获取一级分类Id
            Long category1Id = entry1.getKey();
            // 获取一级分类下的所有集合数据
            List<BaseCategoryView> category2List1 = entry1.getValue();

            JSONObject category1 = new JSONObject();
            // 准备赋值
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            category1.put("categoryName",category2List1.get(0).getCategory1Name());
            // categoryChild
            // category1.put("categoryChild",{}); 留意一下：
            // 迭代index
            index++;
            // 获取二级分类数据集合
            Map<Long, List<BaseCategoryView>> category2Map = category2List1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 声明一个二级分类的集合对象
            List<JSONObject> category2Child = new ArrayList<>();
            // 循环二级分类里面的数据
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 准备二级分类对象，然后给其进行赋值
                JSONObject category2 = new JSONObject();
                // 获取二级分类Id
                Long category2Id = entry2.getKey();

                // 获取二级分类下的所有数据
                List<BaseCategoryView> category3List1 = entry2.getValue();
                // 赋值 categoryId ，categoryName
                category2.put("categoryId",category2Id);
                category2.put("categoryName",category3List1.get(0).getCategory2Name());
                // category2.put("categoryChild",{});  后面添加
                category2Child.add(category2);

                // 处理三级分类数据！
                // 声明一个三级分类数据集合
                List<JSONObject> category3Child = new ArrayList<>();
                // 循环获取三级分类数据
                category3List1.stream().forEach(category3View ->{
                    // 声明一个三级分类对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    // 将每个三级分类对象添加到集合
                    category3Child.add(category3);
                });
                // 处理 category2.put("categoryChild",{});  后面添加
                category2.put("categoryChild",category3Child);
            }
            // category1.put("categoryChild",{}); 留意一下：
            category1.put("categoryChild",category2Child);
            // 将所有的一级分类数据应该添加到集合
            list.add(category1);
        }
        // 存储的每一级的分类数据
        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        // select * from basetrademark where id = tmId
        return baseTrademarkMapper.selectById(tmId);
    }

    // 根据商品Id 查询 对应的平台属性，以及平台属性值。
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //  设计到多表关联！ 自定义方法来完成业务！
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    // 通过attrId 获取数据
    private List<BaseAttrValue> getAttrValueList(Long attrId){
        // select * from base_attr_value where attr_id = attrId;
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
        return baseAttrValueList;
    }

}
