CDH集群安装  
===  

基础环境信息  
---  

OS --> CentOS 7.4
CDH --> 5.13.3
JDK --> 1.8.161
MariaDB --> 10.1.32
Spark --> 2.3.0
集群角色信息规划  
---  

 
基础环境配置  
---  

1.	操作系统
最小化安装即可
2.	JDK环境
选择oracle JDK，摒弃openJDK
rpm -ivh jdk-8u161-linux-x64.rpm
添加环境变量到profile中  
```bash
vi /etc/profile
```
```vim
JAVA_HOME=/usr/java/jdk1.8.0_161
PATH=$JAVA_HOME/bin
CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
export JAVA_HOME
export PATH
export CLASSPATH
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:$PATH
```
3.	防火墙以及selinux  
```bash
systemctl stop firewalld && systemctl disable firewalld  
sed -i 's/enforcing/disabled/g' /etc/selinux/config 
setenforce 0  
```
4.	修改主机名，配置hosts文件  

```bash
hostnamectl set-hostname tm-cdh-xx(ip最后1位)
```
配置hosts文件  
```bash
Vi /etc/hosts
```
```vim
xx.xx.xx.xx tm-cdh-11
xx.xx.xx.xx tm-cdh-12
xx.xx.xx.xx tm-cdh-13
xx.xx.xx.xx tm-cdh-14
xx.xx.xx.xx tm-cdh-15
```
5.	配置时钟同步以及时区  
```bash
timedatectl set-timezone Asia/Shanghai
yum install -y ntp
```
集群内部以tm-cdh-11为时钟源，tm-cdh-11与外部时钟同步  
Tm-cdh-11节点  
```bash
vi /etc/ntp.conf
```
```vim
restrict 192.168.113.0 mask 255.255.255.0 nomodify notrap
server 202.120.2.101 iburst
```
其他节点  
```vim
server tm-cdh-11 iburst
```
启动ntp  
```bash
systemctl start ntpd && systemctl enable ntpd
ntpq -np
```
6.	安装依赖  
```bash
yum install -y perl psmisc libxslt(all node)
```
tm-cdh-11节点  
```bash
yum install -y httpd mod_ssl 
```
7.	在tm-cdh-11节点配置并安装mariadb  
配置mariadb源  
```bash
vi /etc/yum.repo.d/mariadb.repo
```
```vim
[mariadb]
name = MariaDB
baseurl = http://yum.mariadb.org/10.1/centos7-amd64
gpgkey=https://yum.mariadb.org/RPM-GPG-KEY-MariaDB
gpgcheck=1
```
```bash
yum install -y MariaDB-server MariaDB-client
```
修改配置文件，优化mariadb参数  
```bash
vi /etc/my.cnf.d/server.cnf
```
```vim
[server]

# this is only for the mysqld standalone daemon
[mysqld]
transaction-isolation = READ-COMMITTED
key_buffer = 16M
key_buffer_size = 32M
max_allowed_packet = 32M
thread_stack = 256K
thread_cache_size = 64
query_cache_limit = 8M
query_cache_size = 64M
query_cache_type = 1
log_bin=/var/lib/mysql/mysql_binary_log
max_connections = 1024
binlog_format = mixed
log-error=/var/log/mariadb/mariadb.log
read_buffer_size = 2M
read_rnd_buffer_size = 16M
sort_buffer_size = 8M
join_buffer_size = 8M
innodb_file_per_table = 1
innodb_flush_log_at_trx_commit  = 2
innodb_log_buffer_size = 64M
innodb_buffer_pool_size = 4G
innodb_thread_concurrency = 8
innodb_flush_method = O_DIRECT
innodb_log_file_size = 512M
bind-address=0.0.0.0
```
启动mariadb  
```bash
systemctl start mariadb && systemctl enable mariadb
```

