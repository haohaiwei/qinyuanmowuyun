/*
demo:    Java SDK
date:    2018-07-24 18:30
Author:  H
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


public class upload {

    public static void main(String[] args) throws Exception {
/*上传*/
        //指定密钥                                                                                                          
        String ak = "Gcs-qqU0d7FUN773hRtNEER76PFtz5_PUZ-FyO0z";
        String sk = "Zo2O-VBq6LKBDYfgTMeOuuBBBcM3GMLjdC47mamo";
        String bucket = "caoweisen";
        //创建认证
        Auth auth = Auth.create(ak, sk);
        //初始化zone
        Zone zone = new Zone.Builder()
                .upHttp("http://up-qos.zstack-poc.com")
                .upBackupHttp("http://up-qos.zstack-poc.com")
                .rsHttp("http://rs-qos.zstack-poc.com")
                .rsfHttp("http://rsf-qos.zstack-poc.com")
                .apiHttp("http://apiserver-qos.zstack-poc.com")
                .iovipHttps("http://io-qos.zstack-poc.com").build();
        Configuration cfg = new Configuration(zone);
        //初始化upload
        UploadManager uploadManager = new UploadManager(cfg);
        String localFilePath = "/root/qiniu-test2222.png";
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



