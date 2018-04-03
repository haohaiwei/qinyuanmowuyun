k8s 1.8.10 Build  
===  
1.环境信息  
---  
* OS-->centos7.2
* kubernetes-->1.8.10
* flannel-->0.9.1
* docker-->17.12.0-ce
* etcd-->3.2.12
* dashboard-->1.8.3  

2.环境配置  
---  
所有节点  
hosts文件  
```vim
127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
192.168.1.50 k8s01
192.168.1.51 k8s02
192.168.1.52 k8s03
```
关闭防火墙以及selinux  
```bash
service stop firewalld && systemctl disable firewalld
sed -i 's/enforcing/disabled/g' /etc/selinux/config
setenforce 0
```
时钟同步  
```bash
yum install -y ntp
vi /etc/ntp.conf
```
```vim
restrict 192.168.1.0 mask 255.255.255.0 nomodify notrap

# Use public servers from the pool.ntp.org project.
# Please consider joining the pool (http://www.pool.ntp.org/join.html).
server 202.120.2.101 iburst
```
另外2个节点  
```vim
server k8s01 iburst
```
配置内核参数  
```bash
cat << EOF > /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
vm.swappiness=0
EOF
```
配置生效  
```bash
modeprob bridge
sysctl -p /etc/sysctl.d/k8s.conf
```
centos7.3以及以上版本modprobe br_netfilter  
关闭交换分区  
```bash
swapoff -a
```
需要注意的是，在/etc/fstab中注释掉swap  
设置iptables策略  
```bash
iptables -nL(如果为accept，下面可省略)
/sbin/iptables -P FORWARD ACCEPT
echo  "sleep 60 && /sbin/iptables -P FORWARD ACCEPT" >> /etc/rc.local
```
安装依赖  
```bash
yum install -y epel-release
yum install -y yum-utils device-mapper-persistent-data lvm2 net-tools conntrack-tools wget
```
3.创建 CA 证书和秘钥  
---  
（使用CloudFlare 的 PKI 工具集 cfssl 来生成 Certificate Authority (CA) 证书和秘钥文件）  
操作在master节点k8s01上进行，只执行一次，然后拷贝到其他节点  
安装cfssl  
```bash
wget https://pkg.cfssl.org/R1.2/cfssl_linux-amd64
chmod +x cfssl_linux-amd64
mv cfssl_linux-amd64 /usr/local/bin/cfssl
wget https://pkg.cfssl.org/R1.2/cfssljson_linux-amd64
chmod +x cfssljson_linux-amd64
mv cfssljson_linux-amd64 /usr/local/bin/cfssljson
wget https://pkg.cfssl.org/R1.2/cfssl-certinfo_linux-amd64
chmod +x cfssl-certinfo_linux-amd64
mv cfssl-certinfo_linux-amd64 /usr/local/bin/cfssl-certinfo
export PATH=/usr/local/bin:$PATH
```
```bash
mkdir ~/ssl
cd ~/ssl
cat > ca-config.json << EOF
{
  "signing": {
    "default": {
      "expiry": "8760h"
    },
    "profiles": {
      "kubernetes": {
        "usages": [
            "signing",
            "key encipherment",
            "server auth",
            "client auth"
        ],
        "expiry": "8760h"
      }
    }
  }
}
EOF
```
ca-config.json：可以定义多个 profiles，分别指定不同的过期时间、使用场景等参数；后续在签名证书时使用某个 profile；   
signing：表示该证书可用于签名其它证书；生成的 ca.pem 证书中 CA=TRUE；   
server auth：表示 client 可以用该 CA 对 server 提供的证书进行验证；   
client auth：表示 server 可以用该 CA 对 client 提供的证书进行验证；  
创建CA证书签名请求  
```bash
cat > ca-csr.json << EOF
{
  "CN": "kubernetes",
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "CN",
      "ST": "BeiJing",
      "L": "BeiJing",
      "O": "k8s",
      "OU": "System"
    }
  ]
}
EOF
```
“CN”：Common Name，kube-apiserver 从证书中提取该字段作为请求的用户名 (User Name)；浏览器使用该字段验证网站是否合法；   
“O”：Organization，kube-apiserver 从证书中提取该字段作为请求用户所属的组 (Group)；  
生成 CA 证书和私钥  
```bash
cfssl gencert -initca ca-csr.json | cfssljson -bare ca
```
创建 kubernetes 证书签名请求文件  
```bash
cat > kubernetes-csr.json << EOF
{
   "CN": "kubernetes",
    "hosts": [
      "127.0.0.1",
      "192.168.1.50",
      "192.168.1.51",
      "192.168.1.52",
      "10.254.0.1",
      "kubernetes",
      "kubernetes.default",
      "kubernetes.default.svc",
      "kubernetes.default.svc.cluster",
      "kubernetes.default.svc.cluster.local"
    ],
    "key": {
        "algo": "rsa",
        "size": 2048
    },
    "names": [
        {
            "C": "CN",
            "ST": "BeiJing",
            "L": "BeiJing",
            "O": "k8s",
            "OU": "System"
        }
    ]
}
EOF
```
生成 kubernetes 证书和私钥  
```bash
cfssl gencert -ca=ca.pem -ca-key=ca-key.pem -config=ca-config.json -profile=kubernetes kubernetes-csr.json | cfssljson -bare kubernetes
```
创建admin证书  
```bash
cat > admin-csr.json << EOF
{
  "CN": "admin",
  "hosts": [],
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "CN",
      "ST": "BeiJing",
      "L": "BeiJing",
      "O": "system:masters",
      "OU": "System"
    }
  ]
}
EOF
```  

