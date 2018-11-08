/*
 * date:    2018-11-07 21:30
 * demo:    Java Qiniu-S3-STS SDK
 * Author:  Hao
 */

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class sts {

    public static void main(String[] args) throws IOException {
        String clientRegion = "<Region>";
        String bucketName = "<BucketName>";
        String federatedUser = "<UserName>";
        String s3endpoint = "<s3EndPoint>";
        String accessKey = "<AK>";
        String secretKey = "<SK>";
        String targetFilePath = "<DownLoadPATH/filename";
        try {
            /*
             * Get sts token
             */
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3endpoint, clientRegion))
                    .build();

            GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest();
            getFederationTokenRequest.setDurationSeconds(1800);
            getFederationTokenRequest.setName(federatedUser);

            /*
             * Define the policy and add it to the request.
             */
            Policy policy = new Policy("{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\",\"Resource\":\"arn:aws:s3:::test1\"}]}");
            getFederationTokenRequest.setPolicy(policy.toJson());

            /*
             * Get the temporary security credentials.
             */
            GetFederationTokenResult federationTokenResult = stsClient.getFederationToken(getFederationTokenRequest);
            Credentials sessionCredentials = federationTokenResult.getCredentials();
            System.out.println(sessionCredentials);
            /*
             * Package the session credentials as a BasicSessionCredentials
             * object for an Amazon S3 client object to use
             */
            BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
                    sessionCredentials.getAccessKeyId(),
                    sessionCredentials.getSecretAccessKey(),
                    sessionCredentials.getSessionToken());


            S3ClientOptions.Builder options = S3ClientOptions.builder().setPathStyleAccess(true);
            AmazonS3 s3 = new AmazonS3Client(basicSessionCredentials);
            s3.setEndpoint(s3endpoint);
            s3.setS3ClientOptions(options.build());
            /*
             * To verify that the client works, send a listObjects request using
             * the temporary security credentials.
             */

            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" - " + bucket.getName());
            }
            System.out.println();
            /*
             * send a putObject request using the temporary security credentials.
             */
            String s3object = UUID.randomUUID().toString();
            PutObjectResult putResult = s3.putObject(new PutObjectRequest(bucketName, s3object, new File("/Users/haohaiwei/s3.txt")));
            System.out.println("Put object " + s3object + " Etag: " + putResult.getETag());
            /*
             * send a getObject request using the temporary security credentials.
             */
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, s3object));
            if(object!=null) {
                System.out.println("Content-Type: " + object.getObjectMetadata().getContentType());
                InputStream input = null;
                FileOutputStream fileOutputStream = null;
                byte[] data = null;
                try {
                    //获取文件流
                    input = object.getObjectContent();
                    data = new byte[input.available()];
                    int len = 0;
                    fileOutputStream = new FileOutputStream(targetFilePath);
                    while ((len = input.read(data)) != -1) {
                        fileOutputStream.write(data, 0, len);
                    }
                    System.out.println("下载文件成功");

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        catch(AmazonServiceException e) {
            /*
             * The call was transmitted successfully, but Amazon S3 couldn't process
             * it, so it returned an error response.
             */
            e.printStackTrace();
        }
        catch(SdkClientException e) {
            /*
             * Amazon S3 couldn't be contacted for a response, or the client
             * couldn't parse the response from Amazon S3.
             */
            e.printStackTrace();
        }
    }
}
