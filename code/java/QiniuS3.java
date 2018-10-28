/*
 * date:    2018-10-25 18:30
 * demo:    Java Qiniu-S3 SDK
 * Author:  Hao
 */




import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.io.IOException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.S3ClientOptions.Builder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;


public class QiniuS3 {
    public static void main(String[] args) {

        String s3endpoint = "http://s3-qos.tc2.echosoul.cn";
        String s3bucket = "<Bucket Name>";
        String accessKeyId = "<AK>";
        String accessKeySecret = "<SK>";
        String targetFilePath = "<DownLoadFilePath>";
        // credentials
        AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, accessKeySecret);

        Builder options = S3ClientOptions.builder();
        options.setPathStyleAccess(true);
        options.setAccelerateModeEnabled(false);
        options.setPayloadSigningEnabled(true);


        AmazonS3 s3 = new AmazonS3Client(credentials);
        s3.setEndpoint(s3endpoint);
        s3.setS3ClientOptions(options.build());

        try {

            /*
             * List the buckets in your account
             */
            System.out.println("Listing buckets ...");

            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" - " + bucket.getName());
            }
            System.out.println();

            /*
             * Upload an object to your bucket
             */
            System.out.println("Uploading a new object to S3 from local file ...");

            String s3object = UUID.randomUUID().toString();
            PutObjectResult putResult = s3
                    .putObject(new PutObjectRequest(s3bucket, s3object, new File("<UpLoadFilePath>")));
            System.out.println("Put object " + s3object + " Etag: " + putResult.getETag());
            System.out.println();


            /*
             * Download an object from your bucket
             */
            System.out.println("Downloading a new object from your bucket");
            S3Object object = s3.getObject(new GetObjectRequest(s3bucket,"0364e689-811f-4ef9-9562-b74aae20d26e"));
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

            /*
             * Multipart upload
             */
            System.out.println("Multipart uploading ...");

            TransferManagerBuilder tmb = TransferManagerBuilder.standard();
            tmb.setMinimumUploadPartSize((long) (4 << 20)); // force part size to 4M or 4M * N
            tmb.setS3Client(s3);
            tmb.setMultipartUploadThreshold((long) 10);

            File file = new File("/Users/haohaiwei/DSC01.jpg");

            TransferManager tm = tmb.build();
            Upload upload = tm.upload(s3bucket, file.getName(), file);
            UploadResult tmResult = upload.waitForUploadResult();
            System.out.println(tmResult.toString());

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());

        } catch (Throwable ie) {
            System.out.println(ie.getMessage());

        }

    }

}