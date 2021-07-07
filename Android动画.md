# Android动画 #

Android的动画的参考官方介绍：
https://developer.android.google.cn/guide/topics/resources/animation-resource#java

总结一下：
Android动画具体分为属性动画，视图动画（补间动画，帧动画）。
属性动画：可以为控件中的属性设置动画（颜色，大小，位置等等）。如果在xml中设置的话有三个标签set、objectAnimator 或 valueAnimator。其中set是一个集合，可以内置一个或者多个objectAnimator 或 valueAnimator的集合。如果是在代码中设置属性动画，以上三个标签分别对应了AnimatorSet，ValueAnimator、ObjectAnimator。
补间动画：可以为控件设置旋转、淡出、移动和拉伸四种动画。如果在xml中设置的话有，对应有五种标签alpha、scale、translate、rotate、set，其中set是一个集合，可以内置一个或者多个其他的标签。如果是在代码中设置补间动画，以上五个标签分别对应了AlphaAnimation，ScaleAnimation，TranslateAnimation，RotateAnimation。

帧动画：帧动画的效果类似于播放幻灯片，一张张图片轮换播放。这种动画的实质其实是Drawable，所以这种动画的XML定义方式文件一般放在res/drawable/目录下。如果在xml中设置的话，其标签为animation-list，如果在代码中设置的话，对应的是AnimationDrawable。

属性动画的资源位置：res/animator/filename.xml
补间动画的资源位置：res/anim/filename.xml
帧动画的资源位置：res/drawable/filename.xml

 1. 属性动画
 
