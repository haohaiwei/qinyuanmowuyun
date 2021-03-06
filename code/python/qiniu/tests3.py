#!/usr/bin/env python
# -*- coding: utf-8 -*-

import boto3
import botocore

# some configs
s3endpoint = '< endpoint >'
s3region = 'cn-east-1'
s3bucket = '< your bucketname >'
s3object = '< your objectname >'
s3accessKeyId = '< your ak >'
s3accessKeySecret = '< your sk >'


# use resource
s3resource = boto3.resource('s3',
                            endpoint_url=s3endpoint,
                            region_name=s3region,
                            aws_access_key_id=s3accessKeyId,
                            aws_secret_access_key=s3accessKeySecret)

## list buckets
for bucket in s3resource.buckets.all():
    print(bucket)

## upload file
#data = open('/root/testfile', 'rb')
s3response = s3resource.meta.client.upload_file('/root/testfile',s3bucket,s3object)
print(s3response)

### download file
#s3response = s3resource.Bucket(s3bucket).download_file(Key=s3object, Filename="/root/s3test")
#print(s3response)
try:
    s3resource.Bucket(s3bucket).download_file(s3object, 's3test')
except botocore.exceptions.ClientError as e:
    if e.response['Error']['Code'] == "404":
        print("The object does not exist.")
    else:
        raise

multipart_upload = s3resource.MultipartUpload(s3bucket,'testfiles01','id')
print(multipart_upload)
