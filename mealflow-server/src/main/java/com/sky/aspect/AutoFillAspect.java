package com.sky.aspect;

import com.sky.annotations.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {

/**
 * 定义切入点，用于匹配带有@AutoFill注解的com.sky.mapper包下的所有方法
 * execution(* com.sky.mapper..*(..)) 表示匹配com.sky.mapper包及其子包下的所有方法
 * && @annotation(com.sky.annotations.AutoFill) 表示同时带有@AutoFill注解的方法
 */
    @Pointcut("execution(* com.sky.mapper..*(..)) && @annotation(com.sky.annotations.AutoFill)") // 定义切入点表达式
    public void autoFillPointCut(){ // 切入点方法，方法名为autoFillPointCut
    }
/*******************    💫 Codegeex Suggestion    *******************/
/**
 * 在执行@Before注解标记的方法前执行，用于自动填充公共字段
 * @param joinPoint 连接点，可以获取方法执行时的相关信息
 * @t 在执行@Before注解标记的方法前执行
 * @r 用于自动填充公共字段
 * @n 获取数据库操作类型及实体对象，准备赋值的数据，根据当前不同的操作类型，为对应的属性通过反射赋值
 */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) { // 定义切面方法，方法名为autoFill
        //记录日志，表示开始自动填充公共字段数据
        log.info("开始向公共字段自动填充数据");
        //获取数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();
        // 获取方法参数-实体对象
        Object[] args = joinPoint.getArgs();
        //空指针判断
        if(args==null||args.length==0){
            return ;
        }
         Object entity = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId= BaseContext.getCurrentId();
        //根据当前不同的操作类型，为对应的属性通过反射赋值
        if(OperationType.INSERT.equals(operationType)){
            try {
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME,LocalDateTime.class).invoke(entity,now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class).invoke(entity,now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER,Long.class).invoke(entity,currentId);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class).invoke(entity,currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if(OperationType.UPDATE.equals(operationType)){
            try{
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME,LocalDateTime.class).invoke(entity,now);
                entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER,Long.class).invoke(entity,currentId);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
