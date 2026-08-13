package com.sky.controller.admin;


import com.sky.annotations.RequireRole;
import com.sky.constant.RoleConstant;
import com.sky.result.Result;
import com.sky.service.ShopService;
import com.sky.service.impl.ShoppingCartServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Tag(name = "后台管理-店铺管理")
@RequiredArgsConstructor
public class ShopController {


    private final ShopService shopService;

    private final ShoppingCartServiceImpl shoppingCartServiceImpl;

    /**
     * 设置店铺状态
     * @param status
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER})
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态：{}",status==1?"":"关闭");
        shopService.setStatus(status);
        return Result.success();
    }

    /**
     * 获取店铺状态
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER})
    @GetMapping("/status")
    @Operation(summary = "获取店铺状态")
    public Result<Integer> getStatus(){
        Integer status = shopService.getStatus();
        log.info("获取店铺状态为：{}",status==1?"营业中":"打烊中");
        return Result.success(status);
    }

}
