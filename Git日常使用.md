Git日常使用

+ ssh密钥

```
ssh-keygen -t rsa -C "邮箱"
```


+ git用户设置

```
git config --global user.name [姓名]
git config --global user.email [邮箱]

```


+ git commit配置

1. 新建commit的模板信息的文件
```
[Subject]:简要描述修改点
[Module]:DocumentsUI
[ID]:0000
[Issue]:
[Solution]:
[Risk]:NA
[Test Suggestions]:NA
```
2. 设置commit提交的信息模板

```
git config commit.template [commit模板的路径]
git config --global commit.template [commit模板的路径]

//查看当前的config配置
git config --list
```

3. 指定软件打开commit信息的模板，这里指定notepad++

```
git config --global core.editor "'D:\soft\Notepad++\notepad++.exe' -multiInst -notabbar -nosession -noPlugin '$*'"
```

其他：

在上一个commit的基础上再追加对commit信息的修改
```
git commit --amend
```



+ git拉取代码

本地新建分支并指向某个远程
```
git checkout -b [本地分支]  origin/[远程分支]
```


+ git提交代码

```
//不经过gerrit代码审核时的提交
git push origin master

//需要经过gerrit代码审核时的提交
git push origin HEAD:refs/for/master
```


+ git rebase操作

```
git checkout master
git pull
git checkout local
git rebase -i HEAD~2  //合并提交 --- 2表示合并两个
git rebase master
---->解决冲突--->
git rebase --continue
git checkout master
git merge local
git push
```