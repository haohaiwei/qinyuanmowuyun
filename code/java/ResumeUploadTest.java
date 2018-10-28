import com.qiniu.processing.OperationManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.common.Zone;
import com.qiniu.storage.persistent.FileRecorder;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

import java.nio.file.Paths;
import java.io.IOException;
import com.qiniu.storage.model.DefaultPutRet;

import static java.nio.file.Paths.*;

public class ResumeUploadTest {

    public static void main(String[] args) {

        Configuration.defaultApiHost = "api-qos.poc.com";
        Configuration.defaultRsHost = "rs-qos.poc.com";
        Zone zone = new Zone.Builder()
                .upHttp("http://up-qos.poc.com")
                .upBackupHttp("http://up-qos.poc.com")
                .rsHttp("http://rs-qos.poc.com")
                .rsfHttp("http://rsf-qos.poc.com")
                .apiHttp("http://api-qos.poc.com")
                .iovipHttp("http://io-qos.poc.com").build();
        Configuration cfg = new Configuration(zone);
        String bucket = "test01";
        String ak = "yIikdIfA0BFK390lgI6BeD_9dOU6DFaz_wMS5WqW";
        String sk = "LFCyVBMxW6DBiCruZE6LwLjk8_4YVdAwaaCP6KGV";
        Auth auth = Auth.create(ak, sk);
        // 初始化代码结束
//        UploadManager uploadManager = new UploadManager(cfg);
        OperationManager operationManager = new OperationManager(auth, cfg);
        String localFilePath = "/root/testfile";
        //指定在bucket中的别名，null的时候会随机hash一个
        String key = "test001";
        String upToken = auth.uploadToken(bucket);
        String localTempDir = get(System.getenv("java.io.tmpdir"), bucket).toString();
        try {
            //设置断点续传文件进度保存目录
            FileRecorder fileRecorder = new FileRecorder(localTempDir);
            UploadManager uploadManager = new UploadManager(cfg, fileRecorder);
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
                    //ignore
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        class MyRet {
            public String hash;
            public String key;
            public String fsize;
            public String fname;
            public String mimeType;
        }
    }
}