kube-apiserver 使用 RBAC 对客户端(如 kubelet、kube-proxy、Pod)请求进行授权；  
kube-apiserver 预定义了一些 RBAC 使用的 RoleBindings，如 cluster-admin 将 Group system:masters 与 Role cluster-admin 绑定，该 Role 授予了调用kube-apiserver 的所有 API的权限；  
OU 指定该证书的 Group 为 system:masters，kubelet 使用该证书访问 kube-apiserver 时 ，由于证书被 CA 签名，所以认证通过，同时由于证书用户组为经过预授权的 system:masters，所以被授予访问所有 API 的权限  
生成 admin 证书和私钥  
```bash
cfssl gencert -ca=ca.pem -ca-key=ca-key.pem -config=ca-config.json -profile=kubernetes admin-csr.json | cfssljson -bare admin
ll admin*
```  
创建 kube-proxy 证书  
```bash
cat > kube-proxy-csr.json << EOF
{
  "CN": "system:kube-proxy",
  "hosts": [],
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "CN",
      "ST": "BeiJing",
      "L": "BeiJing",
      "O": "k8s",
      "OU": "System"
    }
  ]
}
EOF
```  
CN 指定该证书的 User 为 system:kube-proxy； 
kube-apiserver 预定义的 RoleBinding cluster-admin 将User system:kube-proxy 与 Role system:node-proxier 绑定，该 Role 授予了调用 kube-apiserver Proxy 相关 API 的权限；
生成 kube-proxy 客户端证书和私钥  
```bash
cfssl gencert -ca=ca.pem -ca-key=ca-key.pem -config=ca-config.json -profile=kubernetes  kube-proxy-csr.json | cfssljson -bare kube-proxy
```  
将生成的证书和秘钥文件（后缀名为.pem）拷贝到所有机器的 /etc/kubernetes/ssl 目录下  
```bash
mkdir -p /etc/kubernetes/ssl
cp *.pem /etc/kubernetes/ssl

scp *.pem k8s02:/etc/kubernetes/ssl
scp *.pem k8s03:/etc/kubernetes/ssl
```  
4.部署etcd集群  
---  
```bash
wget https://github.com/coreos/etcd/releases/download/v3.2.12/etcd-v3.2.12-linux-amd64.tar.gz
tar -zxf etcd-v3.2.12-linux-amd64.tar.gz
cp etcd-v3.2.12-linux-amd64/etcd* /usr/local/bin
scp etcd-v3.2.12-linux-amd64/etcd* k8s02:/usr/local/bin
scp etcd-v3.2.12-linux-amd64/etcd* k8s03:/usr/local/bin
```
所有节点创建etcd工作目录（记录k8s集群信息）  
```bash
mkdir -p /var/lib/etcd
```  
创建systemd unit文件  
```bash
cat > etcd.service << EOF
[Unit]
Description=Etcd Server
After=network.target
After=network-online.target
Wants=network-online.target
Documentation=https://github.com/coreos

[Service]
Type=notify
WorkingDirectory=/var/lib/etcd/
ExecStart=/usr/local/bin/etcd \
  --name k8s01 \
  --cert-file=/etc/kubernetes/ssl/kubernetes.pem \
  --key-file=/etc/kubernetes/ssl/kubernetes-key.pem \
  --peer-cert-file=/etc/kubernetes/ssl/kubernetes.pem \
  --peer-key-file=/etc/kubernetes/ssl/kubernetes-key.pem \
  --trusted-ca-file=/etc/kubernetes/ssl/ca.pem \
  --peer-trusted-ca-file=/etc/kubernetes/ssl/ca.pem \
  --initial-advertise-peer-urls https://192.168.1.50:2380 \
  --listen-peer-urls https://192.168.1.50:2380 \
  --listen-client-urls https://192.168.1.50:2379,http://127.0.0.1:2379 \
  --advertise-client-urls https://192.168.1.50:2379 \
  --initial-cluster-token etcd-cluster-0 \
  --initial-cluster k8s01=https://192.168.1.50:2380,k8s02=https://192.168.1.51:2380,k8s03=https://192.168.1.52:2
  --initial-cluster-state new \
  --data-dir=/var/lib/etcd
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
cp etcd.service /etc/systemd/system/
scp etcd.service k8s02:/etc/systemd/system/
scp etcd.service k8s03:/etc/systemd/system/
```  
在k8s02和k8s03节点修改/etc/systemd/system/etcd.service
修改为节点对应信息   
```vim
--name 
--initial-advertise-peer-urls 
--listen-peer-urls 
--listen-client-urls
```
启动服务  
```bash
systemctl daemon-reload && systemctl start etcd && systemctl enable etcd
```
最先启动的节点，会卡住一段时间，直到另外2个节点加入  
验证服务  
```bash
etcdctl --ca-file=/etc/kubernetes/ssl/ca.pem  --cert-file=/etc/kubernetes/ssl/kubernetes.pem --key-file=/etc/kubernetes/ssl/kubernetes-key.pem  cluster-health
```
```vim
member b031925c340617d5 is healthy: got healthy result from https://192.168.1.51:2379
member c23156aeff22aa06 is healthy: got healthy result from https://192.168.1.52:2379
member d0fbccae5731ed67 is healthy: got healthy result from https://192.168.1.50:2379
cluster is healthy
```  
5.安装flannel  
---
```bash
wget https://github.com/coreos/flannel/releases/download/v0.9.1/flannel-v0.9.1-linux-amd64.tar.gz
tar -xzf flannel-v0.9.1-linux-amd64.tar.gz
cp {flanneld,mk-docker-opts.sh} /usr/local/bin
scp {flanneld,mk-docker-opts.sh} k8s02:/usr/local/bin
scp {flanneld,mk-docker-opts.sh} k8s03:/usr/local/bin
```
向etcd写入网段信息  
```bash
etcdctl --endpoints=https://192.168.1.50:2379,https://192.168.1.51:2379,https://192.168.1.52:2379 --ca-file=/etc/kubernetes/ssl/ca.pem --cert-file=/etc/kubernetes/ssl/kubernetes.pem --key-file=/etc/kubernetes/ssl/kubernetes-key.pem mkdir /kubernetes/network
etcdctl --endpoints=https://192.168.1.50:2379,https://192.168.1.51:2379,https://192.168.1.52:2379 --ca-file=/etc/kubernetes/ssl/ca.pem --cert-file=/etc/kubernetes/ssl/kubernetes.pem --key-file=/etc/kubernetes/ssl/kubernetes-key.pem mk /kubernetes/network/config '{"Network":"10.1.0.0/16","SubnetLen":24,"Backend":{"Type":"vxlan"}}'
```  
创建systemd unit文件  
```bash
cat > flanneld.service << EOF
[Unit]
Description=Flanneld overlay address etcd agent
After=network.target
After=network-online.target
Wants=network-online.target
After=etcd.service
Before=docker.service

[Service]
Type=notify
ExecStart=/usr/local/bin/flanneld \
  -etcd-cafile=/etc/kubernetes/ssl/ca.pem \
  -etcd-certfile=/etc/kubernetes/ssl/kubernetes.pem \
  -etcd-keyfile=/etc/kubernetes/ssl/kubernetes-key.pem \
  -etcd-endpoints=https://192.168.1.50:2379,https://192.168.1.51:2379,https://192.168.1.52:2379 \
  -etcd-prefix=/kubernetes/network
ExecStartPost=/usr/local/bin/mk-docker-opts.sh -k DOCKER_NETWORK_OPTIONS -d /run/flannel/docker
Restart=on-failure

[Install]
WantedBy=multi-user.target
RequiredBy=docker.service
EOF
cp flanneld.service /etc/systemd/system/
scp flanneld.service k8s02:/etc/systemd/system/
scp flanneld.service k8s03:/etc/systemd/system/
```
启动flannel  
```bash
systemctl daemon-reload && systemctl start flanneld && systemctl enable flanneld
```
验证服务  
```bash
etcdctl --endpoints=https://192.168.1.50:2379,https://192.168.1.51:2379,https://192.168.1.52:2379 --ca-file=/etc/kubernetes/ssl/ca.pem --cert-file=/etc/kubernetes/ssl/kubernetes.pem --key-file=/etc/kubernetes/ssl/kubernetes-key.pem ls /kubernetes/network/subnets
```
输出如下  

