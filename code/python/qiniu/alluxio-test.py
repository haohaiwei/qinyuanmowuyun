# _*_coding:utf-8 _*_
import json
import sys
import alluxio
from alluxio import option,wire
import time
from time import ctime, sleep
import threading
import thread


# import random
# import pylab as pl
# from multiprocessing.dummy import Pool as ThreadTool


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


def pretty_json(obj):
    return json.dumps(obj, indent=2)


class MyThread(threading.Thread):
    def __init__(self, args):
        threading.Thread.__init__(self)
        self.args = args


    # def getResult(self):
    #     return self.res

    def run(self):
        print 'starting', self.name, 'at:', ctime()
        # self.res = self(*self.args)
        print self, 'finished at:', ctime()


def up1():
    # py_test_root_dir = '/py-test-dir1'
    py_test_nested_dir = '/py-test-dir/nested1'
    py_test = py_test_nested_dir + '/py-test1'
    # py_test_renamed = py_test_root_dir + '/py-test-renamed'

    client = alluxio.Client('localhost', 39999)

    info("creating directory %s" % py_test_nested_dir)
    opt = alluxio.option.CreateDirectory(recursive=True, write_type=wire.WRITE_TYPE_CACHE_THROUGH)
    client.create_directory(py_test_nested_dir, opt)
    info("done")

    info("writing to %s" % py_test)
    with client.open(py_test, 'w') as f:
        f.write('Alluxio works with Python!\n')
        with open(sys.argv[1]) as this_file:
            f.write(this_file)
    info("done")


def up2():
    py_test_nested_dir = '/py-test-dir/nested2'
    py_test = py_test_nested_dir + '/py-test2'
    client = alluxio.Client('localhost', 39999)
    info("creating directory %s" % py_test_nested_dir)
    opt = option.CreateDirectory(recursive=True)
    client.create_directory(py_test_nested_dir, opt)
    info("done")

    info("writing to %s" % py_test)
    with client.open(py_test, 'w') as f:
        f.write('Alluxio works with Python!\n')
        with open(sys.argv[1]) as this_file:
            f.write(this_file)
    info("done")


def up3():
    py_test_nested_dir = '/py-test-dir/nested3'
    py_test = py_test_nested_dir + '/py-test3'
    client = alluxio.Client('localhost', 39999)
    info("creating directory %s" % py_test_nested_dir)
    opt = option.CreateDirectory(recursive=True)
    client.create_directory(py_test_nested_dir, opt)
    info("done")

    info("writing to %s" % py_test)
    with client.open(py_test, 'w') as f:
        f.write('Alluxio works with Python!\n')
        with open(sys.argv[1]) as this_file:
            f.write(this_file)
    info("done")


test = [up1, up2, up3]


def main():
    # print 'starting at:', ctime()
    # thread.start_new_thread(up1,())
    # thread.start_new_thread(up2,())
    # thread.start_new_thread(up3,())
    # print 'all done at:', ctime()
    test1 = range(len(test))
    print '\n *** 单线程'
    for i in test1:
        print 'starting', test[i].__name__, 'at:', ctime()
        print test[i]
        print test[i].__name__, 'finished at:', ctime()
    print '\n *** 多线程'
    threads = []
    for i in test1:
        t = MyThread(test[i])
        threads.append(t)
    for i in test1:
        threads[i].start()
    for i in test1:
        threads[i].join()
        print threads[i]
    print 'all done'

    # info("getting status of %s" % py_test)
    # stat = client.get_status(py_test)
    # print pretty_json(stat.json())
    # info("done")
    #
    # info("renaming %s to %s" % (py_test, py_test_renamed))
    # client.rename(py_test, py_test_renamed)
    # info("done")
    #
    # info("getting status of %s" % py_test_renamed)
    # stat = client.get_status(py_test_renamed)
    # print pretty_json(stat.json())
    # info("done")
    #
    # info("reading %s" % py_test_renamed)
    # with client.open(py_test_renamed, 'r') as f:
    #     print f.read()
    # info("done")
    #
    # info("listing status of paths under /")
    # root_stats = client.list_status('/')
    # for stat in root_stats:
    #     print pretty_json(stat.json())
    # info("done")
    #
    # info("deleting %s" % py_test_root_dir)
    # opt = option.Delete(recursive=True)
    # client.delete(py_test_root_dir, opt)
    # info("done")
    #
    # info("asserting that %s is deleted" % py_test_root_dir)
    # assert not client.exists(py_test_root_dir)
    # info("done")


if __name__ == '__main__':
    main()
