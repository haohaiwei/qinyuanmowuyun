# Hadoop 性能调优  

# hadoop调优可以从以下几个方面来调整:  

* Hadoop自身（配置参数相关）  
* Java虚拟机相关
* OS  
* 硬件  

此次暂不讨论硬件层面  

# 1.Hadoop  


# 2.Java  

# 3.OS  
1. 内核参数相关  
vm.swappiness (值为0-100，这里设置为0，避免使用交换分区，交换分区是指OS在物理内存不够的情况下，将数据交换到磁盘上，大幅降低性能）
vm.overcommit_memory (值为0，1，2，0:表示内核将检查是否有足够的可用内存供应用进程使用，如果足够，则内存申请允许，如果不足，则申请失败，并将错误返回给应用进程，1:表示内核允许分配所有的物理内存，不管当前内存状态如何，2:表示内核允许分配超过所有物理内存和交换空间总和的内存，可以通过vm.overcommit_ratio的值设置超过的比率。一般设置为50%，表示超过物理内存的50%
net.core.somaxconn (表示套接字（socket）监听的backlog上限。backlog是套接字的监听队列，当一个请求尚未被处理或者建立时，会进入backlog，而套接字服务器可以一次性处理backlog中所有的请求，处理后的请求不再位于监听队列中，建议调大于或者等于32768）
最大文件描述符: 包括系统最大打开文件描述符和进程最大打开文件描述符  
系统最大打开文件描述符可以通过fs.file-max设置
进程最大打开文件描述符可以通过ulimit -n 临时设置，永久的话请修改/etc/security/limits.conf 文件  
```vim
* hard nofile 1000000
* soft nofile 1000000
root hard nofile 1000000
root soft nofile 1000000
```
关闭THP，Huge Pages就是大小为2MB～1GB的内存页，THP是一个使管理Huge Pages自动化的抽象层，在运行Hadoop作业时，会造成cpu占用过高，因此需要关闭  



2. 文件系统相关  
hadoop 主要运行在linux上，对于centos6来说，一般采用ext4的文件系统，centos7来说，xfs则是主流，在挂载的时候需要做的一个优化工作是，禁用文件的访问时间，对应参数为noatime

3. 网络相关  
以双万兆网卡为例，可以配置的bond模式有7种，为0-6
* Mode=0(balance-rr) 表示负载分担round-robin，和交换机的聚合强制不协商的方式配合 ，需要交换机配置  
* Mode=1(active-backup) 表示主备模式，只有一块网卡是active,另外一块是备的standby，交换机不需要任何修改  
* Mode=2(balance-xor) 表示XOR Hash负载分担，需要交换机配置（需要xmit_hash_policy）  
* Mode=3(broadcast) 表示所有包从所有interface发出，这个不均衡，只有冗余机制...和交换机的聚合强制不协商方式配合  
* Mode=4(802.3ad) 表示支持802.3ad协议，和交换机的聚合LACP方式配合（需要xmit_hash_policy）  
* Mode=5(balance-tlb) 是根据每个slave的负载情况选择slave进行发送，接收时使用当前轮到的slave，交换机不需要任何修改
* Mode=6(balance-alb) 在5的tlb基础上增加了rlb，交换机不需要任何修改  



