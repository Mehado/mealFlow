package com.sky.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.List;

import com.sky.constant.RoleConstant;
import com.sky.context.BaseContext;
import com.sky.dto.PasswordEditDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String LOGIN_FAIL_COUNT_KEY="login:fail:count:";
    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_MINUTES = 15;

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //防止爆破，先查该用户是否已经被锁定
        String failCount= stringRedisTemplate.opsForValue().get(LOGIN_FAIL_COUNT_KEY+username);
        if(failCount!=null && Integer.parseInt(failCount)>=MAX_FAIL_COUNT) {
            throw new AccountLockedException("密码错误次数过多，账号已经被锁定，请"+LOCK_MINUTES+"分钟后再试");
        }


        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对前端传过来的密码进行BCrypt加密之后再进行比对(自动提取数据库中密文的盐值进行加密)
        //password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordEncoder.matches(password,employee.getPassword())) {
            //密码错误：失败次数加1，并刷新锁定时间
            Long count = stringRedisTemplate.opsForValue().increment(LOGIN_FAIL_COUNT_KEY+username);
            stringRedisTemplate.expire(LOGIN_FAIL_COUNT_KEY+username, LOCK_MINUTES, TimeUnit.MINUTES);
            if(count>=MAX_FAIL_COUNT) {
                //达到上限，锁定账号
                throw new AccountLockedException("密码错误次数过多，账号已经被锁定，请"+LOCK_MINUTES+"分钟后再试");
            }
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        //登陆成功，清楚失败计数
        stringRedisTemplate.delete(LOGIN_FAIL_COUNT_KEY+username);

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    //新增员工
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setStatus(StatusConstant.ENABLE);

        //新增员工没指定职业就给STAFF，即最小权限
        if(employee.getRole() == null||employee.getRole().trim().isEmpty()) {
            employee.setRole(RoleConstant.STAFF);
        }

        employee.setPassword(passwordEncoder.encode(PasswordConstant.DEFAULT_PASSWORD));  // 使用BCrypt加密默认密码

        employeeMapper.insert(employee);
    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        long total=page.getTotal();
        List<Employee> records=page.getResult();

        return new PageResult(total,records);
    }

    //启用禁用员工账号
    @Override
    public void startOrStop(Integer status, Long id) {
        //update employee set status = ? where id = ?
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);
    }

    //根据id查询员工：
    @Override
    public Employee getById(Long id) {
        Employee employee =employeeMapper.getById(id);
        employee.setPassword("****");
        return employee;
    }

    //编辑员工信息
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

    /**
     * 修改密码
     * @param passwordEditDTO
     */
    public void updatePassword(PasswordEditDTO passwordEditDTO) {
        Long id= BaseContext.getCurrentId();

        String  oldPassword = passwordEditDTO.getOldPassword();
        String  newPassword = passwordEditDTO.getNewPassword();

        Employee employee = employeeMapper.getById(id);
        String originPassword = employee.getPassword();
        if (!passwordEncoder.matches(oldPassword,originPassword)) { // 密码比对失败
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
        //employee.setPassword(passwordEncoder.encode(newPassword));
        String encodedPassword = passwordEncoder.encode(newPassword);
        employeeMapper.updatePassword(id, encodedPassword);
    }

}
