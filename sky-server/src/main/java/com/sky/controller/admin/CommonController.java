package com.sky.controller.admin;


import com.sky.annotations.RequireRole;
import com.sky.constant.MessageConstant;
import com.sky.constant.RoleConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("admin/common")
@Tag(name="通用接口",description = "通用接口")
@Slf4j
public class CommonController {
    //允许上传的图片扩展名白名单
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif","bmp","webp");

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传接口

 * 该接口用于处理客户端上传的图片文件，并进行合法性校验后上传至阿里云OSS
     * @param file 客户端上传的MultipartFile文件对象
     * @return 返回Result类型的结果，包含上传成功后的文件访问路径或错误信息
     */
    @RequireRole  // 需要特定角色权限才能访问的注解
    @PostMapping("/upload")  // HTTP POST请求映射，指定请求路径为"/upload"
    @Operation(summary = "文件上传")  // Swagger接口文档注解，描述接口功能
    public Result<String> uploadFile(MultipartFile file){  // 方法定义，接收MultipartFile参数，返回Result<String>
        log.info("文件上传:{}",file);  // 记录文件上传的日志信息

        try {
        // 校验文件是否为空
            if(file==null||file.isEmpty()){
                return Result.error("文件不能为空");  // 文件为空时返回错误信息
            }

        // 获取原始文件名并进行校验
            String originalFilename = file.getOriginalFilename();
            if(originalFilename==null){
                return Result.error("文件名不能为空");  // 文件名为空时返回错误信息
            }
        // 获取文件扩展名并进行校验
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")+1).toLowerCase();
            if(!ALLOWED_EXTENSIONS.contains(extension)){  // 检查文件扩展名是否在允许的列表中
                return Result.error("不支持的文件类型"+extension);  // 不支持的文件类型时返回错误信息
            }
        // 校验文件内容类型是否为图片
            String contentType=file.getContentType();
            if(contentType==null||!contentType.startsWith("image/")){  // 检查是否为图片类型
                return Result.error("只支持图片文件");  // 非图片文件时返回错误信息
            }



        // 生成唯一文件名并上传至OSS
            String objectName = UUID.randomUUID().toString() +"."+ extension;  // 使用UUID生成唯一文件名
            String filePath =aliOssUtil.upload(file.getBytes(),objectName);  // 调用阿里云OSS工具类上传文件
            return Result.success(filePath);  // 上传成功返回文件访问路径
        } catch (IOException e) {
            log.error("文件上传失败:{}",e);  // 记录文件上传失败的异常信息
        }


    // 上传失败返回默认错误信息
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
