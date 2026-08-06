package com.sky.controller.admin;

import com.sky.annotations.RequireRole;
import com.sky.constant.RoleConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Tag(name = "套餐管理")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @param setmealDTO 套餐信息
     * @return
     */
    @RequireRole
    @PostMapping
    @Operation(summary = "新增套餐")
    @CacheEvict(cacheNames="setmealCache",key="#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐:{}",setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }
    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF})
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        PageResult pageResult= setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 根据id批量删除套餐   
     * @param ids
     * @return
     */
    @RequireRole
    @DeleteMapping
    @Operation(summary = "根据id批量删除套餐")
    @CacheEvict(cacheNames="setmealCache",allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("根据id批量删除套餐:{}",ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据id查询套餐，用于修改页面的回显数据
     * @param id
     * @return
     */
    @RequireRole({RoleConstant.OWNER,RoleConstant.CASHIER,RoleConstant.CHEF})
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        SetmealVO setmealVO=setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }
    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @RequireRole
    @PutMapping
    @Operation(summary = "修改套餐")
    @CacheEvict(cacheNames="setmealCache",allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐:{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }
    /**
     * 套餐起售停售
     * @param status
     * @param id
     * @return
     */
    @RequireRole
    @PostMapping("/status/{status}")
    @Operation(summary = "套餐起售停售")
    @CacheEvict(cacheNames="setmealCache",allEntries = true)
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("套餐起售停售:status={},id={}",status,id);
        setmealService.startOrStop(status,id);
        return Result.success();
    }
}
