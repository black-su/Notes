环境：windom10
参考：https://www.jianshu.com/p/367f0886e62b
https://blog.csdn.net/wangkaishou/article/details/90055482
http://www.dedecms.com/knowledge/servers/linux-bsd/2012/0822/12849.html


1. 安装ubuntu系统，做双系统启动。
     之所以选择了双系统启动，而不是安装虚拟机，是因为Vmware虚拟机安装ubuntu系统失败，win10专业版自带的Hyper-v虚拟机安装ubuntu系统后，ubuntu系统中的数据在硬盘中的存储位置不能指定，导致C盘空间瞬间爆满。这两个问题都无法解决，选择了双系统启动，相对于虚拟机中的ubuntu系统操作上有延迟卡顿的感觉，双系统的操作更加流畅。
      准备的东西：
       iso镜像：https://cn.ubuntu.com/download
       
       下载安装UltralSo，作U盘启动
         win10下删除一个硬盘卷，为ubuntu系统预留硬盘位置
 我的电脑 → 管理  → 磁盘管理  → 选择一个盘进行删除卷
        
        重启电脑，重启过程中按F11，进入BOSS模式，选择



分区：
     
/分区：ext4  400G


ubuntu系统装完后需要重启，重启后，启动项里面没有ubuntu引导项。需要开启win10，
以管理员的身份在cmd中敲入命令：
bcdedit /set "{bootmgr}" path \EFI\ubuntu\grubx64.efi

再重启就有ubuntu的引导项了。




2. ubuntu环境配置
下载git：sudo apt-get install git
下载python：sudo apt-get install python
下载curl：sudo apt-get install curl
 新建bin文件：cd ~/
mkdir bin
下载repo：curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
chmod a+x ~/bin/repo

 path路径配置：
根目录下新建.bash_profile
PATH=~/.bin:$PATH
运行source ~/.bash_profile

关闭终端再打开，查看上述的命令是否生效
git --version
python --version
repo –version

如果repo还不生效，终端输入
echo 'export PATH=$PATH:$HOME/bin' >> ~/.bashrc
export PATH=$PATH:$HOME/bin

修改bin/repo文件中的REPO_URL，指向国内清华提供的镜像地址
REPO_URL = 'https://aosp.tuna.tsinghua.edu.cn/git-repo'

git config --global user.email "you@example.com"
git config --global user.name "Your Name"

安装jkd：sudo apt-get install openjdk-8-jdk

3.下载android源码
 
mkdir android_system
cd android_system/
repo init -u https://mirrors.tuna.tsinghua.edu.cn/git/AOSP/platform/manifest


android_system目录下生成了隐藏文件夹.repo

cd .repo/manifests
git branch -a
查看所有android的源码分支，选择对应的分支下载对应的android版本源码

rm -rf .repo/
repo init -u https://mirrors.tuna.tsinghua.edu.cn/git/AOSP/platform/manifest -b android-10.0.0_r9
repo sync


注意：根目录下只有20G的硬盘大小，下载的源码大概 有110G左右，不要放到根目录下。

编译环境依赖：
sudo apt-get install libx11-dev:i386 libreadline6-dev:i386 libgl1-mesa-dev g++-multilib 
sudo apt-get install -y git flex bison gperf build-essential libncurses5-dev:i386 
sudo apt-get install tofrodos python-markdown libxml2-utils xsltproc zlib1g-dev:i386 
sudo apt-get install dpkg-dev libsdl1.2-dev libesd0-dev
sudo apt-get install git-core gnupg flex bison gperf build-essential  
sudo apt-get install zip curl zlib1g-dev gcc-multilib g++-multilib 
sudo apt-get install libc6-dev-i386 
sudo apt-get install lib32ncurses5-dev x11proto-core-dev libx11-dev 
sudo apt-get install libgl1-mesa-dev libxml2-utils xsltproc unzip m4
sudo apt-get install lib32z-dev ccache
sudo apt install libncurses5
初始化编译环境
source build/envsetup.sh
选择编译目标
lunch aosp_arm64-eng
开始编译
make -j8



