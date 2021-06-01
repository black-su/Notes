# 注解 #

* 注解的基本介绍
* Java中的注解
* android中的注解
* 三方框架中的注解

---

## 注解的基本介绍 ##

JDK1.5引进了注解的功能，详情见JDK1.5的新特性介绍:

[JDK1.5新特性](https://docs.oracle.com/javase/1.5.0/docs/relnotes/features.html)

[JDK1.6新特性](http://www.oracle.com/technetwork/java/javase/features-141434.html)

[JDK1.7新特性](http://www.oracle.com/technetwork/java/javase/jdk7-relnotes-418459.html)

[JDK1.8之后的新特性](https://www.oracle.com/java/technologies/javase-downloads.html)

以JDK1.8为例，点击官网的[API Documentation](https://docs.oracle.com/en/java/javase/15/docs/api/index.html)查看api文档，英文不好的可以查看[中文版的JDK1.6版本的API文档](https://tool.oschina.net/apidocs/apidoc?api=jdk-zh)，[JDK11中文版](https://www.apiref.com/java11-zh/java.base/module-summary.html)

注解分为两种：元注解和自定义注解

元注解只有四种：

元注解|说明
--- |  ---
Documented | 指示某一类型的注释将通过 javadoc 和类似的默认工具进行文档化。
Inherited | 指示注释类型被自动继承。
Retention |	指示注释类型的注释要保留多久。它的值通过枚举RetentionPolicy来定义。
Target | 指示注释类型所适用的程序元素的种类。它的值通过枚举ElementType来定义。

RetentionPolicy的值|说明
--- |  ---
CLASS | 编译器将把注释记录在类文件中，但在运行时 VM 不需要保留注释。
RUNTIME | 编译器将把注释记录在类文件中，在运行时 VM 将保留注释，因此可以反射性地读取。
SOURCE | 编译器要丢弃的注释。

ElementType的值|说明
--- |  ---
ANNOTATION_TYPE | 注释类型声明
CONSTRUCTOR | 构造方法声明
FIELD | 字段声明（包括枚举常量）
LOCAL_VARIABLE | 局部变量声明
METHOD | 方法声明
PACKAGE | 包声明
PARAMETER | 参数声明
TYPE | 类、接口（包括注释类型）或枚举声明


JDK定义了以上元注解的使用规则，如果我们需要使用注解，需要使用元注解去自定义注解。我们可以参考Override，Override是JDK为了实现重载的功能自定义的注解，它也是通过元注解来定义的。参考如下：
```
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```
Target元注解中的值，表示Override自定义注解只能使用在方法中。Retention元注解中的值表示在编译过程中这个自定义注解将会被编译器丢弃。

自定义了注解之后，该怎么使用呢？有两种方式：  
一种是在代码运行时通过反射获取自定义注释, 并做相应处理，这种比较耗费性能。
class.java提供了getAnnotations()获取所有当前的类注释。Method.java，Constructor.java，Field.java也分别提供了类似的获取当前相应类型的注释，详细可查看源码。
通过反射的方式，我们可以获取所需的注释并做处理。
```
    public Annotation[] getAnnotations() {
        return AnnotationParser.toArray(this.annotationData().annotations);
    }
```
一种是通过Java编译器的回调，在注释处理器中处理。


Java编译器：

[OpenJDK6源码](http://download.java.net/openjdk/jdk6/)
Java编译器的相关代码在openjdk-6-src-b27-26_oct_2012/langtools/src/share

我们在安装配置好jdk环境后，一般会在cmd窗口运行javac命令确保环境的正确。这个javac的命令就是由java编译器提供的，以可执行文件的方式存在（Window中的javac.exe，mac中的javac unix可执行文件），配置环境后可在任意目录下执行。
如果我们仔细看一眼javac的输出，会发现其中有-processor，-processorpath，-proc:等跟注解相关的命令。
```
用法: javac <options> <source files>
其中, 可能的选项包括:
  -g                         生成所有调试信息
  -g:none                    不生成任何调试信息
  -g:{lines,vars,source}     只生成某些调试信息
  -nowarn                    不生成任何警告
  -verbose                   输出有关编译器正在执行的操作的消息
  -deprecation               输出使用已过时的 API 的源位置
  -classpath <路径>            指定查找用户类文件和注释处理程序的位置
  -cp <路径>                   指定查找用户类文件和注释处理程序的位置
  -sourcepath <路径>           指定查找输入源文件的位置
  -bootclasspath <路径>        覆盖引导类文件的位置
  -extdirs <目录>              覆盖所安装扩展的位置
  -endorseddirs <目录>         覆盖签名的标准路径的位置
  -proc:{none,only}          控制是否执行注释处理和/或编译。
  -processor <class1>[,<class2>,<class3>...] 要运行的注释处理程序的名称; 绕过默认的搜索进程
  -processorpath <路径>        指定查找注释处理程序的位置
  -parameters                生成元数据以用于方法参数的反射
  -d <目录>                    指定放置生成的类文件的位置
  -s <目录>                    指定放置生成的源文件的位置
  -h <目录>                    指定放置生成的本机标头文件的位置
  -implicit:{none,class}     指定是否为隐式引用文件生成类文件
  -encoding <编码>             指定源文件使用的字符编码
  -source <发行版>              提供与指定发行版的源兼容性
  -target <发行版>              生成特定 VM 版本的类文件
  -profile <配置文件>            请确保使用的 API 在指定的配置文件中可用
  -version                   版本信息
  -help                      输出标准选项的提要
  -A关键字[=值]                  传递给注释处理程序的选项
  -X                         输出非标准选项的提要
  -J<标记>                     直接将 <标记> 传递给运行时系统
  -Werror                    出现警告时终止编译
  @<文件名>                     从文件读取选项和文件名
  ```

 Java编译器在运行时会收集注解，并对外提供处理注释的接口，如javac -processorpath <路径> 。如果我们是以命令行的形式编译java文件，可以用命令行指定注解处理的地方。但多数时候我们会习惯使用IDEA开发工具来工作，比如Android studio。
Android Studio用Gradle构建工具来完成依赖管理，编译，打包等工作，因此，如果我们要指定注解处理器，就不能用命令行的形式来指定。
Android Studio中添加注解处理器，在gradle 3.0.0版本之前用 android-apt插件形式，在gradle 3.0.0以及之后的高版本中，用annotationProcessor来指定注解处理器。
[参见android studio官网的介绍](https://developer.android.google.cn/studio/build/dependencies#annotation_processor)

  ```
  dependencies {
    // Adds libraries defining annotations to only the compile classpath.
    compileOnly 'com.google.dagger:dagger:version-number'
    // Adds the annotation processor dependency to the annotation processor classpath.
    annotationProcessor 'com.google.dagger:dagger-compiler:version-number'
}
  ```
以上annotationProcessor指定了由com.google.dagger插件来处理注释。当然，这里我们可以设置任何能处理注释的三方插件或者自定义module来处理。
一般我们项目中有很多个android module，如果多个module都需要收集自定义注释并指定注释处理器，那这些module都必须要要设置annotationProcessor。


application和android library依赖于android sdk，android sdk中包含了android系统的基础类库，相关的三方类库以及jdk的基础类库，但是不包含jdk中的编译器相关代码。
因此，注解处理器必须要在java library中实现才行。

注解处理器其实就是java编译器给javac -processorpath <路径> 所在的实现类的一个回调处理。至于java编译器是如何收集注解，如何回调的，还得详细研究编译器的相关源码。这里只关注如何处理自定义注解，如下：

  ```
  public class JavaAnnotationProcessor extends AbstractProcessor {

    private Filer mFiler; //文件相关的辅助类
    private Elements mElementUtils; //元素相关的辅助类
    private Messager mMessager; //日志相关的辅助类
    @Override
    public Set<String> getSupportedOptions() {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"getSupportedOptions");
        return super.getSupportedOptions();
    }

    /**
     * 定义本注释处理器所需要处理的注释
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"getSupportedAnnotationTypes");
        Set<String> types = new LinkedHashSet<>();
        types.add(ViewFind.class.getName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"getSupportedSourceVersion");
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        mMessager = processingEnvironment.getMessager();
        mMessager.printMessage(Diagnostic.Kind.WARNING,"init");

    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotationMirror, ExecutableElement executableElement, String s) {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"getCompletions");
        return super.getCompletions(element, annotationMirror, executableElement, s);
    }

    @Override
    protected synchronized boolean isInitialized() {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"isInitialized");
        return super.isInitialized();
    }

    /**
     * 编译期生成代理类，在运行时动态变更行为。达到修改当前存在的类的效果。
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"process:");
        //TODO: 1.解析注释所在的java建模元素标签。 2.利用JavaPoet生成java代码
        ......
        return false;
    }
}
  ```
以上是注释处理器的实现类，需要注意的有两点：
一是在getSupportedAnnotationTypes()中添加需要在这里进行处理的特定注释。
二是在process()中处理注释。注释的处理有两个步骤：解析注释所在的java建模元素标签，利用JavaPoet生成java代码。

JDK在处理注释的时候，把java类建模成html标签类的样子，并提供了相关的接口处理。参见[JDK中java compiler模块](https://www.apiref.com/java11-zh/index.html)中Package javax.lang.model.element

其元素标签类似于如下结构：
  ```
<PackageElement>//包
    <TypeElement>//类，接口
        <TypeParameterElement></TypeParameterElement>//类，接口的泛型参数
        <VariableElement></VariableElement>//成员变量
        <ExecutableElement>//函数，构造函数
            <TypeParameterElement></TypeParameterElement>//函数的泛型参数
            <VariableElement></VariableElement>//局部变量
        </ExecutableElement>
    </TypeElement>
</PackageElement>
  ```
通过JDK的回调方法process()中提供的RoundEnvironment来操作此元素标签结构。
RoundEnvironment的getElementsAnnotatedWith()可获取当前注释所在的Element。
如果当前注释所在的位置为类，则返回TypeElement，如果注释所在的位置为函数，则返回ExecutableElement，依次类推。
通过getEnclosingElement()依次一层层的获取其外层的Element，直至到PackageElement。
当前PackageElement再次通过getEnclosingElement()获取到的将是一个null


JavaPoet用于生成java代码，详细用法参见github的官网说明，使用方式介绍得很详尽:[JavaPoet的github地址](https://github.com/square/javapoet)

编译器收集注释后，我们可以通过解析注释，生成特定的代码，但也是仅限于生成一个新的.java文件，它并不能改变已有的.java文件。如果我们需要依靠注释来达到修改已有的.java文件，就必须要通过代理模式来实现了。
比如，实现android中的findViewById()功能：

一.定义接口：
  ```
public interface IProxy<T> {
    /**
     *
     * @param target 所在的类
     * @param root 查找 View 的地方
     */
    public void inject(final T target, View root);
}
  ```
二.通过反射机制获取注释处理生成的代理类的对象，并调用这个代理类的方法实现findViewById()功能
  ```
  public class IProxyUtil {
    //生成代理类的后缀名
    public static final String SUFFIX = "$Proxy";
    public static void findView(Object target, View root){
        try {
            //通过反射获取processor帮忙生成的代理类对象，并调用代理类中的方法完成findView操作
            Class<?> targetClass = target.getClass();
            Class<?> proxyClass = Class.forName(targetClass.getName() + ViewFind.class.getSimpleName() + SUFFIX);
            IProxy proxy = (IProxy) proxyClass.newInstance();
            proxy.inject(target, root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
  }
  
  public class MainActivity extends AppCompatActivity {

    @ViewFind(R.id.name)
    public TextView name;

    @ViewFind(R.id.age)
    public TextView age;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IProxyUtil.findView(this,getWindow().getDecorView());
        Toast.makeText(this,name.getText().toString()+age.getText().toString(),Toast.LENGTH_SHORT).show();
    }

}
  ```
三. 通过注释处理器生成代理类

  ```
      @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMessager.printMessage(Diagnostic.Kind.WARNING,"process:");
        try {

            for (TypeElement typeElement : set) {
                //====================ViewFind============================
                if(ViewFind.class.getName().equals(typeElement.getQualifiedName().toString())) {
                    ProxyClassUtil.generateViewFindProxy(roundEnvironment);
                    Map<String, JavaFile> javaFileMap = ProxyClassUtil.getJavaFileMap();
                    for (String key : javaFileMap.keySet()) {
                        mMessager.printMessage(Diagnostic.Kind.WARNING, "process:" + key);
                        javaFileMap.get(key).writeTo(mFiler);//生成代理类文件
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
  
    /**
     * 用于生成代理类
     */
    public static void generateViewFindProxy(RoundEnvironment roundEnvironment) throws Exception{
        javaFileMap.clear();

        for (Element element : roundEnvironment.getElementsAnnotatedWith(ViewFind.class)){
            //类元素
            TypeElement classElement = (TypeElement) element.getEnclosingElement();
            //类的全限定类名
            String qualifiedName = classElement.getQualifiedName().toString();
            classMap.put(qualifiedName,classElement);//确保同一个类只生成一个代理类

            String fieldName = element.getSimpleName().toString();//变量名
            TypeMirror fieldMirror = element.asType();//变量类型
            ViewFind viewFind = element.getAnnotation(ViewFind.class);
            int value = viewFind.value();//注释中定义的值
            fieldMap.put(fieldName,value);
            System.out.println("qualifiedName:"+qualifiedName+"       fieldName:"+fieldName);

        }

        for (String className : classMap.keySet()){
            TypeElement mTypeElement = classMap.get(className);
            //生成public void inject(final T target, View root)方法
            MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("inject")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(TypeName.get(mTypeElement.asType()), "target", Modifier.FINAL)
                    .addParameter(VIEW, "root");

            //生成findViewById语句
            for (String fieldName : fieldMap.keySet()){
                injectMethodBuilder.addStatement("target."+fieldName+"= root.findViewById("+fieldMap.get(fieldName)+")");
            }
            // 添加以$$Proxy为后缀的类
            TypeSpec finderClass = TypeSpec.classBuilder(mTypeElement.getSimpleName() + ViewFind.class.getSimpleName() +  SUFFIX)
                    .addModifiers(Modifier.PUBLIC)
                    //添加父接口
                    .addSuperinterface(ParameterizedTypeName.get(IPROXY, TypeName.get(mTypeElement.asType())))
                    .addMethod(injectMethodBuilder.build())
                    .build();

            //类的全限定类名
            String qualifiedName = mTypeElement.getQualifiedName().toString();
            String packageName = ((PackageElement)mTypeElement.getEnclosingElement()).getQualifiedName().toString();
            JavaFile javaFile = JavaFile.builder(packageName, finderClass).build();
            javaFileMap.put(qualifiedName,javaFile);
        }

    }
  ```

四.查看注释处理器生成的代理类文件
注释处理器的代码写好后，重新Rebuild Project就可以，生成的代理类文件一般在build/generated/ap_generated_sources/debug/out下面，或者在build/outputs下面。
AnnotationProject/app/build/generated/ap_generated_sources/debug/out/com/example/annotationproject/MainActivityViewFind$Proxy.java
  ```
  package com.example.annotationproject;

import android.view.View;
import com.example.annotationproject.proxy.IProxy;
import java.lang.Override;

public class MainActivityViewFind$Proxy implements IProxy<MainActivity> {
  @Override
  public void inject(final MainActivity target, View root) {
    target.name= root.findViewById(2131230948);
    target.age= root.findViewById(2131230788);
  }
}
  ```

## android中注释相关的三方框架 ##

android中有注释处理器的三方框架主要有：EventBus3.0，Retrofit2.0，dagger2等。


EventBus3.0：
EventBus在3.0版本的时候加入了注释处理器的功能，用户可以自行选择是否使用注释处理器。
3.0之前或者没有选择使用注释处理器的仍旧使用反射处理注释。
详细见[官网介绍](https://greenrobot.org/eventbus/documentation/subscriber-index/)

我们看下官网介绍的build.gradle配置：
  ```
  dependencies {
    def eventbus_version = '3.2.0'
    implementation "org.greenrobot:eventbus:$eventbus_version"
    annotationProcessor "org.greenrobot:eventbus-annotation-processor:$eventbus_version"
}
  ```
注释处理器指定为org.greenrobot:eventbus-annotation-processor。我们把[EventBus3.0源码](https://github.com/greenrobot/EventBus)下载下来看看它的注释处理器eventbus-annotation-processor。

EventBus3.0源码的目录有6个module，除了主要的EventBus，EventBusAnnotationProcessor两个module外，剩下的四个module（EventBusPerformance，EventBusTest，EventBusTestJava，EventBusTestSubscriberInJar）都是测试用。

EventBus module中


EventBusAnnotationProcessor module依赖于EventBus module，打包名称为eventbus-annotation-processor，版本是3.2.0。
  ```
  apply plugin: 'java'

archivesBaseName = 'eventbus-annotation-processor'
group = 'org.greenrobot'
version = '3.2.0'

sourceCompatibility = 1.7

dependencies {
    implementation project(':eventbus')
    implementation 'de.greenrobot:java-`common`:2.3.1'

    // Generates the required META-INF descriptor to make the processor incremental.
    def incap = '0.2'
    compileOnly "net.ltgt.gradle.incap:incap:$incap"
    annotationProcessor "net.ltgt.gradle.incap:incap-processor:$incap"
}
......
  ```

此外，EventBusAnnotationProcessor module中的注释处理交给net.ltgt.gradle.incap:incap-processor:$incap来完成。EventBusAnnotationProcessor本来就是一个注释处理器，为什么它还自己还定义另外一个注释处理器来处理自己的注释。  
我们查看EventBusAnnotationProcessor module中唯一的类EventBusAnnotationProcessor.java。可以看到类注释@IncrementalAnnotationProcessor(AGGREGATING)，这个注释的处理在net.ltgt.gradle.incap:incap-processor:$incap中，这个注释有什么用？现在暂时没看明白，后续有空再回来看看。

  ```
  @SupportedAnnotationTypes("org.greenrobot.eventbus.Subscribe")
@SupportedOptions(value = {"eventBusIndex", "verbose"})
@IncrementalAnnotationProcessor(AGGREGATING)
public class EventBusAnnotationProcessor extends AbstractProcessor {
......
}
  ```
EventBusAnnotationProcessor module中的process()过程主要是把收集的