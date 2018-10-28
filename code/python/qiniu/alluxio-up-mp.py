# _*_coding:utf-8 _*_
import json
import sys
import alluxio
from alluxio import option, wire


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


def main():
    py_test_nested_dir = '/py-test-dir/nested1'
    py_test = py_test_nested_dir + '/py-test1'

    client = alluxio.Client('localhost', 39999)

    info("creating directory %s" % py_test_nested_dir)
    opt = option.CreateDirectory(recursive=True, write_type=wire.WRITE_TYPE_CACHE_THROUGH)
    client.create_directory(py_test_nested_dir, opt)
    info("done")

    info("writing to %s" % py_test)
    with client.open(py_test, 'w') as f:
        f.write('Alluxio works with Python!\n')
        with open(sys.argv[1]) as this_file:
            f.write(this_file)
    info("done")


if __name__ == '__main__':
    main()
