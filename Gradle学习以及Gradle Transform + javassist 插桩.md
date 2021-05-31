#Gradle学习以及Gradle Transform + javassist 插桩

参考：

https://juejin.cn/post/6844903613911973901

https://www.jianshu.com/p/337e8d4f1817

https://juejin.cn/post/6937208620337610766#heading-0

Gradle进阶：

https://www.cnblogs.com/wxishang1991/p/5532006.html

https://www.infoq.cn/article/android-in-depth-gradle/

https://docs.gradle.org/current/dsl/org.gradle.api.Project.html

ubuntu系统下


1. 安装

sudo apt  install gradle  # version 4.4.1-10
gradle -v

可以看到当前安装的gradle版本


2. 新建build.gradle，文件命名必须是build.gradle，否则gradle命令找不到文件。

3.在build.gradle中新建task，执行task逻辑

task hello {
    doLast {
        println 'Hello world!'
    }
}

4.在build.gradle文件所在的位置打开命令行，命令行中执行命令：gradle -q hello


https://www.cnblogs.com/jiangxinnju/p/8229129.html

以上是不使用IDE时运行groovy文件的准备工作。如果使用IDE来运行groovy文件的话，我们是不需要特地安装gradle的，因为IDE已经帮我们安装好了。以android studio为例：
android studio是使用gradle来构建项目的，我们安装android stuido并且新建一个项目的时候，项目的根目录下已经存在了一个gradle/wrapper文件夹，下面有一个gradle-wrapper.jar，这个就是简化版的gradle，我们可以在命令行中通过./gradlew的方式运行groovy文件，跟gradle略微有点区别。我们还可以通过gradle/wrapper/gradle-wrapper.properties中指明使用哪个版本的gradle。下载完后会保存在android studio的安装目录下的plugins/gradle/lib中，并拷贝一份到项目的gradle/wrapper文件夹下供项目构建使用。

--------------

android studio里面虽然使用了Gradle来构建项目，但是它却不可以直接创建groovy项目。如果想创建groovy项目，我们需要先创建一个Java Library项目，然后修改module项目中的build.gradle。

```
//另外一种形式：apply plugin:'groovy'
plugins {
    id 'groovy'
}//引入 groovy 插件

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation localGroovy()//引入 Gradle 所带的 Groovy 库
}

//新建一个task
task haveFun{
    doLast{
        println "Module:lib  have fun!"
    }
}
//./gradlew build过程需要加这个语句，否则不执行task。
compileGroovy.finalizedBy haveFun

```

我们只需要在命令行中执行 ./gradlew build即可看见task的运行输入情况。或者直接输入./gradlew haveFun指定执行某个task。



脚本插件引入：

如果build.gradle过大，我们可以把部分逻辑放到其他的gradle文件中，build.gradle之间引用即可。

build.gradle
```
apply from: 'libs/test.gradle'
//这里可以直接使用testBuild这个task
task haveFun(dependsOn: testBuild){
    doLast{
        println "Module:lib  have fun!"
    }
}
//执行task
compileGroovy.finalizedBy haveFun
```

test.gradle
```
task testBuild{
    doLast{
        println "引用脚本插件"
    }
}
```


对象插件引入：

apply plugin:'groovy'就是一个对象插件引用。

准备工作：

我们来实现并引用一个插件对象，在实现这个之前，我们需要了解一下android studio根目录中的build.gradle结构。

项目的根目录下的build.gradle文件：

```
//声明gradle脚本自身所需要的资源，比如maven，jcenter仓库，第三方插件，本地插件等等。(只针对gradle脚本，不支持项目)
buildscript {
    //设置gradle脚本运行时，遇到依赖时可以去那些仓库查找并下载
    repositories {
        google()
        jcenter()
//        mavenCentral()//Maven中央仓库
        maven {
            //指定本地插件的地址
            url uri('./groovy_project/repo')
            //指定Maven远程仓库的地址
//            url "http://repo.mycompany.com/maven2"
        }
    }
    //gradle脚本运行时所需要的依赖
    dependencies {
        classpath "com.android.tools.build:gradle:4.1.2"
        classpath 'com.example.groovy_project:testplugin:1.0.0'
    }
}

//为项目中的所有module设置仓库
allprojects {
    repositories {
        google()
        jcenter()
    }
}
//./gradlew clean删除主目录下的build文件夹，相当于我们执行clean project操作
task clean(type: Delete) {
    delete rootProject.buildDir
}
```


这里的com.android.tools.build:gradle和gradle-wrapper有点不同，gradle-wrapper是用来构建项目用的(编译，打包等)，com.android.tools.build:gradle是指一个适合android开发的gradle插件集合，众多开发者开发gradle插件后会上传到maven等仓库，我们定义这个gradle版本是为了从仓库中下载指定的gradle插件到本地，以便gradle-wrapper构建项目的时候用到这些gradle插件。
gradle插件下载后会保存在根目录下的隐藏文件夹.gradle下：~\.gradle\caches\modules-2\files-2.1\com.android.tools.build


实现对象插件：

groovy中规定了要实现一个对象插件，必须实现Plugin<Project>接口。

```
public class TestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("这是自定义对象插件!");
    }
}
```

这个插件实现类所在的位置和引用这个插件的位置不同，那引用的方式就不同。

1. 如果TestPlugin实现类和引用方都在同一个build.gradle里面，那么引用方的使用如下：

```
apply plugin: CustomPluginInBuildGradle
//新建一个task
task haveFun(dependsOn: showTestPlugin){
    doLast{
        println "Module:lib  have fun!"
    }
}

class TestPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.task('showTestPlugin'){
            doLast {
                println("task in TestPlugin")
            }
        }
    }
}
```

