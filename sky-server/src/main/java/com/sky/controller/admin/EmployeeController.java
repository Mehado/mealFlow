package com.sky.controller.admin;

import com.sky.annotations.SelfPermission;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "员工管理", description = "员工登录、退出等管理接口")
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    @Operation(summary = "员工登录")
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    @Operation(summary = "员工退出")
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

//新增员工
    @PostMapping
    @Operation(summary = "新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工：{}",employeeDTO);
         employeeService.save(employeeDTO);
         return Result.success();
    }
    @GetMapping("/page")
    @Operation(summary = "员工分页查询")
    // 员工分页查询
    public Result<PageResult> page(@ParameterObject EmployeePageQueryDTO employeePageQueryDTO){
        log.info("员工分页查询：{}",employeePageQueryDTO);
       PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
       return Result.success(pageResult);
    }

    // 员工状态禁用/启用
    @PostMapping("/status/{status}")
    @Operation(summary = "员工状态禁用/启用")
    @SelfPermission(targetId = "#id", type = SelfPermission.CheckType.NOT_SELF)
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("禁用/启用员工账号：{},{}", status, id);
        employeeService.startOrStop(status, id);
        return Result.success();
    }

    /**
     *  员工查询
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "员工查询")
    public Result<Employee> getById(@PathVariable Long id){
        log.info("员工查询：{}",id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }


    @SelfPermission(targetId = "#employeeDTO.id", type = SelfPermission.CheckType.SELF)
    @PutMapping
    @Operation(summary = "员工信息修改")
    public Result update(@RequestBody EmployeeDTO employeeDTO){
        log.info("员工信息修改：{}",employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

    @SelfPermission(targetId = "#passwordEditDTO.empId", type = SelfPermission.CheckType.SELF)
    @PutMapping("/editPassword")
    @Operation(summary = "修改密码")
    public Result<Void> updatePassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        log.info("修改密码：{}", passwordEditDTO);
        employeeService.updatePassword(passwordEditDTO);
        return Result.success();
    }
}
