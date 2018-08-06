# -*- coding: utf-8 -*-
#!/usr/bin/env python


# flake8: noqa

#  test
'''功能性测试'''
import requests
from qiniu import Auth, put_file, etag, BucketManager, config, build_batch_delete
import qiniu

default_rs_host = 'http://rs-qos.tc.echosoul.cn'
default_rsf_host = 'http://rsf-qos.tc.echosoul.cn'
default_api_host = 'http://api-qos.tc.echosoul.cn'

zone = qiniu.zone.Zone(up_host = 'up-qos.tc.echosoul.cn',
                       up_host_backup = 'up-qos.tc.echosoul.cn',
                       io_host = 'io-qos.tc.echosoul.cn',
                       scheme='http')
config.set_default(zone,
                   default_rs_host=default_rs_host,
                   default_rsf_host=default_rsf_host,
                   default_api_host=default_api_host)
bucket_name = 'bucket'

ak = '1rIEvKmJqA3CvSnawvcek0rpFfSKi3EJNj1ozZ0r'
sk = 'NQLVqPhdzqv42cbkp7u-Mj0s8j-UXaPixbiDQFJ8'
# bucket1 = 'bucket'
# bucket1 = 'helloworld'
# bucket1_domain = 'http://test.tc.echosoul.cn'
# bucket2 = 'bucket2'
# bucket2_domain = 'http://test2.tc.echosoul.cn'
bucketp = 'bucket'
bucketp_domain = 'tes01.tc.echosoul.cn'
q = Auth(ak, sk)


def test():
    # 小文件上传测试
    # 上传到七牛后保存的文件名
    key1 = 'smallfile'
    # 生成上传 Token，可以指定过期时间等
    token1= q.upload_token(bucket_name, key1, 3600)
    # 要上传文件的本地路径
    smallfile = './testfile/smallfile'
    bigfile = './testfile/bigfile'
    # 上传
    ret, info = put_file(token1, key1, smallfile)
    # 检验是否成功
    if info.status_code == 200:
        print "small file upload is ok"
    else:
         print info.status_code
    assert ret['key'] == key1
    assert ret['hash'] == etag(smallfile)
    # 大文件上传测试
    key2='bigfile'
    token2=q.upload_token(bucket_name,key2,3600)
    ret,info=put_file(token2,key2,bigfile)
    if info.status_code == 200:
        print "big file upload is ok"
    else:
        print info.status_code
    assert ret['key'] == key2
    assert ret['hash'] == etag(bigfile)
    # List测试
    bucket = BucketManager(q)
    prefix = None
    limit = 10
    delimiter = None
    marker = None

    ret, eof, info = bucket.list(bucket_name, prefix, marker, limit, delimiter)
    if info.status_code == 200:
         print "list is ok"
    else:
        print info.status_code
    assert len(ret.get('items')) is not None
    # 下载测试
    filename ='dl'
    key3 = 'bigfile'
    base_url = 'http://%s/%s' % (bucketp_domain, key3)
    private_url = q.private_download_url(base_url, expires=3600)
    r = requests.get(private_url)
    if r.status_code == 200:
        print "download is ok"
    else:
        print r.status_code
    # mv 测试
    key5 = 'bigfile'
    key6 = 'bigfile2'
    ret, info = bucket.move(bucket_name, key5, bucket_name, key6)
    if info.status_code == 200:
        print "mv is ok"
    else:
        print info
    # copy 测试
    bucket = BucketManager(q)
    key04 = 'smallfile01'
    ret, info = bucket.copy(bucket_name,key1,bucket_name,key04)
    if info.status_code == 200:
        print "copy is ok"
    else:
        print info

    # delete测试
    key = 'smallfile'
    ret, info  = bucket.delete(bucket_name, key)
    if info.status_code == 200:
        print "delete is ok"
    else:
        print info

    # 删除测试文件
    bucket = BucketManager(q)
    keys = ['bigfile2', 'smallfile01']
    ops = build_batch_delete(bucket_name, keys)
    ret, info = bucket.batch(ops)
    if info.status_code == 200:
        print "all test file is deleted"
    else:
        print info


test()






