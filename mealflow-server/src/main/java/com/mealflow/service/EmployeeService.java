package com.mealflow.service;

import com.mealflow.dto.EmployeeDTO;
import com.mealflow.dto.EmployeeLoginDTO;
import com.mealflow.dto.EmployeePageQueryDTO;
import com.mealflow.dto.PasswordEditDTO;
import com.mealflow.entity.Employee;
import com.mealflow.result.PageResult;
import com.mealflow.result.Result;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    //启用禁用员工账号
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    Employee getById(Long id);

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    void update(EmployeeDTO employeeDTO);

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    void updatePassword(PasswordEditDTO passwordEditDTO);
}
