oracle linux 6.5 安装oracle 11g R2 rac集群  
===
实验环境   
---

* 虚拟化软件virtualBox  
* 虚拟机操作系统oracle linux 6.5  
* 虚拟机双网口  
* 一个public network  
* 一个prviate network/jlib  
* 磁盘30G，分配的时候，6G留给交换分区，其他全部划分到根目录下  

1.操作系统以及依赖安装  
---
安装操作系统时需勾选的选项  
* Base System > Base  
* Base System > Compatibility libraries  
* Base System > Hardware monitoring utilities  
* Base System > Large Systems Performance  
* Base System > Network file system client  
* Base System > Performance Tools  
* Base System > Perl Support  
* Servers > Server Platform  
* Servers > System administration tools  
* Desktops > Desktop  
* Desktops > Desktop Platform  
* Desktops > Fonts  
* Desktops > General Purpose Desktop  
* Desktops > Graphical Administration Tools  
* Desktops > Input Methods  
* Desktops > X Window System  
* Applications > Internet Browser  
* Development > Additional Development  
* Development > Development Tools  

2.挂载镜像  
---  
```bash
mount -0 loop centos.iso /mnt
cd /mnt/Packages/
rpm -Uvh binutils-2.*
rpm -Uvh compat-libstdc++-33-3.2.3-69.el6.i686.rpm 
rpm -Uvh compat-libstdc++-33-3.2.3-69.el6.x86_64.rpm 
rpm -Uvh elfutils-libelf-0.152-1.el6.i686.rpm 
rpm -Uvh elfutils-libelf-0.152-1.el6.x86_64.rpm 
rpm -Uvh libaio-0.3.107-10.el6.i686.rpm 
rpm -Uvh libaio-0.3.107-10.el6.x86_64.rpm 
rpm -Uvh libaio-devel-0.3.107-10.el6.i686.rpm 
rpm -Uvh libaio-devel-0.3.107-10.el6.x86_64.rpm 
rpm -Uvh sysstat-9.0.4-22.el6.x86_64.rpm 
rpm -Uvh glibc-2.12-1.132.el6.i686.rpm 
rpm -Uvh glibc-2.12-1.132.el6.x86_64.rpm 
rpm -Uvh glibc-common-2.12-1.132.el6.x86_64.rpm 
rpm -Uvh glibc-devel-2.12-1.132.el6.x86_64.rpm
rpm -Uvh glibc-devel-2.12-1.132.el6.i686.rpm 
rpm -Uvh glibc-headers-2.12-1.132.el6.x86_64.rpm 
rpm -Uvh ksh-20120801-10.el6.x86_64.rpm 
rpm -Uvh make-3.81-20.el6.x86_64.rpm 
rpm -Uvh libgcc-4.4.7-4.el6.i686.rpm 
rpm -Uvh libgcc-4.4.7-4.el6.x86_64.rpm 
rpm -Uvh libstdc++-4.4.7-4.el6.i686.rpm 
rpm -Uvh libstdc++-4.4.7-4.el6.x86_64.rpm 
rpm -Uvh gcc-4.4.7-4.el6.x86_64.rpm 
rpm -Uvh gcc-c++-4.4.7-4.el6.x86_64.rpm 
rpm -Uvh elfutils-libelf-0.152-1.el6.i686.rpm 
rpm -Uvh elfutils-libelf-0.152-1.el6.x86_64.rpm 
rpm -Uvh elfutils-libelf-devel-0.152-1.el6.i686.rpm 
rpm -Uvh elfutils-libelf-devel-0.152-1.el6.x86_64.rpm 
rpm -Uvh libtool-ltdl-2.2.6-15.5.el6.i686.rpm 
rpm -Uvh ncurses*i686*
rpm -Uvh readline*i686*
rpm -Uvh unixODBC* 
```


3.修改内核参数  
---
```bash
vi /etc/sysctl.conf
```
```vim
fs.aio-max-nr = 1048576
fs.file-max = 6815744
kernel.shmmni = 4096
kernel.sem = 250 32000 100 128
net.ipv4.ip_local_port_range = 9000 65500
net.core.rmem_default=262144
net.core.rmem_max=4194304
net.core.wmem_default=262144
net.core.wmem_max=1048586
```
执行生效  
```bash
sysctl -p
```
有报错
```vim
error: "net.bridge.bridge-nf-call-ip6tables" is an unknown key
error: "net.bridge.bridge-nf-call-iptables" is an unknown key
error: "net.bridge.bridge-nf-call-arptables" is an unknown key
```
加载内核模块  
```bash
modprobe bridge
sysctl -p
```
修改文件  
```bash
vi /etc/security/limits.conf
```
```vim
oracle              soft    nproc   2047
oracle              hard    nproc   16384
oracle              soft    nofile  4096
oracle              hard    nofile  65536
oracle              soft    stack   10240
oracle              soft    memlock 10485760
orcale              hard    memlock 10485760

grid              soft    nproc   2047
grid              hard    nproc   16384
grid              soft    nofile  4096
grid              hard    nofile  65536
grid              soft    stack   10240
```
修改文件  
```bash
vi /etc/pam.d/login 
```
```vim
session    required     pam_limits.so
```
```bash
sed -i 's/enforcing/disabled/g' /etc/selinux/config 
setenforce 0
chkconfig iptables off
service iptables stop
```
创建oracle所需用户和用户组  
```bash
/usr/sbin/groupadd -g 1000 oinstall 
/usr/sbin/groupadd -g 1020 asmadmin 
/usr/sbin/groupadd -g 1021 asmdba 
/usr/sbin/groupadd -g 1022 asmoper 
/usr/sbin/groupadd -g 1031 dba 
/usr/sbin/groupadd -g 1032 oper
/usr/sbin/useradd -g oinstall -G asmadmin,asmdba,asmoper,oper,dba -u 1100 grid 
/usr/sbin/useradd -g oinstall -G dba,asmdba,oper -u 1101 oracle
```
然后修改oracle和grid密码

