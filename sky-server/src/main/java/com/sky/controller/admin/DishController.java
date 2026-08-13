package com.sky.controller.admin;

import com.sky.annotations.RequireRole;
import com.sky.constant.RoleConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Tag(name="菜品管理",description = "提供菜品相关的API接口")
public class DishController {


    private final DishService dishService;

    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @RequireRole
    @PostMapping
    @Operation(summary = "新增菜品")
    public Result save(@Valid @RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success("新增菜品成功");

    }

    /**
     * 分页查询菜品
     *
     * @param dishPageQueryDTO
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF})
    @GetMapping("/page")
    @Operation(summary = "分页查询菜品")
    public Result<PageResult> page(@Valid DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id批量删除菜品
     *
     * @return
     * @RequestBody List<Long> ids
     */
    @RequireRole
    @DeleteMapping
    @Operation(summary = "根据id批量删除菜品")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("根据id批量删除菜品:{}", ids);
        dishService.deleteBatch(ids);
        return Result.success("删除菜品成功");
    }

    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF})
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品:{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     *
     * @return
     * @RequestBody DishDTO dishDTO
     */
    @RequireRole
    @PutMapping
    @Operation(summary = "修改菜品")
    public Result update(@Valid @RequestBody DishDTO dishDTO) {
        log.info("修改菜品:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success("修改菜品成功");
    }

    /**
     * 菜品起售停售
     * @PathVariable Integer status
     * @param id
     * @return
     */
    @RequireRole
    @PostMapping("/status/{status}")
    @Operation(summary = "菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status,Long id){
        dishService.startOrStop(status,id);
        return Result.success("修改菜品状态成功");
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF})
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> list=dishService.list(categoryId);
        return Result.success(list);
    }
}
