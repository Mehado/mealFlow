package com.sky.service.impl;

import com.alibaba.fastjson.TypeReference;
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
import com.sky.utils.RedisCacheClient;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;

    private final DishFlavorMapper dishFlavorMapper;

    private final RedisCacheClient cacheClient;

    private final SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
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
        //cache-Aside:先写库，再删除缓存
        cacheClient.delete(dishCacheKey(dishDTO.getCategoryId()));
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
    @Transactional
    public void deleteBatch(List<Long> ids) {
        log.info("根据id批量删除菜品:{}",ids);

        Set<Long> categoryIds=new HashSet<>();
        //判断当前菜品是否可以删除--是否在起售中
        for(Long id:ids){
            Dish dish=dishMapper.getById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            categoryIds.add(dish.getCategoryId());
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

        //只删除受影响的分类缓存，不再无脑清空全部
        categoryIds.forEach(cid->cacheClient.delete(dishCacheKey(cid)));
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
     * @param dishDTO 包含菜品和口味信息的DTO对象
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        //改之前先查旧的分类信息，用于后续缓存处理
        Dish old=dishMapper.getById(dishDTO.getId());

    // 创建新的Dish对象，将DTO中的属性复制到Dish对象中
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //修改菜品表基本信息
        dishMapper.update(dish);
        //删除菜品口味表信息，为重新插入做准备
        dishFlavorMapper.deleteByDishId(dish.getId());
        //新增菜品口味表信息
        List<DishFlavor> flavors=dishDTO.getFlavors();
    // 检查flavors列表不为空
        if ((flavors != null) && (!flavors.isEmpty())) {
        // 为每个口味设置对应的菜品ID
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
        // 批量插入口味信息
            dishFlavorMapper.insertBatch(flavors);
        }
        //删除旧的分类缓存，如果改了分类，新分类缓存也删掉
        cacheClient.delete(dishCacheKey(old.getCategoryId()));
        if(dishDTO.getCategoryId()!=null&&!dishDTO.getCategoryId().equals(old.getCategoryId())){
            cacheClient.delete(dishCacheKey(dishDTO.getCategoryId()));
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
     * @param dish 查询条件对象，包含菜品分类ID等信息
     * @return 返回包含菜品和口味信息的VO列表
     */
    public List<DishVO> listWithFlavor(Dish dish) {
    // 获取菜品分类ID
        Long categoryId=dish.getCategoryId();
    // 构建缓存键，使用分类ID作为键的一部分
        String cacheKey=dishCacheKey(categoryId);

        //先查询缓存，尝试从缓存中获取数据
        List<DishVO> cached=cacheClient.getJson(cacheKey,new TypeReference<List<DishVO>>() {});
    // 如果缓存中存在数据，直接返回
        if (cached!=null){
            return cached;
        }
        //缓存中没有，回源DB
        if(!cacheClient.tryLock("dish:"+categoryId,10)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cached = cacheClient.getJson(cacheKey, new TypeReference<List<DishVO>>() {
            });
            if (cached != null) {
                return cached;
            }
            //等了一次还是没有，重建线程可能刚失败，因此直接查库，不缓存防止脏数据
            return queryAndCache(categoryId, cacheKey);
        }
        try{
            //双重验证：拿到锁后发现别的线程已经重建完成了
            cached=cacheClient.getJson(cacheKey,new TypeReference<List<DishVO>>() {});
            if (cached!=null){
                return cached;
            }
            return queryAndCache(categoryId,cacheKey);
        }finally {
            cacheClient.unlock("dish:"+categoryId);
        }
    }

    /**
     * 菜品起售停售详情
     * @param status 菜品状态（起售/停售）
     * @param id 菜品ID
     */

    public void startOrStop(Integer status, Long id) {
        // 构建Dish对象，设置菜品ID和状态
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        // 更新菜品信息到数据库
        dishMapper.update(dish);

        // 从数据库获取更新后的菜品信息
        Dish db=dishMapper.getById(id);
        // 清除对应分类的菜品缓存
        cacheClient.delete(dishCacheKey(db.getCategoryId()));
    }

    /** 缓存key统一管理：避免散落各处写错*/
    private String dishCacheKey(Long categoryId){
        return "dish:category:" + categoryId;
    }
    /**回源DB 并填回缓存：空值缓存防止穿透，随机TTL防止雪崩*/
    private List<DishVO> queryAndCache(Long categoryId,String cacheKey) {
        Dish dish=Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        List<Dish> dishList=dishMapper.list(dish);

        List<DishVO> dishVOList=new ArrayList<>();
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);
            dishVO.setFlavors(dishFlavorMapper.getByDishId(d.getId()));
            dishVOList.add(dishVO);
            }
        if(dishVOList.isEmpty()){
            //空值缓存:缓存空数组+短TTL，防穿透且可命中
            cacheClient.setJson(cacheKey,dishVOList,60,30);
        }else{
            //随机TTL防止雪崩
            cacheClient.setJson(cacheKey,dishVOList,300,60);
        }
        return dishVOList;
    }

}