package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collections;
import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id集合批量查询订单明细
     * @return 订单明细列表
     */
//    @Select("<script>" +
//            "select * from order_detail where order_id in " +
//            "<foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>" +
//            "#{orderId}" +
//            "</foreach>" +
//            "</script>")
    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);

    // 新增：查询单个（直接复用上面的 XML）
    default List<OrderDetail> getByOrderId(Long orderId) {
        return getByOrderIds(Collections.singletonList(orderId));
    }
}
