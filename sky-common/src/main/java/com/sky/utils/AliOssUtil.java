package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     * 该方法用于将字节数组形式的文件上传到阿里云OSS对象存储服务

 *
     * @param bytes     文件的字节数组
     * @param objectName 上传到OSS后的对象名称（文件名）
     * @return          返回文件在OSS中的访问URL地址
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
    // 使用OSSClientBuilder构建OSS客户端，需要传入endpoint、accessKeyId和accessKeySecret三个参数
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
        // 将字节数组转换为InputStream，然后调用putObject方法上传文件
        // 参数分别为：bucket名称、对象名称和输入流
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
        // 捕获OSSException，处理阿里云OSS服务返回的错误
        // 这种异常表示请求已到达OSS服务，但被拒绝或返回了错误响应
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
        // 捕获ClientException，处理客户端与OSS通信时遇到的严重内部问题
        // 这种异常通常表示客户端无法访问网络或其他通信问题
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
        // 在finally块中确保OSSClient被正确关闭
        // 无论上传成功还是失败，都会执行此代码块来释放资源
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
    // 构建文件在OSS中的访问URL
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)    // 添加bucket名称
                .append(".")           // 添加分隔符
                .append(endpoint)      // 添加endpoint
                .append("/")           // 添加路径分隔符
                .append(objectName);   // 添加对象名称

    // 记录日志，输出文件上传后的访问地址
        log.info("文件上传到:{}", stringBuilder.toString());

    // 返回构建好的文件访问URL
        return stringBuilder.toString();
    }
}
