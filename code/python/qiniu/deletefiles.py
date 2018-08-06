# -*- coding: utf-8 -*-
#!/usr/bin/env python

import sys
sys.path.append("../")
import time
import qiniu
from qiniu import Auth
from qiniu import BucketManager
from qiniu import config

# init qiniu info
zone = qiniu.zone.Zone(up_host='up-qos.xmtest.com',
                       up_host_backup='up-qos.xmtest.com',
                       io_host='io-qos.xmtest.com',
                       scheme='http')
config.set_default(zone,
                   default_rs_host='http://rs-qos.xmtest.com',
                   default_rsf_host='http://rsf-qos.xmtest.com',
                   default_api_host='http://api-qos.xmtest.com')

# build auth
access_key = '-ZHxqatY89W9OOjoAOwdS1WtK2xgg54cQK8pgnsh'
secret_key = 'eaDpBUAFW-3jMLggGm31WgTxZAWoDBkOVjRwbjBb'
q = Auth(access_key, secret_key)

# init BucketManager
bucket = BucketManager(q)

# bucket_name
bucket_name = 'record'

#
prefix = None

#
limit = 10

#
delimiter = None

marker = None

eof = False

file_count = 0
start_time = time.time()
while not eof:
    try:
        ret, eof, info = bucket.list(bucket_name, prefix, marker, limit, delimiter)
        for item in ret.get('items'):
            key = item['key']
            file_count += 1
            ret, info = bucket.delete(bucket_name, key)
        if eof:
            marker = ret.get('marker')
    except Exception as e:
        print(e)
        print(info)
        print(ret)
else:
    elapsed_time_s = time.time() - start_time
    print("The delete %d file costs %6f s." % (file_count, elapsed_time_s))
