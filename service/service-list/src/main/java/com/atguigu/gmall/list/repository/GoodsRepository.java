package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author mqx
 * @date 2020/3/23 15:38
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
