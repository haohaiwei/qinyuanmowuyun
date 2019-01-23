#!/usr/bin/env python
# -*- coding: utf-8 -*-
import time
from multiprocessing.dummy import Pool as ThreadTool
from ftplib import FTP
import os

ftp = FTP()
path = '/jfs'
ftp.connect("xx.xx.xx.xx", "21")
ftp.login("xxxx", "xxxx")
list_name = []
for file in os.listdir(path):
    file_path = os.path.join(path, file)
    if os.path.isfile(file_path):
        list_name.append(file_path)


def ftp_upload(filename):
    bufsize = 1024
    #    ftp.connect("10.32.254.15", "21" )
    #    ftp.login("root", "testftp199202")
    fp = open(filename, 'rb')
    ftp.storbinary('STOR ' + filename, fp, bufsize)


now = time.time()
pool = ThreadTool(1)
result = pool.map(ftp_upload, list_name)
pool.close()
pool.join()
print "1 并发耗时 %s" % (time.time() - now)