```vim
/kubernetes/network/subnets/10.1.75.0-24
/kubernetes/network/subnets/10.1.22.0-24
/kubernetes/network/subnets/10.1.47.0-24
```
6.部署kubectl工具，创建kubeconfig文件 装 
---  
```bash
wget https://dl.k8s.io/v1.8.10/kubernetes-client-linux-amd64.tar.gz
tar -xzvf kubernetes-client-linux-amd64.tar.gz
cd kubernetes/client/bin/
chmod a+x kubernetes/client/bin/kube*
```
创建/root/.kube/config   
```bash
kubectl config set-cluster kubernetes --certificate-authority=/etc/kubernetes/ssl/ca.pem --embed-certs=true --server=https://192.168.1.50:6443
kubectl config set-credentials admin --client-certificate=/etc/kubernetes/ssl/admin.pem --embed-certs=true --client-key=/etc/kubernetes/ssl/admin-key.pem
kubectl config set-context kubernetes --cluster=kubernetes --user=admin
kubectl config use-context kubernetes
```
创建bootstrap.kubeconfig  
```bash
export BOOTSTRAP_TOKEN=$(head -c 16 /dev/urandom | od -An -t x | tr -d ' ')
cat > token.csv <<EOF
${BOOTSTRAP_TOKEN},kubelet-bootstrap,10001,"system:kubelet-bootstrap"
EOF
mv token.csv /etc/kubernetes/
kubectl config set-cluster kubernetes --certificate-authority=/etc/kubernetes/ssl/ca.pem --embed-certs=true --server=https://192.168.1.50:6443 --kubeconfig=bootstrap.kubeconfig
kubectl config set-credentials kubelet-bootstrap --token=${BOOTSTRAP_TOKEN} --kubeconfig=bootstrap.kubeconfig
kubectl config set-context default --cluster=kubernetes --user=kubelet-bootstrap --kubeconfig=bootstrap.kubeconfig
kubectl config use-context default --kubeconfig=bootstrap.kubeconfig
mv bootstrap.kubeconfig /etc/kubernetes/
```
创建kube-proxy.kubeconfig  
```bash
kubectl config set-cluster kubernetes --certificate-authority=/etc/kubernetes/ssl/ca.pem --embed-certs=true --server=https://192.168.1.50:6443 --kubeconfig=kube-proxy.kubeconfig

#设置客户端认证参数
kubectl config set-credentials kube-proxy --client-certificate=/etc/kubernetes/ssl/kube-proxy.pem --client-key=/etc/kubernetes/ssl/kube-proxy-key.pem --embed-certs=true --kubeconfig=kube-proxy.kubeconfig

#设置上下文参数
kubectl config set-context default --cluster=kubernetes --user=kube-proxy --kubeconfig=kube-proxy.kubeconfig

设置默认上下文
kubectl config use-context default --kubeconfig=kube-proxy.kubeconfig
mv kube-proxy.kubeconfig /etc/kubernetes/
```
将生成的bootstrap.kubeconfig，kube-proxy.kubeconfig文件拷贝到其它节点的/etc/kubernetes目录下  
```bash
scp /etc/kubernetes/kube-proxy.kubeconfig k8s02:/etc/kubernetes/
scp /etc/kubernetes/bootstrap.kubeconfig  k8s02:/etc/kubernetes/

scp /etc/kubernetes/kube-proxy.kubeconfig k8s03:/etc/kubernetes/
scp /etc/kubernetes/bootstrap.kubeconfig  k8s03:/etc/kubernetes/
```
7.部署master节点  
---  
```bash
wget https://dl.k8s.io/v1.8.10/kubernetes-server-linux-amd64.tar.gz
tar -xzvf kubernetes-server-linux-amd64.tar.gz
cp -r kubernetes/server/bin/{kube-apiserver,kube-controller-manager,kube-scheduler,kubectl,kube-proxy,kubelet} /usr/local/bin/
```
配置和启动kube-apiserver  
```bash
cat > kube-apiserver.service << EOF
[Unit]
Description=Kubernetes API Server
Documentation=https://github.com/GoogleCloudPlatform/kubernetes
After=network.target
After=etcd.service

[Service]
ExecStart=/usr/local/bin/kube-apiserver \
  --logtostderr=true \
  --admission-control=NamespaceLifecycle,LimitRanger,ServiceAccount,DefaultStorageClass,ResourceQuota,NodeRestriion \
  --advertise-address=192.168.1.50 \
  --bind-address=192.168.1.50 \
  --insecure-bind-address=192.168.1.50 \
  --authorization-mode=Node,RBAC \
  --runtime-config=rbac.authorization.k8s.io/v1alpha1 \
  --kubelet-https=true \
  --enable-bootstrap-token-auth \
  --token-auth-file=/etc/kubernetes/token.csv \
  --service-cluster-ip-range=10.254.0.0/16 \
  --service-node-port-range=8400-10000 \
  --tls-cert-file=/etc/kubernetes/ssl/kubernetes.pem \
  --tls-private-key-file=/etc/kubernetes/ssl/kubernetes-key.pem \
  --client-ca-file=/etc/kubernetes/ssl/ca.pem \
  --service-account-key-file=/etc/kubernetes/ssl/ca-key.pem \
  --etcd-cafile=/etc/kubernetes/ssl/ca.pem \
  --etcd-certfile=/etc/kubernetes/ssl/kubernetes.pem \
  --etcd-keyfile=/etc/kubernetes/ssl/kubernetes-key.pem \
  --etcd-servers=https://192.168.1.50:2379,https://192.168.1.51:2379,https://192.168.1.52:2379 \
  --enable-swagger-ui=true \
  --allow-privileged=true \
  --apiserver-count=3 \
  --audit-log-maxage=30 \
  --audit-log-maxbackup=3 \
  --audit-log-maxsize=100 \
  --audit-log-path=/var/lib/audit.log \
  --anonymous-auth=false \
  --basic-auth-file=/etc/kubernetes/basic_auth_file \
  --event-ttl=1h \
  --v=2
Restart=on-failure
RestartSec=5
Type=notify
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
cp kube-apiserver.service /etc/systemd/system/

systemctl daemon-reload
systemctl enable kube-apiserver
systemctl start kube-apiserver
systemctl status kube-apiserver
```
配置和启动 kube-controller-manager  
```bash
cat > kube-controller-manager.service << EOF
[Unit]
Description=Kubernetes Controller Manager
Documentation=https://github.com/GoogleCloudPlatform/kubernetes

[Service]
ExecStart=/usr/local/bin/kube-controller-manager \
  --logtostderr=true  \
  --address=0.0.0.0 \
  --master=http://192.168.1.50:8080 \
  --allocate-node-cidrs=true \
  --service-cluster-ip-range=10.254.0.0/16 \
  --cluster-cidr=10.1.0.0/16 \
  --cluster-name=kubernetes \
  --cluster-signing-cert-file=/etc/kubernetes/ssl/ca.pem \
  --cluster-signing-key-file=/etc/kubernetes/ssl/ca-key.pem \
  --service-account-private-key-file=/etc/kubernetes/ssl/ca-key.pem \
  --root-ca-file=/etc/kubernetes/ssl/ca.pem \
  --leader-elect=true \
  --v=2
Restart=on-failure
LimitNOFILE=65536
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
cp kube-controller-manager.service /etc/systemd/system/

systemctl daemon-reload
systemctl enable kube-controller-manager
systemctl start kube-controller-manager
```
配置和启动 kube-scheduler  
```bash
cat > kube-scheduler.service << EOF
[Unit]
Description=Kubernetes Scheduler
Documentation=https://github.com/GoogleCloudPlatform/kubernetes

[Service]
ExecStart=/usr/local/bin/kube-scheduler \
  --logtostderr=true \
  --address=0.0.0.0 \
  --master=http://192.168.1.50:8080 \
  --leader-elect=true \
  --v=2
Restart=on-failure
LimitNOFILE=65536
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
cp kube-scheduler.service /etc/systemd/system/

systemctl daemon-reload
systemctl enable kube-scheduler
systemctl start kube-scheduler
```
验证master  
```bash

kubectl get componentstatuses
```bash
输出如下  
```vim
NAME                 STATUS    MESSAGE              ERROR
scheduler            Healthy   ok                   
controller-manager   Healthy   ok                   
etcd-2               Healthy   {"health": "true"}   
etcd-0               Healthy   {"health": "true"}   
etcd-1               Healthy   {"health": "true"}  
```
8.部署Node节点3个节点都作为node节点  
---  
配置和启动docker  
```bash
wget https://download.docker.com/linux/static/stable/x86_64/docker-17.12.0-ce.tgz
tar -xvf docker-17.12.0-ce.tgz
cp docker/docker* /usr/local/bin
cat > docker.service << EOF
[Unit]
Description=Docker Application Container Engine
Documentation=http://docs.docker.io

[Service]
Environment="PATH=/usr/local/bin:/bin:/sbin:/usr/bin:/usr/sbin"
EnvironmentFile=-/run/flannel/subnet.env
EnvironmentFile=-/run/flannel/docker
ExecStart=/usr/local/bin/dockerd \
  --exec-opt native.cgroupdriver=cgroupfs \
  --log-level=error \
  --log-driver=json-file 
ExecReload=/bin/kill -s HUP $MAINPID
ExecStartPost=/sbin/iptables -I FORWARD -s 0.0.0.0/0 -j ACCEPT
Restart=on-failure
RestartSec=5
LimitNOFILE=infinity
LimitNPROC=infinity
LimitCORE=infinity
Delegate=yes
KillMode=process

[Install]
WantedBy=multi-user.target
EOF
```  
启动  
```bash
cp docker.service /etc/systemd/system/docker.service

systemctl daemon-reload
systemctl enable docker
systemctl start docker
systemctl status docker
```
安装和配置 kubelet   
kubelet 启动时向 kube-apiserver 发送 TLS bootstrapping 请求，需要先将 bootstrap token 文件中的 kubelet-bootstrap 用户赋予 system:node-bootstrapper 角色，然后 kubelet 才有权限创建认证请求  
master点执行  
```bash
kubectl create clusterrolebinding kubelet-bootstrap --clusterrole=system:node-bootstrapper --user=kubelet-bootstrap
scp -r kubernetes/server/bin/{kube-proxy,kubelet} k8s02:/usr/local/bin/
scp -r kubernetes/server/bin/{kube-proxy,kubelet} k8s03:/usr/local/bin/
```
所有节点创建kubelet 工作目录  
```bash
mkdir -p /var/lib/kubelet
```
配置启动kubelet  
```bash
cat > kubelet.service << EOF
[Unit]
Description=Kubernetes Kubelet
Documentation=https://github.com/GoogleCloudPlatform/kubernetes
After=docker.service
Requires=docker.service

[Service]
WorkingDirectory=/var/lib/kubelet
ExecStart=/usr/local/bin/kubelet \
  --address=192.168.1.50 \
  --hostname-override=192.168.1.50 \
  --pod-infra-container-image=registry.access.redhat.com/rhel7/pod-infrastructure:latest \
  --experimental-bootstrap-kubeconfig=/etc/kubernetes/bootstrap.kubeconfig \
  --kubeconfig=/etc/kubernetes/kubelet.kubeconfig \
  --require-kubeconfig \
  --cert-dir=/etc/kubernetes/ssl \
  --container-runtime=docker \
  --cluster-dns=10.254.0.2 \
  --cluster-domain=cluster.local \
  --hairpin-mode promiscuous-bridge \
  --allow-privileged=true \
  --serialize-image-pulls=false \
  --register-node=true \
  --logtostderr=true \
  --cgroup-driver=cgroupfs  \
  --v=2

Restart=on-failure
KillMode=process
LimitNOFILE=65536
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
cp kubelet.service /etc/systemd/system/kubelet.service
scp kubelet.service k8s02:/etc/systemd/system/kubelet.service
scp kubelet.service k8s03:/etc/systemd/system/kubelet.service
systemctl daemon-reload
systemctl enable kubelet
systemctl start kubelet
systemctl status kubelet
#k8s02和k8s03节点注意修改节点的地址
```
kubelet 首次启动时向 kube-apiserver 发送证书签名请求，必须授权通过后，Node才会加入到集群中 
在三个节点都部署完kubelet之后，在master节点执行授权操作  
查询授权请求   
```bash
kubectl get csr
```
授权  
```bash
kubectl certificate approve xxx xxx xxx
```
验证  
```bash
kubectl get node
```
配置和启动kube-proxy  
```bash
mkdir -p /var/lib/kube-proxy
cat > kube-proxy.service << EOF
[Unit]
Description=Kubernetes Kube-Proxy Server
Documentation=https://github.com/GoogleCloudPlatform/kubernetes
After=network.target

[Service]
WorkingDirectory=/var/lib/kube-proxy
ExecStart=/usr/local/bin/kube-proxy \
  --bind-address=192.168.1.50 \
  --hostname-override=192.168.1.50 \
  --cluster-cidr=10.254.0.0/16 \
  --kubeconfig=/etc/kubernetes/kube-proxy.kubeconfig \
  --logtostderr=true \
  --v=2
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
cp kube-proxy.service /etc/systemd/system/
scp kube-proxy.service k8s02:/etc/systemd/system/
scp kube-proxy.service k8s03:/etc/systemd/system/
systemctl daemon-reload
systemctl enable kube-proxy
systemctl start kube-proxy
systemctl status kube-proxy
systemctl daemon-reload
systemctl enable kube-proxy
systemctl start kube-proxy
systemctl status kube-proxy
#k8s02和k8s03注意修改对应IP
```
配置dns插件  
```bash 
wget https://github.com/kubernetes/kubernetes/releases/download/v1.8.10/kubernetes.tar.gz
tar xzvf kubernetes.tar.gz

cd /root/kubernetes/cluster/addons/dns
mv  kubedns-svc.yaml.sed kubedns-svc.yaml
#把文件中$DNS_SERVER_IP替换成10.254.0.2
sed -i 's/$DNS_SERVER_IP/10.254.0.2/g' ./kubedns-svc.yaml

mv ./kubedns-controller.yaml.sed ./kubedns-controller.yaml
#把$DNS_DOMAIN替换成cluster.local
sed -i 's/$DNS_DOMAIN/cluster.local/g' ./kubedns-controller.yaml

ls *.yaml
kubedns-cm.yaml  kubedns-controller.yaml  kubedns-sa.yaml  kubedns-svc.yaml

kubectl create -f .
```
配置dashboard   
```bash
wget https://raw.githubusercontent.com/kubernetes/dashboard/master/src/deploy/recommended/kubernetes-dashboard.yaml