在grid和oracle用户下配置互信
test01节点  
```bash
ssh-keygen -t dsa
ssh-copy-id -i .ssh/id_dsa.pub test01
ssh-copy-id -i .ssh/id_dsa.pub test02
```
test02节点  
```bash
ssh-keygen -t dsa
ssh-copy-id -i .ssh/id_dsa.pub test01
ssh-copy-id -i .ssh/id_dsa.pub test02
```
修改oracle和grid环境变量
节点1  
```bash
su - oracle
vi .bash_profile
```
```vim
ORACLE_SID=orcl1; export ORACLE_SID
ORACLE_BASE=/u01/app/oracle; export ORACLE_BASE
ORACLE_HOME=$ORACLE_BASE/product/11.2.0/db_1; export ORACLE_HOME
PATH=${PATH}:/usr/bin:/bin:/sbin:/usr/bin/X11:/usr/local/bin:$ORACLE_HOME/bin
PATH=${PATH}:/oracle/product/common/oracle/bin
export PATH

LD_LIBRARY_PATH=$ORACLE_HOME/lib
LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:$ORACLE_HOME/oracm/lib
LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib:/usr/lib:/usr/local/lib
export LD_LIBRARY_PATH

CLASSPATH=$ORACLE_HOME/JRE
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/rdbms/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/network/jlib
export CLASSPATH

export TEMP=/tmp
export TMPDIR=/tmp
umask 022
```
节点2  
```bash
su - oracle
vi .bash_profile
```
```vim
ORACLE_SID=orcl2; export ORACLE_SID
ORACLE_BASE=/u01/app/oracle; export ORACLE_BASE
ORACLE_HOME=$ORACLE_BASE/product/11.2.0/db_1; export ORACLE_HOME
PATH=${PATH}:/usr/bin:/bin:/sbin:/usr/bin/X11:/usr/local/bin:$ORACLE_HOME/bin
PATH=${PATH}:/oracle/product/common/oracle/bin
export PATH

LD_LIBRARY_PATH=$ORACLE_HOME/lib
LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:$ORACLE_HOME/oracm/lib
LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib:/usr/lib:/usr/local/lib
export LD_LIBRARY_PATH

CLASSPATH=$ORACLE_HOME/JRE
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/rdbms/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/network/jlib
export CLASSPATH

export TEMP=/tmp
export TMPDIR=/tmp
umask 022
```
grid环境变量
节点1  
```bash
su - grid
vi .bash_profile
```
```vim
export ORACLE_HOME=/u01/app/11.2.0/grid/
export PATH=$ORACLE_HOME/bin:$ORACLE_HOME/OPatch:/sbin:/bin:/usr/sbin:/usr/bin

export ORACLE_SID=+ASM1
export LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/lib32

export ORACLE_BASE=/u01/app/grid
export ORA_NLS10=$ORACLE_HOME/nls/data
export NLS_LANG=american_america.AL32UTF8 

CLASSPATH=$ORACLE_HOME/JRE
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/rdbms/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/network/jlib
export CLASSPATH
```
节点2  
```bash
su - grid
vi .bash_profile
```
```vim
export ORACLE_HOME=/u01/app/11.2.0/grid/
export PATH=$ORACLE_HOME/bin:$ORACLE_HOME/OPatch:/sbin:/bin:/usr/sbin:/usr/bin

export ORACLE_SID=+ASM2
export LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/lib32

export ORACLE_BASE=/u01/app/grid
export ORA_NLS10=$ORACLE_HOME/nls/data
export NLS_LANG=american_america.AL32UTF8 

CLASSPATH=$ORACLE_HOME/JRE
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/rdbms/jlib
CLASSPATH=${CLASSPATH}:$ORACLE_HOME/network/jlib
export CLASSPATH
```

SID根据实际情况选择  

