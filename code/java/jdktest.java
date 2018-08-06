/*
demo:    Java SDK
date:    2018-07-24 18:30
Author:  Hao
  */


import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.common.Zone;
import com.google.gson.Gson;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.http.Response;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.BatchStatus;


public class jdktest {

    public static void main(String[] args) throws Exception {
        //指定密钥                                                                                                          
        String ak = "6lafXhdrd3BYG7FnYz1mv8ROlJE0TuhLSgr3MBGV";
        String sk = "wpc2xH96dGimOYw0bnwRYEdDNZHkWikOtVlnThmR";
        String bucket = "test01";
        //创建认证
        Auth auth = Auth.create(ak, sk);
        //初始化zone
        Zone zone = new Zone.Builder()
                .upHttp("http://up-qos.xmtest.com")
                .upBackupHttp("http://up-qos.xmtest.com")
                .rsHttp("http://rs-qos.xmtest.com")
                .rsfHttp("http://rsf-qos.xmtest.com")
                .apiHttp("http://api-qos.xmtest.com")
                .iovipHttp("http://io-qos.xmtest.com").build();
        Configuration cfg = new Configuration(zone);
/*上传*/       
        //初始化upload
        UploadManager uploadManager = new UploadManager(cfg);
        String localFilePath = "/Users/haohaiwei/hao.py";
        //指定在bucket中的别名，null的时候会随机hash一个
        String key = "test001";
        String upToken = auth.uploadToken(bucket);
        try {
            Response response = uploadManager.put(localFilePath, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key);
            System.out.println(putRet.hash);
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                /* ignore */
            }


        }
/* 获取文件信息 */
        BucketManager bucketManager = new BucketManager(auth, cfg);
        try {
            FileInfo fileInfo = bucketManager.stat(bucket, "1.pdf");
            System.out.println(fileInfo.hash);
            System.out.println(fileInfo.fsize);
            System.out.println(fileInfo.mimeType);
            System.out.println(fileInfo.putTime);
        } catch (QiniuException ex) {
            System.err.println(ex.response.toString());
        }
/*list操作*/
        String prefix = "";
        int limit = 1000;
        String delimiter = "";
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucket, prefix, limit, delimiter);
        while (fileListIterator.hasNext()) {
            //处理获取的file list结果
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                System.out.println(item.key);
                System.out.println(item.hash);
                System.out.println(item.fsize);
                System.out.println(item.mimeType);
                System.out.println(item.putTime);
                System.out.println(item.endUser);
            }
        }
/*move操作*/
        String fromBucket = "test01";
        String fromKey = "test001";
        String toBucket = "test01";
        String toKey = "test003";
        try {
            bucketManager.move(fromBucket, fromKey, toBucket, toKey);
        } catch (QiniuException ex) {
            //如果遇到异常，说明移动失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }
/*cp操作*/
        String bucket01 = "test01";
        String bucket02 = "test01";
        String key01 = "test003";
        String key02 = "test004";
        try {
            bucketManager.copy(bucket01, key01, bucket02,key02 );
        } catch (QiniuException ex) {
            //如果遇到异常，说明复制失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }
/*del操作*/
        try {
            bucketManager.delete(bucket, key);
        } catch (QiniuException ex) {
            //如果遇到异常，说明删除失败
            System.err.println(ex.code());
            System.err.println(ex.response.toString());
        }
        }

    }



