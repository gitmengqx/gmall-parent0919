package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author mqx
 * http://api.gmall.com/admin/product/fileUpload
 * @date 2020/3/14 16:20
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {

    // fileUrl=http://192.168.200.128:8080/
    @Value("${fileServer.url}")
    private String fileUrl;

    // img10.360buyimg.com/n7/jfs/t1/106477/26/14262/166916/5e6372d5E2df611b4/1b476a83d5c54c69.jpg
    // 此方法返回应该是路径
    // 利用springmvc 文件上传的知识点
    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception{
        String path = null;
        // 如果上传对象不为空
        if (file!=null){
            // 获取resource 目录下的tracker.conf
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            // 初始化配置
            ClientGlobal.init(configFile);
            // 创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            // 创建一个trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();
            // 创建一个StorageClient1对象
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            // 上传获取到path
            // 第一个参数：文件的字节数组
            // 第二个参数：文件的后缀名
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            System.out.println("文件路径:"+fileUrl+path);

        }
        // 返回的是一个路径 服务的ip 地址：
        return Result.ok(fileUrl+path);
    }

}
