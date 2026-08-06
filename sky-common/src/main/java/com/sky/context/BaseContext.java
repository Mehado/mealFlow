package com.sky.context;

/**
 * BaseContext类用于管理线程局部变量(ThreadLocal)，主要用于存储当前线程的用户ID和角色信息
 * 通过ThreadLocal实现线程间的数据隔离，确保每个线程访问的是自己独立的变量副本
 */
public class BaseContext {

    // 定义一个ThreadLocal<Long>类型的静态变量，用于存储当前线程的用户ID
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     * @param id 用户ID，Long类型
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前线程的用户ID
     * @return 当前线程的用户ID，如果未设置则返回null
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 移除当前线程的用户ID
     * 通常在请求处理完成后调用，防止内存泄漏
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }

    // 定义一个ThreadLocal<String>类型的静态变量，用于存储当前线程的角色信息
    public static ThreadLocal<String> RoleThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的角色信息
     * @param role 角色信息，String类型
     */
    public static void setRole (String role) {
        RoleThreadLocal.set(role);
    }

    /**
     * 获取当前线程的角色信息
     * @return 当前线程的角色信息，如果未设置则返回null
     */
    public static String getRole() {
        return RoleThreadLocal.get();
    }
    /**
     * 移除当前线程的角色信息
     * 通常在请求处理完成后调用，防止内存泄漏
     */
    public static void removeRole() {
        RoleThreadLocal.remove();
    }

}