6.时钟同步配置  
---
```bash
vi /etc/sysconfig/ntpd
```
```vim
OPTIONS="-x -u ntp:ntp -p /var/run/ntpd.pid -g"
```
其中一个节点和外部时钟服务器同步，另一个同步到这个节点
本例中test01和外部同步，test02和test01同步
//
共享存储创建
创建完成后分区
```bash
/sbin/scsi_id -g -u -d /dev/sdb1
/sbin/scsi_id -g -u -d /dev/sdc1
/sbin/scsi_id -g -u -d /dev/sdd1
/sbin/scsi_id -g -u -d /dev/sde1
/sbin/scsi_id -g -u -d /dev/sdf1
```
dev绑定磁盘路径和权限  
```bash
vi /etc/udev/rules.d/99-oracle-asmdevices.
```
```vim
KERNEL=="sd*", BUS=="scsi", PROGRAM=="/sbin/scsi_id -g -u -d /dev/$parent", RESULT=="1ATA_VBOX_HARDDISK_VB67713a3b-d1
448e77",NAME="asm-disk1", OWNER="grid", GROUP="asmadmin", MODE="0660"
KERNEL=="sd*", BUS=="scsi", PROGRAM=="/sbin/scsi_id -g -u -d /dev/$parent", RESULT=="1ATA_VBOX_HARDDISK_VB6b88618f-dc
ac53c7",NAME="asm-disk2", OWNER="grid", GROUP="asmadmin", MODE="0660"
KERNEL=="sd*", BUS=="scsi", PROGRAM=="/sbin/scsi_id -g -u -d /dev/$parent", RESULT=="1ATA_VBOX_HARDDISK_VB8e0e340e-bc
75d7d4",NAME="asm-disk3", OWNER="grid", GROUP="asmadmin", MODE="0660"
KERNEL=="sd*", BUS=="scsi", PROGRAM=="/sbin/scsi_id -g -u -d /dev/$parent", RESULT=="1ATA_VBOX_HARDDISK_VBb7a7ff67-64
0abe54",NAME="asm-disk4", OWNER="grid", GROUP="asmadmin", MODE="0660"
KERNEL=="sd*", BUS=="scsi", PROGRAM=="/sbin/scsi_id -g -u -d /dev/$parent", RESULT=="1ATA_VBOX_HARDDISK_VB2bd1aa20-86
735a83",NAME="asm-disk5", OWNER="grid", GROUP="asmadmin", MODE="0660"
```

多路径绑定配置  
```vim
multipath {
                wwid                    360050767008080096800000000000049
                alias                   RAC_DATA_01
        }
        multipath {
                wwid                    36005076700808009680000000000004d
                alias                   RAC_DATA_02
        }
        multipath {
                wwid                    360050767008080096800000000000051
                alias                   RAC_DATA_03
        }
        multipath {
                wwid                    360050767008080096800000000000055
                alias                   RAC_DATA_04
        }
        multipath {
                wwid                    360050767008080096800000000000059
                alias                   RAC_DATA_05
        }
        multipath {
                wwid                    36005076700808009680000000000002d
                alias                   RAC_DATA_06
        }
        multipath {
                wwid                    360050767008080096800000000000031
                alias                   RAC_DATA_07
        }
        multipath {
                wwid                    360050767008080096800000000000035
                alias                   RAC_DATA_08
        }
        multipath {
                wwid                    360050767008080096800000000000039
                alias                   RAC_DATA_09
        }
        multipath {
                wwid                    36005076700808009680000000000003d
                alias                   RAC_DATA_10
        }
        multipath {
                wwid                    360050767008080096800000000000041
                alias                   RAC_VOTEDISK_01
        }
        multipath {
                wwid                    360050767008080096800000000000045
                alias                   RAC_VOTEDISK_02
        }
       

        multipath {
                wwid                    36000d7780000394d22c054bc122c83a8
                alias                   FAL_voteDISK_03

```
udev绑定配置  

```vim

//
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000049",SYMLINK+="asm-toyo-disk01",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-36005076700808009680000000000004d",SYMLINK+="asm-toyo-disk02",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000051",SYMLINK+="asm-toyo-disk03",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000055",SYMLINK+="asm-toyo-disk04",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000059",SYMLINK+="asm-toyo-disk05",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-36005076700808009680000000000002d",SYMLINK+="asm-toyo-disk06",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000031",SYMLINK+="asm-toyo-disk07",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000035",SYMLINK+="asm-toyo-disk08",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000039",SYMLINK+="asm-toyo-disk09",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-36005076700808009680000000000003d",SYMLINK+="asm-toyo-disk10",OWNER="grid", GROUP="asmadmin", MO
DE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000041",SYMLINK+="asm-toyo-votedisk01",OWNER="grid", GROUP="asmadmin"
, MODE="0660"
KERNEL=="dm-*",ENV{DM_UUID}=="mpath-360050767008080096800000000000045",SYMLINK+="asm-toyo-votedisk02",OWNER="grid", GROUP="asmadmin"
, MODE="0660"
```

