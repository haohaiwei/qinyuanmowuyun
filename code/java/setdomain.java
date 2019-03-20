import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;


public class setDomain {

    public static void main(String[] args)  {


        //指定密钥
        String ak = "< your AK >";
        String sk = "< your SK >";
        String bucket = "< your bucket name >";
        String domain = "< your domain >";
        //创建认证
        Auth auth = Auth.create(ak, sk);
        String url = "http://< rs domain >/publish/" + UrlSafeBase64.encodeToString(domain) + "/from/" + bucket +"/domaintype/1";
        System.out.println(url);
        StringMap headers = auth.authorization(url, null, "application/x-www-form-urlencoded");
        System.out.println(headers);
        Client client = new Client();
        com.qiniu.http.Response response = null;
        try {
            response = client.get(url, headers);
            System.out.println(response.statusCode);
            System.out.println(response.bodyString());
        }catch (QiniuException e) {
            e.printStackTrace();
        }finally {
            if (response != null) response.close();

        }
    }
}
