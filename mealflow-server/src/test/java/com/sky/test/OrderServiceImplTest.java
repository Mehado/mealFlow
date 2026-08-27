package com.sky.test;


import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.service.StockService;
import com.sky.service.impl.OrderServiceImpl;
import com.sky.vo.OrderSubmitVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * submitOrder 单元测试
 * 只mock被测方法用到的依赖，用不到的不mock
 * @InjectMocks 会给它们留null，只要被测方法不碰就行
 */
@ExtendWith(MockitoExtension.class)

public class OrderServiceImplTest {
    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private AddressBookMapper addressBookMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<Long> idempotentTokenScript;

    @Mock
    private UserMapper userMapper;
    @Mock
    private WebSocketServer webSocketServer;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private StockService stockService;



    @BeforeEach
    void setUp(){
        BaseContext.setCurrentId(1L);
    }
    @AfterEach
    void tearDown(){
        BaseContext.removeCurrentId();
    }

    /** 用例1：幂等token消费失败->>抛出异常 ，并且地址簿查询都不该发生*/
    @Test
    void submitOrder_tokenConsumedFail_shouldThrow() {
        mockTokenConsumed(0L);
        OrdersSubmitDTO ordersSubmitDTO = new OrdersSubmitDTO();
        ordersSubmitDTO.setToken("token-xxx");

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(ordersSubmitDTO));
        //验证：token消费失败后，连地址簿查询都不该发生
        verifyNoInteractions(addressBookMapper);
    }
    /** 用例2：地址簿为空-> 抛AddressBookBusinessException*/
    @Test
    void submitOrder_addressBookNull_shouldThrow() {
        mockTokenConsumed(1L);
        when(addressBookMapper.getById(anyLong())).thenReturn(null);

        OrdersSubmitDTO ordersSubmitDTO = new OrdersSubmitDTO();
        ordersSubmitDTO.setToken("token-xxx");
        ordersSubmitDTO.setAddressBookId(100L);
        assertThrows(AddressBookBusinessException.class,()->orderService.submitOrder(ordersSubmitDTO));
    }
    /** 用例3：购物车为空 → 抛异常，且还没走到插入订单那一步 */
    @Test
    void submitOrder_cartEmpty_shouldThrow() {
        mockTokenConsumed(1L);
        when(addressBookMapper.getById(anyLong())).thenReturn(buildAddressBook());
        when(shoppingCartMapper.list(any())).thenReturn(Collections.emptyList());

        assertThrows(AddressBookBusinessException.class, () -> orderService.submitOrder(buildValidDto()));

        verifyNoInteractions(orderMapper);
    }

    /** 用例4：全部正常 → 返回 VO，且插入订单/明细、清购物车三步都执行了 */
    @Test
    void submitOrder_allValid_shouldReturnVO() {
        mockTokenConsumed(1L);
        when(addressBookMapper.getById(anyLong())).thenReturn(buildAddressBook());

        ShoppingCart cart = new ShoppingCart();
        cart.setName("宫保鸡丁");
        cart.setNumber(1);
        cart.setAmount(new BigDecimal("28.00"));
        when(shoppingCartMapper.list(any())).thenReturn(Collections.singletonList(cart));

        OrderSubmitVO vo = orderService.submitOrder(buildValidDto());

        assertNotNull(vo);
        assertNotNull(vo.getOrderNumber());   // 订单号由 System.currentTimeMillis() 生成，必非空
        verify(orderMapper).insert(any(Orders.class));
        verify(orderDetailMapper).insertBatch(anyList());
        verify(shoppingCartMapper).deleteByUserId(1L);
        verify(stockService).deductStock(anyList());
    }

    /** 用例5：地址含"外省" → 模拟配送范围校验抛异常 */
    @Test
    void submitOrder_outOfRange_shouldThrow() {
        mockTokenConsumed(1L);
        AddressBook farAddress = buildAddressBook();
        farAddress.setCityName("外省");        // 触发 checkOutOfRange 的模拟判断
        when(addressBookMapper.getById(anyLong())).thenReturn(farAddress);

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(buildValidDto()));
    }

    // ---------- 测试数据构造 ----------

    private void mockTokenConsumed(Long result) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(result);
    }

    private AddressBook buildAddressBook() {
        AddressBook addressBook = new AddressBook();
        addressBook.setCityName("北京市");
        addressBook.setDistrictName("朝阳区");
        addressBook.setDetail("幸福路100号");
        addressBook.setConsignee("张三");
        return addressBook;
    }

/**
 * 构建一个有效的订单提交DTO对象
 * @return 返回一个配置好的OrdersSubmitDTO对象，包含订单提交所需的各项信息
 */
    private OrdersSubmitDTO buildValidDto() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setToken("token-xxx");
        dto.setAddressBookId(1L);
        dto.setAmount(new BigDecimal("28.00"));
        dto.setPackAmount(0);
        dto.setTablewareNumber(1);
        return dto;
    }

}
