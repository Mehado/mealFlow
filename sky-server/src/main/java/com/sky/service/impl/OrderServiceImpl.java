package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /**
     * 订单数据访问对象
     */
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 订单详情数据访问对象
     */
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    /**
     * 购物车数据访问对象
     */
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    /**
     * 用户数据访问对象
     */
    @Autowired
    private UserMapper userMapper;
    /**
     * 地址簿数据访问对象
     */
    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 店铺地址
     */
    @Value("${sky.shop.address}")
    private String shopAddress;

    /**
     * 百度地图API密钥
     */
    @Value("${sky.baidu.ak}")
    private String ak;


    //幂等token消费脚本：token存在且属于当前用户才删除并返回1，否则返回0
    private static final DefaultRedisScript<Long> IDEMPOTENT_SCRIPT = new DefaultRedisScript<>();

    static {
        IDEMPOTENT_SCRIPT.setResultType(Long.class);
        IDEMPOTENT_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end"
        );
    }

    /**
     * 提交订单
     * @param ordersSubmitDTO 提交订单的数据传输对象
     * @return 订单提交后的视图对象
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 检查幂等令牌
        Long consumed=stringRedisTemplate.execute(
                IDEMPOTENT_SCRIPT,
                Collections.singletonList("order:submit:token:"+ordersSubmitDTO.getToken()),
                String.valueOf(BaseContext.getCurrentId()));
                if(consumed==null|| consumed!=1L){
                    throw new OrderBusinessException("请勿重复提交订单");
                }

        // 获取地址簿信息
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 检查是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        // 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        // 获取用户的购物车列表
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 创建订单对象
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        // 插入订单
        orderMapper.insert(orders);

        // 创建订单详情列表
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        // 批量插入订单详情
        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 构建订单提交视图对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO 订单支付数据传输对象
     * @return 订单支付视图对象
     * @throws Exception 支付过程中可能出现的异常
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 处理支付成功逻辑
        paySuccess(ordersPaymentDTO.getOrderNumber());

        // 构建支付视图对象
        return OrderPaymentVO.builder()
                .nonceStr("mock_nonce")
                .paySign("mock_pay_sign")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("RSA")
                .packageStr("prepay_id=mock_prepay_id")
                .build();
    }

    /**
     * 支付成功处理
     * @param outTradeNo 交易编号
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单编号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 更新订单状态为待接单，支付状态为已支付
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 发送WebSocket消息给厨师
        Map map=new HashMap();
        map.put("type","1");
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号: "+outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户订单分页查询
     * @param pageNum 页码
     * @param pageSize 页面大小
     * @param status 订单状态
     * @return 分页结果对象
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 使用PageHelper进行分页
        PageHelper.startPage(pageNum, pageSize);
        // 构建查询条件
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 执行分页查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 转换为VO列表
        List<OrderVO> list = new ArrayList<>();

        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long ordersId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(ordersId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 获取订单详情
     * @param id 订单ID
     * @return 订单视图对象
     */
    public OrderVO details(Long id) {
        // 获取订单基本信息
        Orders orders = orderMapper.getById(id);

        //判空校验
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //归属校验
        if(!orders.getUserId().equals(BaseContext.getCurrentId())){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);}
        // 获取订单详情列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 构建订单视图对象
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id 订单ID
     * @throws Exception 取消过程中可能出现的异常
     */
    public void userCancelById(Long id) {
        // 查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);}


            // 检查订单状态是否可以取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 构建更新对象
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 如果订单状态为待接单，设置支付状态为退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

/**
 * 重复订单功能方法
 * 将已存在的订单详情转换为购物车项目，并批量插入到当前用户的购物车中
 * @param id 订单ID，用于获取该订单的详细信息
 */
    public void repetition(Long id) {
        //先查订单，做归属校验
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
    // 获取当前登录用户的ID
        Long userId = BaseContext.getCurrentId();

    // 根据订单ID查询该订单的所有订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

    // 将订单详情列表转换为购物车列表
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
        // 创建新的购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();
        // 复制订单详情的属性到购物车对象，排除id属性
            BeanUtils.copyProperties(x, shoppingCart, "id");
        // 设置购物车用户ID为当前用户ID
            shoppingCart.setUserId(userId);
        // 设置购物车创建时间为当前时间
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

    // 批量插入购物车列表到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

/**
 * 根据条件进行订单分页查询
 * @param ordersPageQueryDTO 订单分页查询条件DTO，包含页码、页面大小等查询参数
 * @return PageResult 包含总记录数和订单VO列表的分页结果对象
 */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
    // 使用PageHelper设置分页参数，开始分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

    // 调用订单Mapper进行分页查询，获取订单数据
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

    // 将查询到的订单数据转换为VO对象列表
        List<OrderVO> orderVOList = getOrderVOList(page);

    // 返回分页结果，包含总记录数和VO列表
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 获取订单VO列表
     * @param page 订单分页对象
     * @return 订单VO列表
     */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 获取订单菜品字符串
     * @param orders 订单对象
     * @return 菜品字符串
     */
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        return String.join("", orderDishList);
    }

    /**
     * 获取订单统计信息
     * @return 订单统计视图对象
     */
    public OrderStatisticsVO statistics() {
        // 统计各状态订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 构建统计视图对象
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 确认订单
     * @param ordersConfirmDTO 订单确认数据传输对象
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 更新订单状态为已确认
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒绝订单
     * @param ordersRejectionDTO 订单拒绝数据传输对象
     * @throws Exception 拒绝过程中可能出现的异常
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 查询订单信息
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 如果订单已支付，设置支付状态为退款
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            ordersDB.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态为已拒绝
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO 订单取消数据传输对象
     * @throws Exception 取消过程中可能出现的异常
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 查询订单信息
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 如果订单已支付，设置支付状态为退款
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            ordersDB.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态为已取消
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 发货
     * @param id 订单ID
     */
    public void delivery(Long id) {
        // 查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为配送中
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id 订单ID
     */
    public void complete(Long id) {
        // 查询订单信息
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 更新订单状态为已完成
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客户催单
     * @param id 订单ID
     */
    public void reminder(Long id) {
        Orders ordersDB=orderMapper.getById(id);
        if (ordersDB == null) {
                       throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
                   }
        // 新增：归属校验
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map=new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","您的订单"+ordersDB.getNumber()+"已被催单");

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 获取下单幂等令牌:生成随机UUID，存入redis，10分钟有效
     */
    public String getSubmitToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        Long userId = BaseContext.getCurrentId();
        stringRedisTemplate.opsForValue().set(
                "order:submitToken:" + token,
                String.valueOf(userId),
                10, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 检查是否超出配送范围（模拟实现）
     * @param address 地址
     */
    private void checkOutOfRange(String address) {
        // 模拟实现：直接返回成功，不做真实的距离计算
        // 如果需要模拟超出配送范围的情况，可以根据地址关键词判断
        if (address != null && (address.contains("外省") || address.contains("偏远"))) {
            throw new OrderBusinessException("超出配送范围");
        }
        // 其他情况都认为在配送范围内
        log.info("模拟检查配送范围：地址 {} 在配送范围内", address);
    }
}