官方文档：
 [Google API : ObjectAnimator]{https://developer.android.google.cn/reference/android/animation/ObjectAnimator}
[Google API : Property Animation Overview]{https://developer.android.google.cn/guide/topics/graphics/prop-animation#object-animator}

[博客：属性动画]{https://www.cnblogs.com/wondertwo/p/5312482.html}

总结一下：
属性动画与补间动画相比，补间动画是针对视图外观的动画实现，动画被应用时外观改变但视图的触发点不会发生变化，还是在原来定义的位置。而属性动画的触发点会随着动画的执行而改变。除此之外，属性动画除了旋转、淡出、移动和拉伸四种动画外，还可以设置颜色变化，甚至可以自定义属性的变化。属性动画提供了更精确复杂的动画控制，比如动画的速率，轨迹等等。

属性动画中两个比较重要的概念：

动画速率控制：插值器TimeInterpolator(Interpolator)
Google提供的常见插值器
| 属性     | 用法 |  
| --------   | -----  | 
|AccelerateDecelerateInterpolator| 在动画开始与结束的地方速率改变比较慢，在中间的时候加速|
|AccelerateInterpolator|  在动画开始的地方速率改变比较慢，然后开始加速|
|AnticipateInterpolator| 开始的时候向后然后向前甩|
|AnticipateOvershootInterpolator| 开始的时候向后然后向前甩一定值后返回最后的值|
|BounceInterpolator|   动画结束的时候弹起|
|CycleInterpolator| 动画循环播放特定的次数，速率改变沿着正弦曲线|
|DecelerateInterpolator| 在动画开始的地方快然后慢|
|LinearInterpolator|   以常量速率改变|
|OvershootInterpolator|    向前甩一定值后再回到原来位置|

AnticipateOvershootInterpolator源码示例，getInterpolation()方法中就是定义速率的公式，如果google提供的插值器不满足需求，可以自定义。
```
public class AccelerateDecelerateInterpolator extends BaseInterpolator
        implements NativeInterpolator {
    public AccelerateDecelerateInterpolator() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public AccelerateDecelerateInterpolator(Context context, AttributeSet attrs) {
    }

    public float getInterpolation(float input) {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

    /** @hide */
    @Override
    public long createNativeInterpolator() {
        return NativeInterpolatorFactory.createAccelerateDecelerateInterpolator();
    }
}
```

动画的评估程序（估值器）：TypeEvaluator（这个TypeEvaluator我理解为动画在屏幕上绘制的每一帧的属性值计算，可以查看FloatKeyframeSet.getAnimatedValue()源码实现的流程，每一帧的属性值来跟动画有关。TypeEvaluator和TimeInterpolator分开设计的原因是：TimeInterpolator单独设置动画速率，我们可以通过调用TypeEvaluator.evaluate(float fraction, Number startValue, Number endValue)，在不同的动画阶段（startValue ～ endValue），执行不同的速率（fraction），这样达到一个动画事件内实现多种不同的动画路径效果。

```
    @Override
    public float getFloatValue(float fraction) {
        if (fraction <= 0f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(0);
            final FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(1);
            float prevValue = prevKeyframe.getFloatValue();
            float nextValue = nextKeyframe.getFloatValue();
            float prevFraction = prevKeyframe.getFraction();
            float nextFraction = nextKeyframe.getFraction();
            final TimeInterpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
        } else if (fraction >= 1f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(mNumKeyframes - 2);
            final FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(mNumKeyframes - 1);
            float prevValue = prevKeyframe.getFloatValue();
            float nextValue = nextKeyframe.getFloatValue();
            float prevFraction = prevKeyframe.getFraction();
            float nextFraction = nextKeyframe.getFraction();
            final TimeInterpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            float intervalFraction = (fraction - prevFraction) / (nextFraction - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
        }
        FloatKeyframe prevKeyframe = (FloatKeyframe) mKeyframes.get(0);
        for (int i = 1; i < mNumKeyframes; ++i) {
            FloatKeyframe nextKeyframe = (FloatKeyframe) mKeyframes.get(i);
            if (fraction < nextKeyframe.getFraction()) {
                final TimeInterpolator interpolator = nextKeyframe.getInterpolator();
                float intervalFraction = (fraction - prevKeyframe.getFraction()) /
                    (nextKeyframe.getFraction() - prevKeyframe.getFraction());
                float prevValue = prevKeyframe.getFloatValue();
                float nextValue = nextKeyframe.getFloatValue();
                // Apply interpolator on the proportional duration.
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return mEvaluator == null ?
                        prevValue + intervalFraction * (nextValue - prevValue) :
                        ((Number)mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).
                            floatValue();
            }
            prevKeyframe = nextKeyframe;
        }
        // shouldn't get here
        return ((Number)mKeyframes.get(mNumKeyframes - 1).getValue()).floatValue();
    }
```

```
    public class FloatEvaluator implements TypeEvaluator {

        public Object evaluate(float fraction, Object startValue, Object endValue) {
            float startFloat = ((Number) startValue).floatValue();
            return startFloat + fraction * (((Number) endValue).floatValue() - startFloat);
        }
    }
```

* ObjectAnimator，objectAnimator标签

根据动画的要求不同，ObjectAnimator属性动画的使用有以下几种用法。

动画中只有一个属性变化，速率统一

```
<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="2000"
    android:valueFrom="1"
    android:valueTo="0"
    android:valueType="floatType"
    android:propertyName="alpha"
    android:startOffset="100"
    android:repeatCount="infinite"
    android:repeatMode="restart"
    android:interpolator="@android:anim/accelerate_interpolator"
    ></objectAnimator>
```

动画中有多个动画，但是多个动画的动画时间，动画执行次数都一样，可用propertyValuesHolder标签
```
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="2000"
    android:startOffset="100"
    android:repeatCount="infinite"
    android:repeatMode="restart"
    android:interpolator="@android:anim/accelerate_interpolator"
    >

    <propertyValuesHolder
        android:valueTo="0.5"
        android:valueFrom="1"
        android:valueType="floatType"
        android:propertyName="alpha"
        >
    </propertyValuesHolder>

    <propertyValuesHolder
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="floatType"
        android:propertyName="translationX"
        />
</objectAnimator>
```
动画中的属性变化复杂，动画执行的这段时间内，每一小段时间的动画执行要求不一样（速率），可用keyframe标签。fraction的值是0~1（动画的已执行时间/动画执行总时间），可以为每一帧之间的动画设置不一样的速率。
```
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="2000"
    android:startOffset="100"
    android:repeatCount="infinite"
    android:repeatMode="restart"
    android:interpolator="@android:anim/accelerate_interpolator"
    >
    <propertyValuesHolder
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="floatType"
        android:propertyName="translationY"
        >
        <keyframe android:fraction="0.8" android:value="100" />
        <keyframe android:fraction="0.2"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="100" />
        <keyframe android:fraction="1"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="150" />
        <keyframe android:fraction="0.2"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="200" />
    </propertyValuesHolder>
</objectAnimator>
```
除此之外，objectAnimator标签还有一个特别的用法：不同于上面的三种用法，我们可以使用android:propertyXName，android:propertyYName，android:pathData搭配使用。（pathData不能搭配propertyName）。利用矢量图标的格式为动画设置执行路径。[Android vector标签 PathData 画图超详解]{https://www.cnblogs.com/yuhanghzsd/p/5466846.html}
```
    <objectAnimator
        android:duration="2000"
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="floatType"
        android:startOffset="100"
        android:propertyXName="translationX"
        android:propertyYName="translationY"
        android:pathData="M10,0 L10,40 40,40"
        android:repeatMode="restart"
        android:repeatCount="infinite"
        android:interpolator="@android:anim/accelerate_interpolator"
        />
```

java代码中引用xml
```
    Animator animator = (Animator) AnimatorInflater.loadAnimator(myContext,
        R.animator.property_animator);
    set.setTarget(myObject);
    set.start();
```

java代码中设置ObjectAnimator：ObjectAnimator中提供了很多不同的方法来或者ObjectAnimator对象，可以根据类型的不同，使用ofFloat,ofArgb,ofMultiInt,ofObject,ofPropertyValuesHolder来创建ObjectAnimator对象，各个类型的方法中还可以根据参数（Path，TypeConverter，TypeEvaluator等）的不同来创建。可自行查看源码。
```
    ObjectAnimator animation = ObjectAnimator.ofFloat(textView, "translationX", 100f);
    animation.setDuration(1000);
    animation.start();
```

| 属性     | 用法 |  
| --------   | -----  | 
|android:duration|动画执行时间|
|android:valueTo|动画结束时的属性值|
|android:valueFrom|动画开始时的属性值|
|android:valueType|动画属性值的类型，有floatType,colorType,intType,pathType四种类型|
|android:startOffset|动画的延时启动|
|android:propertyName|动画属性名字，基本的属性有：translationX 和 translationY：这些属性用于控制视图所在的位置，值为视图的布局容器所设置的左侧坐标和顶部坐标的增量。rotation、rotationX 和 rotationY：这些属性用于控制视图围绕轴心点进行的 2D（ 属性）和 3D 旋转。scaleX 和 scaleY：这些属性用于控制视图围绕其轴心点进行的 2D 缩放。
pivotX 和 pivotY：这些属性用于控制旋转和缩放转换所围绕的轴心点的位置。默认情况下，轴心点位于对象的中心。x 和y：这些是简单的实用属性，用于描述视图在容器中的最终位置，值分别为左侧值与 translationX 值的和以及顶部值与 translationY 值的和。alpha：表示视图的 Alpha 透明度。此值默认为 1（不透明），值为 0 则表示完全透明（不可见）。|
|android:propertyXName|动画属性名字，参见propertyName，搭配pathData使用|
|android:propertyYName|动画属性名字，参见propertyName，搭配pathData使用|
|android:pathData|矢量图的格式路径，参照："M10,0 L10,40 40,40"|
|android:repeatMode|动画的重复模式，有restart,reverse两种模式|
|android:repeatCount|动画的重复次数，可以设置具体次数，无限循环使用infinite|
|android:interpolator|动画的速率设置，如："@android:anim/accelerate_interpolator"|


* ValueAnimator，animator标签
ValueAnimator是ObjectAnimator的基类，ObjectAnimator标签中的属性大多数都来源于ValueAnimator。因此ValueAnimator的标签属性与ObjectAnimator没有太大的不同。
```
<animator xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="2000"
    android:repeatMode="restart"
    android:repeatCount="infinite"
    android:startOffset="100"
    android:valueType="intType"
    android:valueFrom="0"
    android:valueTo="200"
    android:interpolator="@android:anim/accelerate_interpolator"
    >
    <propertyValuesHolder
        android:valueTo="0.5"
        android:valueFrom="1"
        android:valueType="floatType"
        android:propertyName="alpha"
        >
    </propertyValuesHolder>
    <propertyValuesHolder
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="intType"
        android:propertyName="rotationY"
        >
        <keyframe android:fraction="0.2" android:value="10" />
        <keyframe android:fraction="0.5"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="100" />
        <keyframe android:fraction="0.8"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="150" />
        <keyframe android:fraction="1"
            android:interpolator="@android:anim/accelerate_interpolator"
            android:value="200" />
    </propertyValuesHolder>
</animator>
```
唯一的不同的地方就是animator标签中没有propertyName属性值。因为没有属性值，所以在使用的时候如果没有设置属性变化，那么这个动画执行的时候不会有动画效果。要想实现动画效果，就必须监听动画并设置属性变化，如下：
```
    ((ValueAnimator)animator).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            PropertyValuesHolder[] propertyValuesHolders = animation.getValues();
            for (PropertyValuesHolder propertyValuesHolder: propertyValuesHolders){
                String propertyName = propertyValuesHolder.getPropertyName();
                Log.v(TAG,propertyName+"===="+animation.getAnimatedFraction()+"  "+animation.getAnimatedValue(propertyName)+"  "+animation.getInterpolator());
            }
            imageView.setAlpha((float) animation.getAnimatedValue("alpha"));
            imageView.setTranslationX((float)animation.getAnimatedValue("translationX"));
            imageView.setRotationY((Integer)animation.getAnimatedValue("rotationY"));
        }
    });
```

* AnimatorSet，set标签

objectAnimator标签里面可以设置多个属性，多个属性共用一个动画执行时间。而set标签可以设置多个objectAnimator标签来实现各复杂的动画效果，多个属性执行不同的动画时间。set标签提供了android:ordering属性来设置执行的顺序，android:ordering有两个不同的值：sequentially和together,多个动画属性按顺序执行，还是一起执行。

```
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:ordering="sequentially"
    >
    <objectAnimator
        android:duration="2000"
        android:valueFrom="0"
        android:valueTo="1"
        android:valueType="floatType"
        android:propertyName="alpha"
        android:startOffset="100"
        android:interpolator="@android:anim/accelerate_interpolator"
        />
    <objectAnimator
        android:duration="2000"
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="pathType"
        android:startOffset="100"
        android:propertyXName="translationX"
        android:propertyYName="translationY"
        android:pathData="M10,0 L10,40 40,40"
        android:repeatMode="reverse"
        android:repeatCount="infinite"
        android:interpolator="@android:anim/accelerate_interpolator"
        />
    <objectAnimator
        android:duration="2000"
        android:valueTo="200"
        android:valueFrom="0"
        android:valueType="floatType"
        android:propertyName="translationY"
        android:startOffset="100"
        />
</set>
```
java代码中引用xml
```
    Animator animator = (Animator) AnimatorInflater.loadAnimator(myContext,
        R.animator.property_animator);
    set.setTarget(myObject);
    set.start();
```

java代码中设置AnimatorSet
```
    AnimatorSet bouncer = new AnimatorSet();
    bouncer.play(bounceAnim).before(squashAnim1);
    bouncer.play(squashAnim1).with(squashAnim2);
    bouncer.play(squashAnim1).with(stretchAnim1);
    bouncer.play(squashAnim1).with(stretchAnim2);
    bouncer.play(bounceBackAnim).after(stretchAnim2);
    ValueAnimator fadeAnim = ObjectAnimator.ofFloat(newBall, "alpha", 1f, 0f);
    fadeAnim.setDuration(250);
    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.play(bouncer).before(fadeAnim);
    animatorSet.start();
```

* 其他属性动画
View.java中定义了ViewPropertyAnimator。ViewPropertyAnimator提供了很多属性动画的操作，使得属性动画的使用更加方便：
```
myView.animate().x(50f).y(100f);
```
StateListAnimator
指定的视图状态（例如“按下”或“聚焦”）发生更改，就会调用该动画。
```
    <Button android:stateListAnimator="@xml/animate_scale"
            ... />
```
animate_scale
```
<?xml version="1.0" encoding="utf-8"?>
    <selector xmlns:android="http://schemas.android.com/apk/res/android">
        <!-- the pressed state; increase x and y size to 150% -->
        <item android:state_pressed="true">
            <set>
                <objectAnimator android:propertyName="scaleX"
                    android:duration="@android:integer/config_shortAnimTime"
                    android:valueTo="1.5"
                    android:valueType="floatType"/>
                <objectAnimator android:propertyName="scaleY"
                    android:duration="@android:integer/config_shortAnimTime"
                    android:valueTo="1.5"
                    android:valueType="floatType"/>
            </set>
        </item>
        <!-- the default, non-pressed state; set x and y size to 100% -->
        <item android:state_pressed="false">
            <set>
                <objectAnimator android:propertyName="scaleX"
                    android:duration="@android:integer/config_shortAnimTime"
                    android:valueTo="1"
                    android:valueType="floatType"/>
                <objectAnimator android:propertyName="scaleY"
                    android:duration="@android:integer/config_shortAnimTime"
                    android:valueTo="1"
                    android:valueType="floatType"/>
            </set>
        </item>
    </selector>
```

 2. 补间动画

补间动画的基础属性，alpha、scale、translate、rotate、set五种标签都可以设置：
| 属性     | 用法 |  
| --------   | -----  | 
|android:detachWallpaper|	窗口动画的特殊选项：如果此窗口位于墙纸的顶部，请不要使用该墙纸设置动画。 
|android:duration|	动画运行的时间（以毫秒为单位）。 
|android:fillAfter|	设置为true时，动画结束后将应用动画变换。 
|android:fillBefore|	当设置为true或未将fillEnabled设置为true时，将在开始动画之前应用动画转换。 
|android:fillEnabled|	设置为true时，将考虑fillBefore的值。 
|android:interpolator|	定义用于平滑动画运动的插值器。 
|android:repeatCount|	定义动画应重复多少次。 
|android:repeatMode|	定义当动画到达终点且重复计数大于0或无限时的动画行为。 
|android:startOffset|	一旦达到开始时间，动画运行之前的毫秒数延迟。 
|android:zAdjustment|	允许在动画期间调整要动画的内容的Z顺序。 



* ScaleAnimation，拉伸动画，scale标签
 
 xml
```
 <?xml version="1.0" encoding="utf-8"?>
<scale xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromXScale="0"
    android:fromYScale="0"
    android:toXScale="2"
    android:toYScale="2"
    android:pivotX="0"
    android:pivotY="0"
    >
</scale>
```
xml引用：
```
Animation animation = AnimationUtils.loadAnimation(this,resourceId);
animation.setDuration(3000);
animation.setRepeatCount(Animation.INFINITE);       
imageView.startAnimation(animation);
```
java代码使用：
```
//pivot:Animation.ABSOLUTE,Animation.RELATIVE_TO_SELF,Animation.RELATIVE_TO_PARENT
ScaleAnimation scaleAnimation = new ScaleAnimation(0,0,2,2,Animation.RELATIVE_TO_SELF,Animation.RELATIVE_TO_SELF);
scaleAnimation.setDuration(2000);
scaleAnimation.setFillAfter(false);
imageView.startAnimation(scaleAnimation);
```

| 属性     | 用法 |  
| --------   | -----  | 
| android:fromXScale     | 动画开始时控件的X方向的缩放比例，1代表原比例，0代表最小缩放比例 |  
| android:fromYScale     | 动画开始时控件的Y方向缩放比例，1代表原比例，0代表最小缩放比例 |  
| android:toXScale     | 动画结束时控件的X方向缩放比例，1代表原比例，0代表最小缩放比例 |  
| android:toYScale     | 动画结束时控件的Y方向缩放比例，1代表原比例，0代表最小缩放比例 |
| android:pivotX     | 这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p。 | 
| android:pivotY     | 这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p。 | 
缩放动画需要一个参考点，缩放时按照参考点为中心进行缩放。这个参考点就是根据android:pivotX，android:pivotY来设置的。当它的值是数值时（比如50），那这个参考点在整个屏幕中的坐标就是（left+50，top+50），left和top表示控件的左上角坐标。当它的值是百分比时（比如50%），那这个参考点在整个屏幕中的坐标就是（left+width*50%,top+height*50%）,width和height分别为控件的宽高。当它的值时百分比p时（比如50%p），那这个参考点在整个屏幕中的坐标就是（left+width*50%,top+height*50%）,width和height分别为屏幕的宽高。




* TranslateAnimation，translate标签，移动动画
平移动画
xml
```
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromXDelta="-50"
    android:fromYDelta="-50"
    android:toXDelta="50"
    android:toYDelta="100"
    >
</translate>
```
xml引用：
```
Animation animation = AnimationUtils.loadAnimation(this,resourceId);
animation.setDuration(3000);
animation.setRepeatCount(Animation.INFINITE);       
imageView.startAnimation(animation);
```
java代码使用：
```
TranslateAnimation translateAnimation = new TranslateAnimation(0,0,100,100);
translateAnimation.setDuration(2000);
translateAnimation.setFillAfter(false);
imageView.startAnimation(translateAnimation);
```

| 属性     | 用法 |  
| --------   | -----  | 
| android:fromXDelta     | 动画开始时所在X轴方向的位置，这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p |  
| android:fromYDelta     | 动画开始时所在Y轴方向的位置，这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p |  
| android:toXDelta     | 动画结束时所在X轴方向的位置， 这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p |  
| android:toYDelta     | 动画结束时所在Y轴方向的位置，这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p |
平移动画可以设置数值，也可以设置比例来决定平移的距离，正负表示方向。当它的值是数值时（比如50），那它的平移距离是50。当它的值是百分比时（比如50%），那它的平移距离是width(height)*50%,width和height分别为控件的宽高。当它的值时百分比p时（比如50%p），那它的平移距离是width(htight)*50%,width和height分别为屏幕的宽高。



* RotateAnimation，rotate标签，旋转动画

xml
```
<?xml version="1.0" encoding="utf-8"?>
<rotate xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromDegrees="0"
    android:toDegrees="360"
    android:pivotX="50%"
    android:pivotY="50%"
    >
</rotate>
```
xml引用：
```
Animation animation = AnimationUtils.loadAnimation(this,resourceId);
animation.setDuration(3000);
animation.setRepeatCount(Animation.INFINITE);       
imageView.startAnimation(animation);
```
java代码使用：
```
//pivot:Animation.ABSOLUTE,Animation.RELATIVE_TO_SELF,Animation.RELATIVE_TO_PARENT
RotateAnimation rotateAnimation = new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,Animation.RELATIVE_TO_SELF);
rotateAnimation.setDuration(2000);
rotateAnimation.setFillAfter(false);
imageView.startAnimation(rotateAnimation);
```

| 属性     | 用法 |  
| --------   | -----  | 
| android:fromDegrees     |动画开始时旋转的角度，0~360~...，正负表示旋转方向 |  
| android:toDegrees     | 动画结束时旋转的角度，0~360~...，正负表示旋转方向  |  
| android:pivotX     | 这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p。 |
| android:pivotY     | 这个属性有三种样式：数值、百分数、百分数p。例：50、50%、50%p。 |
android:pivotX和android:pivotY的用法跟scale标签一样，参考scale标签。

 
 
* AlphaAnimation，alpha标签，透明动画

xml
```
<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromAlpha="0"
    android:toAlpha="1">
</alpha>
```
xml引用：
```
Animation animation = AnimationUtils.loadAnimation(this,resourceId);
animation.setDuration(3000);
animation.setRepeatCount(Animation.INFINITE);       
imageView.startAnimation(animation);
```
java代码使用：
```
AlphaAnimation alphaAnimation = new AlphaAnimation(0,1);
alphaAnimation.setDuration(2000);
alphaAnimation.setFillAfter(false);
imageView.startAnimation(alphaAnimation);
```

| 属性     | 用法 |  
| --------   | -----  | 
| android:fromAlpha     |动画开始时的透明度，0~1，0为完全透明 |  
| android:toAlpha     | 动画结束时的透明度，0~1，0为完全透明 |  


* AnimationSet，set标签，动画集合
动画的集合，set标签中可以设置alpha，rotate，scale，translate标签中的任何一种或者多种组合。而AnimationSet类中可以通过addAnimation()方法添加多个Animation补间动画，AnimationSet中可以通过在各个Animation中设置启动时间setStartOffset()来达到顺序播放的效果。在各个Animation中设置单个动画的循环播放setRepeatMode(),setRepeatCount()来达到循环播放的效果。不过这个循环播放效果不一定如逾期，建议使用AnimatorSet来达到更加精确的控制播放效果
xml
```
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="3000"
    android:fillAfter="true"
    android:fillBefore="true"
    android:repeatMode="reverse"
    android:shareInterpolator="true"
    android:startOffset="100"
    >

    <alpha
        android:toAlpha="0"
        android:fromAlpha="1"></alpha>

    <rotate
        android:drawable="@color/black"
        android:fromDegrees="0"
        android:toDegrees="360"
        android:pivotX="200"
        android:pivotY="200"
        android:visible="true"
        ></rotate>

    <scale
        android:fromXScale="0"
        android:fromYScale="0"
        android:toXScale="2"
        android:toYScale="2"
        android:pivotX="0"
        android:pivotY="0"
        ></scale>

    <translate
        android:fromXDelta="-50"
        android:fromYDelta="-50"
        android:toXDelta="50"
        android:toYDelta="100"
        ></translate>
</set>
```
xml引用：
```
Animation animation = AnimationUtils.loadAnimation(this,resourceId);
imageView.startAnimation(animation);
```
java代码中使用
```
AnimationSet animationSet = new AnimationSet(true);
animationSet.addAnimation(alphaAnimation);
animationSet.addAnimation(translateAnimation);
animationSet.addAnimation(scaleAnimation);
animationSet.addAnimation(rotateAnimation);
imageView.startAnimation(animationSet);
```

| 属性     | 用法 |  
| --------   | -----  | 
| android:duration     |动画的持续时间|  
| android:fillAfter     | 如果设置为true，控件动画结束时，将保持动画最后时的状态 |
| android:fillBefore     | 如果设置为true，控件动画结束时，还原到开始动画前的状态 |
| android:repeatMode     | 有两个值：reverse,restart。reverse表示倒序回放，restart表示重新放一遍 |  
| android:shareInterpolator     | false或者true。设置是否可以用插值器 |  
| android:startOffset     |设置动画延迟时间|  


 3. 帧动画
```
    <?xml version="1.0" encoding="utf-8"?>
    <animation-list xmlns:android="http://schemas.android.com/apk/res/android"
        android:oneshot="false">
        <item android:drawable="@drawable/rocket_thrust1" android:duration="200" />
        <item android:drawable="@drawable/rocket_thrust2" android:duration="200" />
        <item android:drawable="@drawable/rocket_thrust3" android:duration="200" />
    </animation-list>
```

```
    ImageView rocketImage = (ImageView) findViewById(R.id.rocket_image);
    rocketImage.setBackgroundResource(R.drawable.rocket_thrust);

    rocketAnimation = rocketImage.getBackground();
    if (rocketAnimation instanceof Animatable) {
        ((Animatable)rocketAnimation).start();
    }
```
 


视图动画实现原理：

```
AlphaAnimation alphaAnimation = new AlphaAnimation(0,1);
alphaAnimation.setDuration(2000);
alphaAnimation.setFillAfter(false);
imageView.startAnimation(alphaAnimation);
```

View.java
```
    public void startAnimation(Animation animation) {
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);//给View设置动画
        invalidateParentCaches();
        invalidate(true);//刷新View，会调用draw()。
    }
    public void setAnimation(Animation animation) {
        mCurrentAnimation = animation;

        if (animation != null) {
            // If the screen is off assume the animation start time is now instead of
            // the next frame we draw. Keeping the START_ON_FIRST_FRAME start time
            // would cause the animation to start when the screen turns back on
            if (mAttachInfo != null && mAttachInfo.mDisplayState == Display.STATE_OFF
                    && animation.getStartTime() == Animation.START_ON_FIRST_FRAME) {
                animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());
            }
            animation.reset();
        }
    }
    boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
        ......
        Transformation transformToApply = null;
        boolean concatMatrix = false;
        final boolean scalingRequired = mAttachInfo != null && mAttachInfo.mScalingRequired;
        final Animation a = getAnimation();
        if (a != null) {
            //修改view中的Matrix矩阵信息
            more = applyLegacyAnimation(parent, drawingTime, a, scalingRequired);
            concatMatrix = a.willChangeTransformationMatrix();
            if (concatMatrix) {
                mPrivateFlags3 |= PFLAG3_VIEW_IS_ANIMATING_TRANSFORM;
            }
            //Transformation中有Matrix矩阵信息，通过修改矩阵达到对view进行动画的目的
            transformToApply = parent.getChildTransformation();
        }
        //后续有使用transformToApply中的Matrix矩阵来进行动画，详见后续代码
        ......
    }
    private boolean applyLegacyAnimation(ViewGroup parent, long drawingTime,
            Animation a, boolean scalingRequired) {
        Transformation invalidationTransform;
        final int flags = parent.mGroupFlags;
        final boolean initialized = a.isInitialized();
        if (!initialized) {
            a.initialize(mRight - mLeft, mBottom - mTop, parent.getWidth(), parent.getHeight());
            a.initializeInvalidateRegion(0, 0, mRight - mLeft, mBottom - mTop);
            if (mAttachInfo != null) a.setListenerHandler(mAttachInfo.mHandler);
            onAnimationStart();
        }

        final Transformation t = parent.getChildTransformation();
        //获取Transformation对象并设置Transformation中的属性值
        boolean more = a.getTransformation(drawingTime, t, 1f);
        if (scalingRequired && mAttachInfo.mApplicationScale != 1f) {
            if (parent.mInvalidationTransformation == null) {
                parent.mInvalidationTransformation = new Transformation();
            }
            invalidationTransform = parent.mInvalidationTransformation;
            a.getTransformation(drawingTime, invalidationTransform, 1f);
        } else {
            invalidationTransform = t;
        }
        ......
    }
```

Animation.java
```
    public boolean getTransformation(long currentTime, Transformation outTransformation,
            float scale) {
        mScaleFactor = scale;
        return getTransformation(currentTime, outTransformation);
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation) {
        .......
        if ((normalizedTime >= 0.0f || mFillBefore) && (normalizedTime <= 1.0f || mFillAfter)) {
            if (!mStarted) {
                fireAnimationStart();
                mStarted = true;
                if (NoImagePreloadHolder.USE_CLOSEGUARD) {
                    guard.open("cancel or detach or getTransformation");
                }
            }

            if (mFillEnabled) normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), 0.0f);

            if (mCycleFlip) {
                normalizedTime = 1.0f - normalizedTime;
            }

            final float interpolatedTime = mInterpolator.getInterpolation(normalizedTime);
            applyTransformation(interpolatedTime, outTransformation);
        }
        ......
    }
    //applyTransformation是一个空方法，主要实现在AlphaAnimation，RotateAnimation，TranslateAnimation，ScaleAnimation中
    protected void applyTransformation(float interpolatedTime, Transformation t) {
    }
```

TranslateAnimation.java
```
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = mFromXDelta;
        float dy = mFromYDelta;
        if (mFromXDelta != mToXDelta) {
            dx = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime);
        }
        if (mFromYDelta != mToYDelta) {
            dy = mFromYDelta + ((mToYDelta - mFromYDelta) * interpolatedTime);
        }
        t.getMatrix().setTranslate(dx, dy);
    }
```

AlphaAnimation，RotateAnimation，TranslateAnimation，ScaleAnimation实现的applyTransformation()方法中都是以线性的速率来实现动画的效果？？？？？？？？，基本上是通过对view的Matrix矩阵操作。如果使用视图动画，但是又要求实现不同的速率，那就需要自定义Animation并实现applyTransformation()方法，在applyTransformation()方法中提供不同速率下的动画值，再通过Matrix实现动画效果。当然，更加复杂一点的动画，比如一个动画时间内，实现多个不同动画速率的效果，也是可以实现的，interpolatedTime是动画进度(0~1),根据进度的不同，设置不动的速率即可。可以参考属性动画的插值器和估值器来实现。



属性动画的原理：

https://blog.csdn.net/mg2flyingff/article/details/112726656