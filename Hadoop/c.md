# Hadoop 性能调优  

# hadoop调优可以从以下几个方面来调整:  

* Hadoop自身（配置参数相关）  
* Java虚拟机相关
* OS  
* 硬件  

此次暂不讨论硬件层面  

# 1.Hadoop  

Hadoop层级的调优主要是针对配置参数来进行调优  

原则如下:  

(1) 增大作业的并行程度，如，增大map任务的数量  
(2) 保证任务执行时有足够的资源  
(3) 满足前两条的前提下，尽可能为shuffle阶段提供资源  

 
hdfs-site.xml

* dfs.block.size 通过该参数可以配置hadoop文件块大小，2.7.3以前的版本默认为64M，之后默认为128M，建议调整为128M或者256M  
* dfs.namenode.handler.count 该参数表示namenode同时和datanode通信的线程数，默认为10，建议调整为40  
* dfs.datanode.max.xcievers  该参数相当于linux上文件句柄的限制，当datanode上面的连接数超过配置中的参数时，datanode就会拒绝连接，建议调整为65535  
* dfs.datanode.balance.bandwidthPerSe 该参数表示执行start-balance.sh的带宽，默认为1MB/s，建议增大到20MB/s    
* dfs.replication 该参数表示HDFS的副本数，默认为3，对于同一个文件来说，当读的请求过多，成为瓶颈时，可以通过增大副本数，来缓解这种情况，但是会造成大量的磁盘空间占用，这时，可以修改hadoop客户端的配置，这样，从Hadoop客户端上传的文件副本数，将以hadoop客户端为准  
* dfs.datanode.max.transfer.threads 该参数表示设置datanode在进行文件传输时的最大线程数，通常设置为8192，如果集群中某台的这个值比其他的主机大，那么，这台主机存储的数据相对于别的主机较多，导致数据分布不均匀，即使balance，仍然会不均匀  

core-site.xml  

* io.file.buffer.size  该参数控制hadoop缓冲区的大小，hadoop读和写还有map的中间结果都会用到这个缓冲区，默认时4KB，建议调整为128KB  

yarn-site.xml  

* yarn.nodemanager.resource.memory-mb 该参数表示该物理节点会有多少内存加入到资源池，设置时，需要注意为操作系统和其他服务预留资源  
* yarn.nodemanager.resource.cpu-vcores 该参数表示该物理节点会有多少虚拟cpu加入到资源池，设置时，需要注意为操作系统和其他服务预留资源  
* yarn.scheduler.increment-allocation-mb 该参数表示内存申请大小的规整化单位，默认为1024MB，即如果申请的内存为1.5GB，那么将被计算为2GB  
* yarn.scheduler.increment-allocation-vcores 该参数表示内存申请的规整化单位，默认为1个  
* yarn.scheduler.maximum-allocation-mb 该参数表示单个任务能够申请到的最大内存，根据容器内存容量设置，默认为8GB，如果设定为和参数yarn.nodemanager.resource.memory-mb一样，那么表示单个任务使用的内存资源不受限制  
* yarn.scheduler.minimum-allocation-mb 该参数表示当任务能够申请到的最小内存资源，默认为1GB  
* yarn.scheduler.maximum-allocation-vcores 该参数表示单个任务能够申请到的最大虚拟cpu数，默认为4，如果和arn.nodemanager.resource.cpu-vcores一样，表示不受限制  
 
mapred-site.xml  

* mapreduce.map.output.compress  该参数表示是否对map任务的中间结果进行压缩，当设置为true时，会占用一部分cpu资源，但是会减少数据传输的带宽，建议视具体环境而定  
* mapreduce.job.jvm.numtasks 该参数表示JVM重用设置，默认为1，表示一个JVM只能启动一个任务，可以设置为-1，表示1个JVM可以启动的任务不受限制，默认为true  
* mapreduce.cluster.local.dir 该参数表示mapreduce中间结果的本地存储路径，建议配置多个目录，提高I/O效率   
* mapred.child.java.opts 该参数表示执行map任务和reduce任务的JVM参数，还可以配置GC等一些常见的Java选项，由于map任务和reduce任务内存需求和堆大小一般是不同的，建议单独设置  
* mapreduce.map.java.opts 该参数表示执行map任务的JVM参数，map任务堆的大小可以在这里分别设置  
* mapreduce.reduce.java.opts 该参数为执行reduce任务的JVM参数，reduce任务的堆大小可以在这里设置  
* mapreduce.map.memory.mb 该参数表示map任务需要的内存大小，可以从mapreduce.map.java.opts继承，如果没有设置，该值根据容器内存设置  
* mapreduce.map.cpu.vcores 该参数表示map任务需要的虚拟cpu数，默认为1，根据容器虚拟cpu数设定，可以适当加大，尽量于mapredduce.map.memory.mb成线性比例，才不致于浪费  
* mapreduce.reduce.memory.mb 该参数表示reduce任务需要的内存大小，可以从mapreduce.reduce.java.opts参数继承，如果没有设置，该值根据容器内存设置  
* mapreduce.reduce.cpu.vcores 该参数表示reduce任务需要的虚拟cpu数，默认为1，可以适当加大，根据容器虚拟cpu数设定，可以适当加大，尽量于mapredduce.reduce.memory.mb成线性比例，才不致于浪费  







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

2. 关闭Huge Pages管理  

关闭THP，Huge Pages就是大小为2MB～1GB的内存页，THP是一个使管理Huge Pages自动化的抽象层，在运行Hadoop作业时，会造成cpu占用过高，因此需要关闭  



3. 文件系统相关  

hadoop 主要运行在linux上，对于centos6来说，一般采用ext4的文件系统，centos7来说，xfs则是主流，在挂载的时候需要做的一个优化工作是，禁用文件的访问时间，对应参数为noatime

4. 网络相关  
 
以双万兆网卡为例，可以配置的bond模式有7种，为0-6  

* Mode=0(balance-rr) 表示负载分担round-robin，和交换机的聚合强制不协商的方式配合 ，需要交换机配置  
* Mode=1(active-backup) 表示主备模式，只有一块网卡是active,另外一块是备的standby，交换机不需要任何修改  
* Mode=2(balance-xor) 表示XOR Hash负载分担，需要交换机配置（需要xmit_hash_policy）  
* Mode=3(broadcast) 表示所有包从所有interface发出，这个不均衡，只有冗余机制...和交换机的聚合强制不协商方式配合  
* Mode=4(802.3ad) 表示支持802.3ad协议，和交换机的聚合LACP方式配合（需要xmit_hash_policy）  
* Mode=5(balance-tlb) 是根据每个slave的负载情况选择slave进行发送，接收时使用当前轮到的slave，交换机不需要任何修改
* Mode=6(balance-alb) 在5的tlb基础上增加了rlb，交换机不需要任何修改  



