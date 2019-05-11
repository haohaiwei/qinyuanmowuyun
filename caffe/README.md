# CentOS 7.4 下 Caffe 环境搭建(GPU版本)  

* 此次安装只开放python接口，matlab留待下次

## 1. 各组件版本(可以手动编译，此次省略)  

* OS CentOS Linux release 7.4.1708 (Core)
* GPU GeForce GTX 980 Ti
* Cuda 8.0
* Cudnn 5.1
* Blas openBLAS
* Nvidia 384.98
* OpenCV 3.4.0
* System Python 2.7.5
* Venv Python 2.7.11

这里采用pyenv来管理系统上面不同的python版本
安装方式如下:
```bash
git clone https://github.com/yyuu/pyenv.git ~/.pyenv
vi .bashrc
##需要加入的部分
export PYENV_ROOT="$HOME/.pyenv"
export PATH="$PYENV_ROOT/bin:$PATH"
eval "$(pyenv init -)"
##
# Source global definitions
if [ -f /etc/bashrc ]; then
. /etc/bashrc
fi
source /root/.bashrc
```
## 2. Build Python ENV
```bash
pyenv install anaconda2-4.0.0
pyenv rehash
pyenv global anaconda2-4.0.0 
echo 'alias activat="source $PYENV_ROOT/versions/anaconda2-4.0.0/bin/activate"' >> ~/.bashrc 
source .bashrc 
python
```
版本管理如下
切换系统版本
```bash
[root@localhost ~]# pyenv global system 
[root@localhost ~]# python
Python 2.7.5 (default, Aug 4 2017, 00:39:18) 
[GCC 4.8.5 20150623 (Red Hat 4.8.5-16)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> exit()
切换anaconda版本
[root@localhost ~]# pyenv global anaconda2-4.0.0 
[root@localhost ~]# python
Python 2.7.11 |Anaconda 4.0.0 (64-bit)| (default, Dec 6 2015, 18:08:32) 
[GCC 4.4.7 20120313 (Red Hat 4.4.7-1)] on linux2
Type "help", "copyright", "credits" or "license" for more information.
Anaconda is brought to you by Continuum Analytics.
Please check out: http://continuum.io/thanks and https://anaconda.org
>>> 
```
依赖安装
```bash
yum install -y protobuf-devel leveldb-devel snappy-devel boost-devel hdf5-devel
yum install readline readline-devel readline-static -y
yum install openssl openssl-devel openssl-static -y
yum install sqlite-devel -y
yum install bzip2-devel bzip2-libs -y
yum install -y libprotobuf-dev libleveldb-dev libsnappy-dev libhdf5-serial-dev protobuf-compiler
```
## 3. 安装openCV
这里选择效率高的opencv3.0以上
需要手动编译，官方yum源只有2.4的
下载地址如下
https://github.com/opencv/opencv/archive/3.4.0.tar.gz
手动编译需要cmake编译器
```bash
yum install cmake gcc gcc-c++ gtk+-devel gimp-devel gimp-devel-tools gimp-help-browser zlib-devel libtiff-devel libjpeg-devel libpng-devel gstreamer-devel libavc1394-devel libraw1394-devel libdc1394-devel jasper-devel jasper-utils swig python libtool nasm
解压
tar -zxf opencv-3.4.0.tar.gz
cd opencv-3.4.0
yum install cmake gcc gcc-c++ gtk+-devel gimp-devel gimp-devel-tools gimp-help-browser zlib-devel libtiff-devel libjpeg-devel libpng-devel gstreamer-devel libavc1394-devel libraw1394-devel libdc1394-devel jasper-devel jasper-utils swig python libtool nasm
# 解压
tar -zxf opencv-3.4.0.tar.gz
cd opencv-3.4.0
mkdir build
cd build
cmake ..
make
make install
```
上述cmake阶段有可能会卡在IPPICV，和网速有关，可以手动下载ippicv_2017u3_lnx_intel64_general_20170822.tgz
然后进入/root/opencv3.4.0文件夹创建一个.cache文件夹，并拷贝相应的文件：
```bash 
cd /root/opencv3.3.1 ipp_file=ippicv_2017u3_lnx_intel64_general_20170822.tgz && ipp_hash=$(md5sum /root/$ipp_file | cut -d" " -f1) && ipp_dir=.cache/ippicv && mkdir -p ${ipp_dir} && cp /root/$ipp_file $ipp_dir/$ipp_hash-$ipp_file 
```
## 4. 安装caffe依赖 手动编译或者yum均可 
```bash
yum install -y openblas-devel  gflags-devel glog-devel lmdb-devel 
git clone https://github.com/BVLC/caffe.git ~/caffe
使用pyenv切换python版本
pyenv global anaconda2-4.0.0
安装python依赖
尽量修改为pip
for req in $(cat caffe/python/requirements.txt);do pip install $req;done
cp Makefile.config.example Makefile.config
# 修改Makefile.config
# gpu版本
USE_CUDNN := 1
OPENCV_VERSION := 3
BLAS := open
BLAS_INCLUDE := /usr/include/openblas
注释掉
#PYTHON_INCLUDE := /usr/include/python2.7 \
		/usr/lib/python2.7/dist-packages/numpy/core/include
使用anaconda
ANACONDA_HOME := $(PYENV_ROOT)/versions/anaconda2-4.0.0
PYTHON_INCLUDE := $(ANACONDA_HOME)/include \
		 $(ANACONDA_HOME)/include/python2.7 \
		 $(ANACONDA_HOME)/lib/python2.7/site-packages/numpy/core/include
cpu版本
#USE_CUDNN := 1
CPU_ONLY := 1
OPENCV_VERSION := 3
BLAS := open
BLAS_INCLUDE := /usr/include/openblas
# 注释掉
#PYTHON_INCLUDE := /usr/include/python2.7 \
		/usr/lib/python2.7/dist-packages/numpy/core/include
# 使用anaconda
ANACONDA_HOME := $(PYENV_ROOT)/versions/anaconda2-4.0.0
PYTHON_INCLUDE := $(ANACONDA_HOME)/include \
		 $(ANACONDA_HOME)/include/python2.7 \
		 $(ANACONDA_HOME)/lib/python2.7/site-packages/numpy/core/include

make all
make runtest
make pycaffe
```
## 5. 实例测试
```bash
./data/mnist/get_mnist.sh
./examples/mnist/create_mnist.sh
./examples/mnist/train_lenet.sh
```

