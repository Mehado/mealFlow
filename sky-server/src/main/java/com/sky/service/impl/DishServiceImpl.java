package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Cacheable(cacheNames = "dishCache",key="#dishDTO.categoryId")
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);
        //获取Insert语句生成的主键值
        Long dishId = dish.getId();

        //向菜品口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if ((flavors != null) && (!flavors.isEmpty())) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }


/**
 * 菜品分页查询方法
 * @param dishPageQueryDTO 菜品分页查询条件数据传输对象，包含分页参数和查询条件
 * @return PageResult 分页查询结果，包含菜品数据列表和分页信息
 */

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 根据id批量删除菜品
     * @param ids
     */
    @CacheEvict(cacheNames = "dishCache",allEntries = true)
    @Transactional
    public void deleteBatch(List<Long> ids) {
        log.info("根据id批量删除菜品:{}",ids);
        //判断当前菜品是否可以删除--是否在起售中
        for(Long id:ids){
            Dish dish=dishMapper.getById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否可以删除--是否有关联的订单
        List<Long> setmealIds=setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds!=null && !setmealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品和菜品口味
//        for (Long id : ids) {
//                dishMapper.deleteById(id);
//                dishFlavorMapper.deleteByDishId(id);
//        }
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品对应信息和口味
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        log.info("根据id查询菜品对应信息和口味:{}",id);
        //根据id查询菜品信息
        Dish dish=dishMapper.getById(id);
        //根据id查询菜品口味信息
        List<DishFlavor> dishflavors=dishFlavorMapper.getByDishId(id);

        //将菜品信息和口味信息封装到DishVO对象中
        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishflavors);

        return dishVO;
    }

    /**
     * 修改菜品信息和口味
     * @param dishDTO
     */
    @CacheEvict(cacheNames = "dishCache",allEntries = true)
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //修改菜品表基本信息
        dishMapper.update(dish);
        //删除菜品口味表信息
        dishFlavorMapper.deleteByDishId(dish.getId());
        //新增菜品口味表信息
        List<DishFlavor> flavors=dishDTO.getFlavors();
        if ((flavors != null) && (!flavors.isEmpty())) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类ID，用于指定要查询的菜品分类
     * @return 返回指定分类下的菜品列表
     */
    @Override  // 标记该方法覆盖了父类或接口中的同名方法
    public List<Dish> list(Long categoryId) {  // 方法声明，返回菜品列表，接受一个分类ID参数
        log.info("根据分类id查询菜品:{}",categoryId);  // 记录日志，输出查询的分类ID
    // 创建Dish对象，设置分类ID和状态为启用
         Dish dish =Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
    // 调用dishMapper的list方法，查询符合条件的菜品列表
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    @Cacheable(cacheNames="dishCache",key="#dish.categoryId")
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 菜品起售停售详情
     * @param status
     * @param id
     */
    @CacheEvict(cacheNames = "dishCache",allEntries = true)
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }
}