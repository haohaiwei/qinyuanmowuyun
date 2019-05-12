# Ceph	 luminous部署  

## 1. 环境信息  

* OS --> CentOS7.3
* ceph -->  luminous
* python --> 2.7.5

## 2. 基础环境配置  

1. 集群互信，以及时钟同步  
2. 防火墙以及selinux  
3. hosts文件配置  
4. ceph-deploy工具安装  
```bash
yum install -y ceph-deploy
```

## 3. 部署  

1. mon以及osd部署
```bash
mkdir ceph-cluster
cd ceph-cluster
ceph-deploy new ceph-1 ceph-2 ceph-3
ceph-deploy install ceph-1 ceph-2 ceph-3 --repo-url=http://mirrors.aliyun.com/ceph/rpm-luminous/el7/ --gpgurl=http://mirrors.aliyun.com/centos/RPM-GPG-KEY-CentOS-7 --release=luminous
```
2. 调整ceph副本数
```bash
vi ceph.conf
#set repl  num
osd pool default size = 2
```
3. 配置初始 monitor(s)、并收集所有密钥
```bash
ceph-deploy mon create-initial
```
4. 配置osd,每个盘作为1个osd
```bash
#list disk
ceph-deploy disk list ceph-1
#add block device
ceph-deploy disk zap ceph-1:vdb
#add osd
ceph-deploy osd create ceph-1 --data /dev/vdb 
#--block-db /dev/sdf1 --block-wal /dev/sdf2(省略这2个参数，则默认数据放在同一个盘上）
#拷贝配置文件到其他节点
copy-deploy admin client ceph-1 ceph-2 ceph-3
```

5. mgr配置dashboard

```bash
ceph-mgr -I ceph-1 ceph-2 ceph-3
ceph mgr module enable dashboard
ceph config-key put mgr/dashboard/server_addr 172.20.5.241
ceph config-key put mgr/dashboard/server_port 7000
```
weh访问地址：http://172.20.5.241:7000

6. 查看状态
```bash
ceph -s
```

## 4. CephFS部署  

1. add metadata and create fs
```bash
ceph-deploy mds create ceph-1
ceph osd pool create cephfs_data 64
ceph osd pool create cephfs_metadata 64
ceph fs new nfs cephfs_metadata cephfs_data
```
2. mount CephFS ，it need kernel support
```bash
mount.ceph ceph-1:/ /mnt/cephfs/ -o name=admin,secret=AQAzCfRadRzIDhAAATbRfsO6kOhqDKKPejrRnw==
```
## 5. 对象存储部署  

1. rgw安装  

```bash
ceph-deploy install --rgw ceph-1 ceph-2 ceph-3
ceph-deploy rgw create ceph-1 ceph-2 ceph-3
```

2. 创建用户  

```bash
radosgw-admin user create --uid=hao --display-name="hao" --email=admin@xx.com
```

```vim
{
	"user_id": "hao",
	"display_name": "hao",
	"email": "admin@xx.com",
	"suspended": 0,
	"max_buckets": 1000,
	"auid": 0,
	"subusers": [],
	"keys": [
		{
			"user": "hao",
			"access_key": "65IHUP77R1X5FPNH1EOK",
			"secret_key": "vKi5MGmChDh6p3qivFlPljiJWGrzXj75V5Vp2Cge"
		}
	],
	"swift_keys": [],
	"caps": [],
	"op_mask": "read, write, delete",
	"default_placement": "",
	"placement_tags": [],
	"bucket_quota": {
		"enabled": false,
		"check_on_raw": false,
		"max_size": -1,
		"max_size_kb": 0,
		"max_objects": -1
	},
	"user_quota": {
		"enabled": false,
		"check_on_raw": false,
		"max_size": -1,
		"max_size_kb": 0,
		"max_objects": -1
	},
	"temp_url_keys": [],
	"type": "rgw"
}
```

3. 创建子用户  

```bash
radosgw-admin subuser create --uid=hao --subuser=hao:swift --access=full
#access权限可分为read，write，readwrite，full
```

4. S3接口创建bucket测试,安装依赖  

```bash
yum install -y python2-boto
```

5. s3接口测试  

测试脚本如下:  
```python
import boto.s3.connection

access_key = '65IHUP77R1X5FPNH1EOK'
secret_key = 'vKi5MGmChDh6p3qivFlPljiJWGrzXj75V5Vp2Cge'
conn = boto.connect_s3(
		aws_access_key_id=access_key,
		aws_secret_access_key=secret_key,
		host='ceph-1', port=7480,
		is_secure=False, calling_format=boto.s3.connection.OrdinaryCallingFormat(),
	   )

bucket = conn.create_bucket('my-new-bucket')
for bucket in conn.get_all_buckets():
	print "{name} {created}".format(
		name=bucket.name,
		created=bucket.creation_date,
	)
```

## 6. swift测试
```bash
pip install python-swiftclient
#list bucket
swift -A http://ceph-1:7480/auth/1.0 -U hao:swift -K 'h3v0WiIkWt3kYMggKd3zVQPDsX3H4uCUe9ixzQRt' list
```
## 7. RBD（块设备）测试  

1. 创建pool  

```bash
#create pool and set pg_num
ceph osd create pool test 256
#set pgp_num
ceps osd pool set test pgp_num 256
#enable rbd
ceph osd pool application enable test rbd
```

2. 创建rbd  

```bash
red create test/hao --size=1024
```

3.映射本地  

```bash
rbd  map test/hao
#报错
rbd: image hao: image uses unsupported features: 0x38
#新版本中，ceph会为image打上许多标签，因此禁用部分特性
rbd feature disable test/hao exclusive-lock, object-map, fast-diff, deep-flatten
rbd  map test/hao
```  

```vim
[root@ceph-1 ceph-cluster]# rbd map test/hao
/dev/rbd0
```

4. 格式化挂载到本地  

```bash
mkfs.xfs /dev/rbd0
mount -t xfs /dev/rbd0 /mnt
df -h
```

```vim
[root@ceph-1 ceph-cluster]# df -h
Filesystem      Size  Used Avail Use% Mounted on
/dev/vda2        19G  2.2G   17G  12% /
devtmpfs        3.9G     0  3.9G   0% /dev
tmpfs           3.9G     0  3.9G   0% /dev/shm
tmpfs           3.9G   33M  3.8G   1% /run
tmpfs           3.9G     0  3.9G   0% /sys/fs/cgroup
/dev/vda1      1014M  136M  879M  14% /boot
tmpfs           783M     0  783M   0% /run/user/0
tmpfs           3.9G   48K  3.9G   1% /var/lib/ceph/osd/ceph-1
tmpfs           3.9G   48K  3.9G   1% /var/lib/ceph/osd/ceph-2
tmpfs           3.9G   48K  3.9G   1% /var/lib/ceph/osd/ceph-3
172.20.5.241:/  270G  9.2G  261G   4% /nfstest
/dev/rbd0      1014M   33M  982M   4% /mnt
```



