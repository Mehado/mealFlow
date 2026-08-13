package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotations.AutoFill;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 插入员工数据
     **/
    @Insert("insert into employee (username, name, password, phone, sex, id_number, status, " +
            "create_time, update_time, create_user, update_user,role) values (#{username}, " +
            "#{name}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}," +
            " #{createTime}, #{updateTime}, #{createUser}, #{updateUser}, #{role})")
    @AutoFill(value= OperationType.INSERT)
    void insert(Employee employee);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用和禁用员工账号
     * @param employee
     * @return
     */
    @AutoFill(value= OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    /**
     * 统计指定角色的员工数量
     * @param role 角色
     * @return 数量
     */
    @Select("select count(*) from employee where role = #{role}")
    Long countByRole(String role);

    /**
     * 修改密码
     * @param id
     * @param encodedPassword
     */
    @Update("update employee set password = #{encodedPassword} where id = #{id}")
    void updatePassword(Long id, String encodedPassword);
}