apply plugin: CustomPluginInBuildGradle直接引用即可。

2. TestPlugin实现类在一个module里面，引用方在另一个module中直接引用。
   这个方式在android studio中无法模拟，有待验证。在IntelliJ中，新建一个buildSrc命名的module，该目录下的代码会在构建时自动编译打包，并被添加到buildScript中的classpath下，所以不需要任何额外的配置，就可以直接被其他模块的构建脚本所引用。 

3. TestPlugin实现类打包成jar形式上传到远程仓库，或者直接拷贝jar包到引用方所在的项目直接使用。

TestPlugin实现类所在的module中，在build.gradle中新增如下：

```
apply plugin: 'maven'
//提交仓库到本地目录
def version = "1.0.0";
def artifactId = "testplugin";
def groupId = "com.example.groovy_project";
uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: uri('./repo')) {
                pom.groupId = groupId
                pom.artifactId = artifactId
                pom.version = version
            }
        }
    }
}
```
uploadArchives是maven插件中的task，所以要使用前必须先引入maven依赖：apply plugin: 'maven'。如果要上传到私服或者maven服务器，修改url地址即可。

```
uploadArchives {
    repositories {
        mavenDeployer {
            pom.artifactId = '项目信息'
            pom.version = '版本信息'
            repository(url: '私服仓库地址') {
                authentication(userName: '账号', password: '密码')
            }
            snapshotRepository(url: '私服快照地址') {
               authentication(userName: '账号', password: '密码')
            }
        }
    }
}
```


在命令行中执行./gradlew uploadArchives命令，或者在Gradle面板中找到uploadArchives选项，在指定目录下生成repo文件夹，里面就包含了封装好的jar包。

引用方：

参照在准备工作中，项目根目录下的build.gradle的配置，指定本地插件的地址(就是uploadArchives中定义的地址)，并依赖具体的路径(生成的jar包的具体路径)。

```
buildscript {
    repositories {
        google()
        jcenter()
        maven {
            //指定本地插件的地址
            url uri('./groovy_project/repo')
        }
    }
    //gradle脚本运行时所需要的依赖
    dependencies {
        classpath "com.android.tools.build:gradle:4.1.2"
        classpath 'com.example.groovy_project:testplugin:1.0.0'
    }
}
```

接着在项目主module(app module)的build.gradle中引入gradle对象插件即可：
```
apply plugin: com.example.groovy_project.TestPlugin//应用插件
```


在gradle对象插件引入的同时，会调用TestPlugin中的apply()方法。


以上就是gradle对象插件的生成以及使用的过程。



gradle Transform就是使用了gradle对象插件的方式实现了插桩，在class文件转换成dex文件之前修改代码。

在gradle对象插件的基础上，我们只需要在apply()方法中注册我们自定义的Transform，在class文件转换成dex之前，gradle构建会先回调Transform中的方法，然后再进行转换。

TestPlugin.java
```
public class TestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("这是自定义插件!");
        //注册自定义的Transform，等待gradle打包apk过程中的回调
        project.getExtensions().findByType(BaseExtension.class)
                .registerTransform(new TestTransform());
    }
}
```

自定义的Transform
```

public class TestTransform extends Transform {

    //用于指明本Transform的名字，也是代表该Transform的task的名字
    @Override public String getName() {
        return "TestTransform";
    }

    //用于指明Transform的输入类型，可以作为输入过滤的手段。
    @Override public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    //用于指明Transform的作用域
    @Override public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    //是否增量编译
    @Override public boolean isIncremental() {
        return false;
    }

    @Override public void transform(TransformInvocation invocation) {
        System.out.println("TestTransform transform");
        for (TransformInput input : invocation.getInputs()) {
            //遍历jar文件 对jar不操作，但是要输出到out路径
            input.getJarInputs().parallelStream().forEach(jarInput -> {
                File src = jarInput.getFile();
                System.out.println("input.getJarInputs fielName:" + src.getName());
                File dst = invocation.getOutputProvider().getContentLocation(
                        jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(),
                        Format.JAR);
                try {
                    FileUtils.copyFile(src, dst);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            //遍历文件，在遍历过程中
            input.getDirectoryInputs().parallelStream().forEach(directoryInput -> {
                File src = directoryInput.getFile();
                System.out.println("input.getDirectoryInputs fielName:" + src.getName());
                File dst = invocation.getOutputProvider().getContentLocation(
                        directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY);
                try {
                    System.out.println("transform=====ctClass:"+src.getAbsolutePath()+"    " +dst.getAbsolutePath());
                    scanFilesAndInsertCode(src.getAbsolutePath());
                    FileUtils.copyDirectory(src, dst);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    //使用javassist解析calss字节码
    private void scanFilesAndInsertCode(String path) throws Exception {
        System.out.println("scanFilesAndInsertCode=====path:"+path);
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(path);//将当前路径加入类池,不然找不到这个类
        //修改已经存在的类
        CtClass ctClass = classPool.getCtClass("com.example.myapplication.gradle.PluginTestClass");
        if (ctClass == null) {
            return;
        }
        System.out.println("scanFilesAndInsertCode=====ctClass:"+ctClass.getName());
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }
        CtMethod ctMethod = ctClass.getDeclaredMethod("init");
        System.out.println("scanFilesAndInsertCode=====ctMethod:"+ctMethod.getName());

        String insetStr = "System.out.println(\"我是插入的代码\");";
        ctMethod.insertAfter(insetStr);//在方法末尾插入代码
        ctClass.writeFile(path);
        ctClass.detach();//释放
    }
}
```
