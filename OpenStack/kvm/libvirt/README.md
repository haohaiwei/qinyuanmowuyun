libvirt ERROR  qemuMonitorIO:697 : internal error: End of file from qemu monitor  
===  
环境信息  
* OS --> CentOS 7.3
* libvirt --> 3.2.0
* qemu --> 1.5.3  

kvm安装完虚拟机后，reboot后闪退，查看libvirt日志发现libvirt日志调试未打开
修改libvirtd.conf
添加如下：  
```vim
log_level = 3
log_outputs="1:file:/var/log/libvirt/libvirtd.log"
```
重启  
```bash
systemctl restart libvirtd
```
启动一台虚拟机，查看日志,libvirt无报错，虚拟机报错如下：  

```vim
Warning **: reds.c:3891:spice_server_add_interface: unsupported playback interface
((null):13267): Spice-Warning **: reds.c:3900:spice_server_add_interface: unsupported record interface
```

怀疑spice-server有问题  
```bash
rpm -qa|grep spice
```  
```vim
spice-glib-0.31-6.el7.x86_64
spice-gtk3-0.31-6.el7.x86_64
spice-vdagent-0.14.0-14.el7.x86_64
spice-server-0.12.8-2.el7.1.x86_64
spice-protocol-0.12.11-1.el7.noarch
```  
依赖都在  
```bash
yum install -y spice-server
``` 
启动虚拟机，可以正常启动了
