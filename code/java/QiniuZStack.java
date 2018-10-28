import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;


public class QiniuZStack {

    public static void main(String[] args){

        System.out.println("---------------begin-----------------");
        String accessKey = "bvNLPxASNgQCBjCiijrEXuNVTpL-Y6T2AeyRZc7N";
        String secretKey = "Zkwx4_Zzx9IKQLYQysBbdR7TvZaZnPRYjM3du8-J";
        Auth auth = Auth.create(accessKey, secretKey);
        // 初始化代码开始
        Zone zone = new Zone.Builder()
                .upHttp("http://up-qos.qiniutest.com")
                .upBackupHttp("http://up-qos.qiniutest.com")
                .rsHttp("http://rs-qos.qiniutest.com")
                .rsfHttp("http://rsf-qos.zstack-poc.com")
                .apiHttp("http://api-qos.zstack-poc.com")
                .iovipHttps("http://io-qos.zstack-poc.com").build();
        Configuration cfg = new Configuration(zone);



        //初始化上传接口函数，并用之前 cfg 配置环境
        UploadManager uploadManager = new UploadManager(cfg);
        OperationManager operationManager = new OperationManager(auth, cfg);
        // 上传业务代码
        String bucket = "test123";
        // 传文件
        String localfilepath= "/root/test.png";
        String key="testhttp";
        String upToken = auth.uploadToken(bucket);
        try{

            Response response = uploadManager.put(localfilepath,key,upToken);
            DefaultPutRet defaultPutRet = new Gson().fromJson(response.bodyString(),DefaultPutRet.class);
            System.out.println(defaultPutRet.key);
            System.out.println(defaultPutRet.hash);
        }catch (QiniuException ex){
            Response response=ex.response;
            System.out.println(response.toString());try{
                System.out.println(response.bodyString());
            }catch (QiniuException ex2){
                //ignore
            }
        }



        
        System.out.println("---------------success-----------------");
        System.out.println("---------------success-----------------");
        System.out.println("---------------success-----------------");
    }




}