初始化mariadb配置  
```bash
mysql_secure_installation
```
8.	集群内部互信配置  
配置tm-cdh-11和tm-cdh-14节点  
```bash
ssh-keygen -t dsa
ssh-copy-id -i .ssh/id_dsa.pub tm-cdh-11
ssh-copy-id -i .ssh/id_dsa.pub tm-cdh-12
ssh-copy-id -i .ssh/id_dsa.pub tm-cdh-13
ssh-copy-id -i .ssh/id_dsa.pub tm-cdh-14
ssh-copy-id -i .ssh/id_dsa.pub tm-cdh-15
```
9.	配置系统参数  
```bash
vi /etc/sysctl.conf
```
```vim
vm.swappiness=0
```
//
关闭大内存分页  
```bash
echo never > /sys/kernel/mm/transparent_hugepage/defrag
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```
10.	格式化数据盘  
```bash
for i in /dev/sd{b,c,d,e,f,g};do parted $i mklabel gpt && parted $i mkpart primary 2048s 100%; done
mkfs.xfs /dev/sdb1
mkfs.xfs /dev/sdc1
mkfs.xfs /dev/sdd1
mkfs.xfs /dev/sde1
mkfs.xfs /dev/sdf1
mkfs.xfs /dev/sdg1
mkdir -p /mnt/disk1
mkdir -p /mnt/disk2
mkdir -p /mnt/disk3
mkdir -p /mnt/disk4
mkdir -p /mnt/disk5
mkdir -p /mnt/disk6
```
挂载  
```bash
Vi /etc/fstab
```
```vim
/dev/sdb1 /mnt/disk1   	xfs 			defaults		0 0
/dev/sdc1 /mnt/disk2  	xfs 			defaults		0 0
/dev/sdd1 /mnt/disk3   	xfs 			defaults		0 0
/dev/sde1 /mnt/disk4   	xfs 			defaults		0 0
/dev/sdf1 /mnt/disk5   	xfs 			defaults		0 0
/dev/sdg1 /mnt/disk6   	xfs 			defaults		0 0
```
```bash
mount -a
```
四、	CDH安装  
1.	安装包准备（tm-cdh-11）节点  
```bash
tar -zxf cloudera-manager-centos7-cm5.13.3_x86_64.tar.gz -C /opt/
tar -zxf mysql-connector-java-5.1.45.tar.gz 
cd /opt/ cloudera/parcel-repo/
mv /root/CDH-5.13.3-1.cdh5.13.3.p0.2-el7.parcel* ./
mv CDH-5.13.3-1.cdh5.13.3.p0.2-el7.parcel.sha1 CDH-5.13.3-1.cdh5.13.3.p0.2-el7.parcel.sha
mv /root/manifest.json ./ 
cp /root/mysql-connector-java-5.1.45/mysql-connector-java-5.1.45-bin.jar /opt/cm-5.13.3/share/cmf/lib/
```  

mysql驱动放到/usr/share/java目录下（先查看目录是存在，不存在先创建目录）  
```bash
cp /root/mysql-connector-java-5.1.45/mysql-connector-java-5.1.45-bin.jar /usr/share/java
```
2.	CDH配置  
   	所有节点  
```bash
useradd --system --home=/opt/cm-5.13.3/run/cloudera-scm-server --no-create-home --shell=/bin/false --comment "Cloudera SCM User" cloudera-scm
```
修改agent配置文件  
```bash
vim /opt/cm-5.13.3/etc/cloudera-scm-agent/config.ini
```
```vim
# Hostname of the CM server.
server_host=tm-cdh-11

# Port that the CM server is listening on.
server_port=7182
```
拷贝到其他节点  
```bash
scp -r /opt/cm-5.13.3/ tm-cdh-xx:/opt/
```
tm-cdh-11节点  

执行建库脚本  
内容如下：  
```sql
CREATE USER 'amon'@'*' IDENTIFIED BY'amon';
CREATE USER 'rman'@'*' IDENTIFIED BY'rman';
CREATE USER 'hive'@'*' IDENTIFIED BY'hive';
CREATE USER 'sentry'@'*' IDENTIFIED BY'sentry';
CREATE USER 'nav'@'*' IDENTIFIED BY 'nav';
CREATE USER 'navms'@'*' IDENTIFIED BY'navms';
CREATE USER 'hue'@'*' IDENTIFIED BY 'hue';
CREATE USER 'oozie'@'*' IDENTIFIED BY'oozie';
 
create database amon DEFAULT CHARACTER SET utf8;
grant all on amon.* TO 'amon'@'%'IDENTIFIED BY 'amon';
 
create database rman DEFAULT CHARACTER SET utf8;
grant all on rman.* TO 'rman'@'%'IDENTIFIED BY 'rman';
 
create database hive DEFAULT CHARACTER SET utf8;
grant all on hive.* TO 'hive'@'%'IDENTIFIED BY 'hive';
 
create database sentry DEFAULT CHARACTER SET utf8;
grant all on sentry.* TO 'sentry'@'%'IDENTIFIED BY 'sentry';
 
create database nav DEFAULT CHARACTER SET utf8;
grant all on nav.* TO 'nav'@'%' IDENTIFIED BY 'nav';
 
create database navms DEFAULT CHARACTER SET utf8;
grant all on navms.* TO 'navms'@'%'IDENTIFIED BY 'navms';
 
create database hue DEFAULT CHARACTER SET utf8;
grant all on hue.* to 'hue'@'%' identified by 'hue';
 
create database oozie DEFAULT CHARACTER SET utf8;
grant all on oozie.* to 'oozie'@'%'identified by 'oozie';
 
FLUSH PRIVILEGES;
```

```bash 
source /root/cdh.sql
/opt/cm-5.13.3/share/cmf/schema/scm_prepare_database.sh mysql cm -uroot -h tm-cdh-11 -p123456 --scm-host tm-cdh-11 root 123456
mkdir -p /var/lib/cloudera-scm-server
chown cloudera-scm.cloudera-scm /var/lib/cloudera-scm-server/
```
启动server  
```bash
/opt/cm-5.13.3/etc/init.d/cloudera-scm-server start
```
启动agent
```bash
/opt/cm-5.13.3/etc/init.d/cloudera-scm-agent start
```
3.	安装  
登陆地址tm-cdh-11:7180  
用户名密码默认admin,admin  
剩下的就是按照集群角色规划，分配角色  
 

