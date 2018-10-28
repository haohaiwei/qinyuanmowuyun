# -*- encoding:utf-8 -*-

import time
from multiprocessing.dummy import Pool as ThreadTool
import alluxio
from alluxio import option, wire
import sys


def colorize(code):
    def _(text, bold=False):
        c = code
        if bold:
            c = '1;%s' % c
        return '\033[%sm%s\033[0m' % (c, text)

    return _


green = colorize('32')


def info(s):
    print green(s)


# 创建初始化目录
py_test_root_dir = '/py-test-dir'
py_test_nested_dir = '/py-test-dir/nested'
client = alluxio.Client('localhost', 39999)
opt1 = option.CreateDirectory(recursive=True, write_type=wire.WRITE_TYPE_CACHE_THROUGH)
client.create_directory(py_test_nested_dir, opt1)


def upload(file):
    # 定义上传
    opt = alluxio.option.CreateFile(write_type=wire.WRITE_TYPE_CACHE_THROUGH)
    with client.open(file, 'w', opt) as f:
        f.write('Alluxio works with Python!\n')
        with open(sys.argv[1]) as this_file:
            f.write(this_file)


def delete(path):
    # 定义删除
    info("deleting %s" % path)
    opt2 = option.Delete(recursive=True)
    client.delete(path, opt2)
    info("done")


file_list = ['/py-test-dir/nested/file1', '/py-test-dir/nested/file2', '/py-test-dir/nested/file3',
             '/py-test-dir/nested/file4', '/py-test-dir/nested/file5']

now = time.time()
pool = ThreadTool(1)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)


now = time.time()
pool = ThreadTool(2)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)

now = time.time()
pool = ThreadTool(3)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)

now = time.time()
pool = ThreadTool(4)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)

now = time.time()
pool = ThreadTool(5)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)


now = time.time()
pool = ThreadTool(6)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)

now = time.time()
pool = ThreadTool(7)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)

now = time.time()
pool = ThreadTool(8)
result = pool.map(upload, file_list)
pool.close()
pool.join()
print "8 并发耗时 %s" % (time.time()-now)
delete(file_list)