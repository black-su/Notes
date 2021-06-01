##  Android Resource 资源文件 ##
 
 
以下操作均在Android Studio 4.1.1版本上


   在android项目中新建资源文件，一般推荐的是new -> Android Resource file操作，当我们选定Resource type的时候，android studio会帮我们定义好Root element，一般我们不需要改变Root element，只需要定义一个名字即可，这样就完成了一个标准的资源文件创建。资源文件会被放在指定的目录下。
Resource type的类型有如下几种，每个type类型都有自己的专属目录：

| Resource type     | Root element |   Directory name     |   describe     |
| --------   | -----  | ----  | ----  |
| Animation | 默认set为根标签,其他跟标签有sacle,rotate,translate,alpha,layoutAnimation,gridLayoutAnimation | anim |
| Animator | 默认set为根标签,其他跟标签有objectAnimator,animator  | animator |
| Color   |    selector    |  color  |
| Drawable   |   selector    |  drawable  |
| Font   |    font-family    |  font  |
| Layout   |   androidx.constraintlayout.widget.ConstraintLayout    |  layout  |
| Menu   |    menu    |  menu  |
| Navigation   |    navigation   |  navigation  |Google I/O 2018 上新出现了一个导航组件
| Transition   |    transitionManager    |  transition  |
| Values   |    resources    |  values  |
| XML   |    PreferenceScreen    |  xml  |

如果我们熟悉哪个目录下可以放哪些type类型的文件，也可以自己先创建指定的目录，然后再创建具体类型的文件。可以new -> Android Resource Directory，然后再new -> Android Resource file。
只不过这个操作下，多了这三个文件，这三个文件的文件类型不确定
mipmap
raw
interpolator

---

 1.  Animation



* layoutAnimation标签
layoutAnimation用于给ViewGroup设置进场动画。

[参考一](https://blog.csdn.net/s13383754499/article/details/85243075)
[参考二](https://blog.csdn.net/superbigcupid/article/details/52173691)

xml
```
<?xml version="1.0" encoding="utf-8"?>
<layoutAnimation xmlns:android="http://schemas.android.com/apk/res/android"
    android:animation="@anim/translate_pivot_percent_inself"
    android:animationOrder="normal"
    android:delay="0"
    >
</layoutAnimation>
```
用法：
```

```

* gridLayoutAnimation


 2. Animator
 * set标签
 见Animation中的set标签介绍
 * animator标签
 
 * objectAnimator标签
    

 3. Color
 4. Drawable
 5. Font
 6. Layout
 7. Menu
 8. Navigation
 9. Transition
 10. Values
 11. XML
 12. mipmap
 13. raw
 14. interpolator