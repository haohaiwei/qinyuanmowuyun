AlphaPose Environment Build  
===

1.环境信息  
---

* OS-->ubuntu-16.0.4  
* torch-->7  
* tensorflow-gpu-->1.2  
* python-->2.7.14  
* Anaconda2-->5.1.0  

2.Anaconda2安装  
---

```bash
wget https://repo.continuum.io/archive/Anaconda2-5.1.0-Linux-x86_64.sh
bash Anaconda2-5.1.0-Linux-x86_64.sh
source .bashrc
```
按照提示即可  
3.tensorflow安装  
---

```bash
conda create -n tensorflow python=2.7
source activate tensorflow
pip install tensorflow-gpu==1.2 -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install cython -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install --user opencv-python -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install matplotlib -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install pillow -i https://pypi.tuna.tsinghua.edu.cn/simple
pip install h5py -i https://pypi.tuna.tsinghua.edu.cn/simple
```
不安装最新版本，因为本机cuda8.0，最新版本需要9.0支持  
测试  
```bash
python -c "import tensorflow"
```
无报错即为正常  
4.torch安装  
---
```bash
git clone https://github.com/torch/distro.git ~/torch --recursive
cd torch
bash install-deps
./install.sh
```
最后需要注意的是，选yes，这样会自动把环境变量写入到.bashrc中  
```bash
cd ~
source .bashrc
```
安装hdf5依赖，后面AlphaPose要用到  
这里不能直接通过lua安装，直接安装的hdf5是1.10版本，在torch中会提示不支持  
```bash
git clone https://github.com/anibali/torch-hdf5.git
cd torch-hdf5/
git checkout hdf5-1.10 
luarocks make hdf5-0-0.rockspec
```
测试  
```bash
th -lhdf5
```
正常则如下所示  
```vim
(tensorflow) hao@hao-HP-Z440-Workstation:~/torch$ th -lhdf5
 
  ______             __   |  Torch7 
 /_  __/__  ________/ /   |  Scientific computing for Lua. 
  / / / _ \/ __/ __/ _ \  |  Type ? for help 
 /_/  \___/_/  \__/_//_/  |  https://github.com/torch 
                          |  http://torch.ch 
	
th> 
```
5.AlphaPose安装  
---
```bash
git clone https://github.com/MVIG-SJTU/AlphaPose.git
cd AlphaPose/human-detection/lib/
make clean
make
cd newnms/
make
cd ../../../
./install.sh
chmod +x fetch_models.sh
./fetch_models.sh
```
测试  
```bash
./run.sh --indir examples/demo/ --outdir examples/results/ --vis
```
正常输出结果如下  
```vim
generating bbox from Faster RCNN...
/home/hao/anaconda2/lib/python2.7/site-packages/h5py/__init__.py:36: FutureWarning: Conversion of the second argument of issubdtype from `float` to `np.floating` is deprecated. In future, it will be treated as `np.float64 == np.dtype(float).type`.
  from ._conv import register_converters as _register_converters
2018-03-20 05:28:38.136208: W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.1 instructions, but these are available on your machine and could speed up CPU computations.
2018-03-20 05:28:38.136231: W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use SSE4.2 instructions, but these are available on your machine and could speed up CPU computations.
2018-03-20 05:28:38.136236: W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX instructions, but these are available on your machine and could speed up CPU computations.
2018-03-20 05:28:38.136241: W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use AVX2 instructions, but these are available on your machine and could speed up CPU computations.
2018-03-20 05:28:38.136246: W tensorflow/core/platform/cpu_feature_guard.cc:45] The TensorFlow library wasn't compiled to use FMA instructions, but these are available on your machine and could speed up CPU computations.
2018-03-20 05:28:38.483083: I tensorflow/core/common_runtime/gpu/gpu_device.cc:940] Found device 0 with properties: 
name: GeForce GTX 980 Ti
major: 5 minor: 2 memoryClockRate (GHz) 1.114
pciBusID 0000:02:00.0
Total memory: 5.93GiB
Free memory: 3.34GiB
2018-03-20 05:28:38.483115: I tensorflow/core/common_runtime/gpu/gpu_device.cc:961] DMA: 0 
2018-03-20 05:28:38.483122: I tensorflow/core/common_runtime/gpu/gpu_device.cc:971] 0:   Y 
2018-03-20 05:28:38.483136: I tensorflow/core/common_runtime/gpu/gpu_device.cc:1030] Creating TensorFlow device (/gpu:0) -> (device: 0, name: GeForce GTX 980 Ti, pci bus id: 0000:02:00.0)
Loaded network ../output/res152/coco_2014_train+coco_2014_valminusminival/default/res152.ckpt
/home/hao/AlphaPose/examples/demo/

 33%|████████████████████████████████████████▋                                                                       67%|████████████████████████████████████████████████████████████████████████████100%|██████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████| 3/3 [00:05<00:00,  1.92s/it]
pose estimation with RMPE...
MPII	
 [======================================== 69/69 ======================================>]  Tot: 11s137ms | Step: 163ms  
----------Finished----------	
/home/hao/anaconda2/lib/python2.7/site-packages/h5py/__init__.py:36: FutureWarning: Conversion of the second argument of issubdtype from `float` to `np.floating` is deprecated. In future, it will be treated as `np.float64 == np.dtype(float).type`.
  from ._conv import register_converters as _register_converters
visualization...
  0%|                                                                                                                                  | 0/3 [00:00<?, ?it/s]Qt: XKEYBOARD extension not present on the X server.
/home/hao/anaconda2/lib
Use QT_XKB_CONFIG_ROOT environmental variable to provide an additional search path, add ':' as separator to provide several search paths and/or make sure that XKB configuration data directory contains recent enough contents, to update please see http://cgit.freedesktop.org/xkeyboard-config/ .
 33%|████████████████████████████████████████▋                                                                       67%|████████████████████████████████████████████████████████████████████████████100%|██████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████| 3/3 [00:02<00:00,  1.32it/s]
(tensorflow) hao@hao-HP-Z440-Workstation:~/AlphaPose$ 
```



