Spark Cluster envirnonment Build  
===  

1.版本信息  
---
* OS-->centos7.2
* Java-->1.8_161
* Hadoop-->2.7.3
* Spark-->2.2.1  

2.节点信息以及角色分配   
---
* hadoop-1 datanode namenode DFSZKFailoverController 
* hadoop-2 datanode zookeeper journalnode NM Master
* hadoop-3 datanode zookeeper journalnode RM NM worker  
* hadoop-4 datanode Secondnamenode NM DFSZKFailoverController worker  
* hadoop-5 datanode zookeeper journalnode NM SecondRM worker  

3.环境搭建  
---
本次只写Spark搭建，hadoop请参考[Hadoop](../Hadoop/README.md)
将下载好的的spark的压缩包上传到hadoop-2节点  
scala上传到hadoop-2，hadoop-3,hadoop-4,hadoop-5节点  
2,3,4,5节点执行  

```bash
rpm -ivh scala-2.11.8.rpm
```

hadoop-2  

```bash
tar -zxf spark-2.2.1-bin-hadoop2.7.tgz -C /usr/local
```

修改配置文件  

```bash
cd /usr/local/spark/spark-2.2.1-bin-hadoop2.7/conf
cp slaves.template slaves
cp spark-env.sh.template spark-env.sh  
```

修改slaves和spark-env.sh文件  
slaves  

```vim
hadoop-3
hadoop-4
hadoop-5
```

spark-env.sh

```vim
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
export SCALA_HOME=/usr/share/scala  
export HADOOP_HOME=/usr/local/hadoop/hadoop-2.7.3
export SPARK_MASTER_IP=192.168.1.116
export SPARK_MASTER_PORT=7077  
export SPARK_MASTER_WEBUI_PORT=7070  
export SPARK_WORKER_CORES=2  
export SPARK_WORKER_MEMORY=1024m  
export SPARK_WORKER_INSTANCES=2  
export SPARK_CLASSPATH=$HBASE_HOME/lib/hbase-protocol-1.2.4.jar:$HBASE_HOME/lib/hbase-common-1.2.4.jar:$HBASE_HOME/lib/htrace-core-3.1.0-incubating.jar:$HBAS
E_HOME/lib/hbase-server-1.2.4.jar:$HBASE_HOME/lib/hbase-client-1.2.4.jar:$HBASE_HOME/lib/metrics-core-2.2.0.jar:$SPARK_CLASSPATH  
export SPARK_LOCAL_DIR="/mnt/spark/tmp"  
export SPARK_JAVA_OPTS="-Dspark.storage.blockManagerHeartBeatMs=60000-Dspark.local.dir=$SPARK_LOCAL_DIR -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:$S
PARK_HOME/logs/gc.log -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=60"
```

修改完成后，在hadoop-2节点  

```bash
for i in $[,3,4,5];do scp -rq /usr/local/spark/ root@hadoop-$i:/usr/local/;done
/usr/local/spark/spark-2.2.1-bin-hadoop2.7/sbin/start-all.sh
```

测试  
---

hadoop-2节点
自己写一个测试程序提交  
standalone模式

```bash
spark-submit --class kafkademo test.jar --master hadoop-2:7077
```

yarn模式  
```bash
spark-submit --class testhbase --master yarn --deploy-mode cluster test.jar
```

测试代码如下  
需要注意的是打成jar包时，需要去掉.setMaster("spark://hadoop-2:7077")  

```scala
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ConnectionFactory, Put}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapred.TableOutputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapred.JobConf
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}


object testhbase {
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf().setMaster("spark://hadoop-2:7077").setAppName("hbase"))
    val rdd = sc.makeRDD(Array(1)).flatMap(_ => 0 to 1000000)
    rdd.foreachPartition(x => {
      val hbaseConf = HBaseConfiguration.create()
      hbaseConf.set("hbase.zookeeper.quorum", "hadoop-2,hadoop-3,hadoop-5")
      hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
      hbaseConf.set("hbase.defaults.for.version.skip", "true")
      val hbaseConn = ConnectionFactory.createConnection(hbaseConf)
      val table = hbaseConn.getTable(TableName.valueOf("word01"))
      x.foreach(value => {
        var put = new Put(Bytes.toBytes(value.toString))
        put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("c1"), Bytes.toBytes(value.toString))
        table.put(put)
        })
  })
 }
}
```



