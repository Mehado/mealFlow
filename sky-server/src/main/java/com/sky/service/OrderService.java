package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
 * 订单服务接口，定义了订单相关的业务操作
 */
public interface OrderService {

    /**
     * 提交订单
     * @param ordersSubmitDTO 订单提交数据传输对象
     * @return OrderSubmitVO 订单提交结果视图对象
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO 订单支付数据传输对象
     * @return OrderPaymentVO 订单支付结果视图对象
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    /**
     * 支付成功处理
     * @param outTradeNo 外部交易号
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户分页查询订单
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 订单状态
     * @return PageResult 分页查询结果
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 根据订单ID查询订单详情
     * @param id 订单ID
     * @return OrderVO 订单详情视图对象
     */
    OrderVO details(Long id);

    /**
     * 用户取消订单
     * @param id 订单ID
     */
    void userCancelById(Long id);

    /**
     * 重复下单
     * @param id 订单ID
     */
    void repetition(Long id);

    /**
     * 条件搜索订单
     * @param ordersPageQueryDTO 订单分页查询条件数据传输对象
     * @return PageResult 分页查询结果
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 获取订单统计数据
     * @return OrderStatisticsVO 订单统计数据视图对象
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     * @param ordersConfirmDTO 订单确认数据传输对象
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒绝接单
     * @param ordersRejectionDTO 订单拒绝数据传输对象
     * @throws Exception 可能抛出的异常
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 取消订单
     * @param ordersCancelDTO 订单取消数据传输对象
     * @throws Exception 可能抛出的异常
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 发货
     * @param id 订单ID
     */
    void delivery(Long id);

    /**
     * 完成订单
     * @param id 订单ID
     */
    void complete(Long id);

    /**
     * 订单提醒
     * @param id 订单ID
     */
    void reminder(Long id);
}
