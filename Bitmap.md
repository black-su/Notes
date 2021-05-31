#Bitmap


参考：

https://blog.csdn.net/wanliguodu/article/details/84973846

https://blog.csdn.net/guolin_blog/article/details/54895665

人类肉眼可以识别的大部分颜色，都可以用红绿蓝三种颜色组成，根据这三种颜色的占比不同，就可以显示出大部分颜色。红绿蓝俗称光学三原色。RGB，R代表Red（红色），G代表Green（绿色），B代表Blue（蓝色）。

RGB每一个原色都有亮度的数值，0～256，0为最暗，256为最亮。根据RGB亮度的不同，可以组合不同颜色。（当然，亮度的数值可以有更大的，比如16位，24位，32位，不过那些颜色人类肉眼已经无法识别了，只作为研究用。2^8=256俗称8位）比如：
RGB(255,255,255) 为白色
RGB(0,0,0) 为黑色
RGB(255,255,0) 为黄色
RGB(0,255,0) 为绿色

计算机中只能用二进制来表示，RGB如果要在计算机使用，那就要把0～256这些数值转换成二进制来表示。
比如：
RGB(0b11111111,0b11111111,0b11111111) 为白色
RGB(0b00000000,0b00000000,0b00000000) 为黑色
RGB(0b11111111,0b11111111,0b00000000) 为黄色
RGB(0b00000000,0b11111111,0b00000000) 为绿色

根据二进制的位数，我们可以计算出RGB每一个原色在计算机中的大小是8bit，2^8 = 256。8个二进制刚好表示完所有的256个亮度值。

二进制太麻烦了，它适合计算机识别，但对于我们来说位数太多，转换成16进制更容易识别。(2^4)^2 = 2^8。比如：
RGB(0xff,0xff,0xff) 为白色，---> 0xffffff
RGB(0x00,0x00,0x00) 为黑色，---> 0x000000
RGB(0xff,0xff,0x00) 为黄色，---> 0xffff00
RGB(0x00,0xff,0x00) 为绿色，---> 0x00ff00

0x00ff00就是我们代码中常用的颜色定义了。

而有时我们需要表示一个颜色透明度的时候，就需要引入透明度值alpha：（透明）0 –> 255（不透明），它在计算机中的大小也是8bit。比如：

ARGB(0xff,0xff,0xff,0xff) 为不透明白色，---> 0xffffffff
ARGB(0xff,0x00,0x00,0x00) 为不透明黑色，---> 0xff000000
ARGB(0x00,0xff,0xff,0x00) 为透明黄色，---> 0x00ffff00
ARGB(0x00,0x00,0xff,0x00) 为透明绿色，---> 0x0000ff00



图片都是由一个个像素组合起来的，像素上填充的就是一个GRB三原色。而有些图片需要到透明度，所以有些图片填充的就是AGRB。

根据业务上的需求，不是所有的业务都需要这么高清的图片，为了减少图片的内存大小。Android的Bitmap.Config提供了四个像素选择类型：

ALPHA_8：只需要保存图片的透明度，每一个像素占8bit，其他颜色信息抛弃。
ARGB_4444：AGRB原本都需要8bit才能表示完，现在减少为4bit，这样一些像素就没办法显示部分阉割掉的颜色，整个图片质量较差，官方不建议使用。
ARGB_8888：按照原本AGRB的原色大小显示元素。图片质量好，但是图片的内存较大。
RGB_565：不存储透明度，而且RGB原本的8bit分别砍为565bit，同样导致图片质量较差，不过比ARGB_4444要好一些。


图片的格式：png/jpg/webp/git

https://www.zhihu.com/question/20028452


我们常见的png/jpg/webp图片格式，是已经被压缩算法转换过的一种数据格式。

android中要使用，就必须把这类的数据格式再转换成android所识别的数据格式，那就是Bitmap。Bitmap中存储了图片的像素信息，我们可以通过Bitmap来操作像素。通常我们也使用Drawable来操作图片，不过Drawable并不直接操作像素，它的存在更像是存储对Canvas的一系列操作，它是一个抽象类，它的实现类BitmapDrawable会通过Bitmap来操作像素，并把 Bitmap 渲染到 Canvas 上。


```
//新建一个空白的，ARGB_8888格式的Bitmap
Bitmap bitmap = Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
//加载图片文件到Bitmap中并返回Bitmap对象
Bitmap bitmapPic = BitmapFactory.decodeResource(getResources(),R.drawable.picture1);
```

他们的区别是：第二种Bitmap是不可变更的，意味着我们不能直接修改这个Bitmap的像素。如果我们需要修改像素，一般会把Bitmap中的像素先写入流中，然后再新建一个Bitmap从流中读出像素信息。这样我们就可以修改新建Bitmap中的像素信息。如下：

```
ByteArrayOutputStream baos = new ByteArrayOutputStream();
//把Bitmap中的像素信息写入流中
bitmapPic.compress(Bitmap.CompressFormat.PNG,100,baos);
ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
BitmapFactory.Options options = new BitmapFactory.Options();
options.outWidth = 100;
options.outHeight = 100;
BitmapFactory.decodeStream(isBm,null,options);
//把流中的像素信息读到Bitmap中
Bitmap inBitmap = Bitmap.createBitmap(options.outWidth, options.outHeight, Bitmap.Config.RGB_565);
```


Bitmap的创建需要向jvm申请内存，如果图片大太，或者有多个比较大的Bitmap同时申请内存，会导致jvm抛出OOM异常。

Bitmap所需的内存大小和图片的分辨率，图片所在位置，Bitmap加载方式都有关，具体的可以查看：

https://www.cnblogs.com/dasusu/

减轻Bitmap所占内存有两种方式：
一是把图片的像素大小调小一点，这会导致图片质量变差。(ALPHA_8,ARGB_4444,ARGB_8888,RGB_565)
二是把图片的分辨率调小(减小图片宽高)。



调整像素（质量压缩）：

调整像素的思路仍然是改变Bitmap.Config，由ARGB_8888改为RGB_565等。不过由于直接从资源文件中BitmapFactory.decodeResource得到的Bitmap是不可变更的，因此需要通过流写入读出来创建一个新的Bitmap来操作。

```
ByteArrayOutputStream baos = new ByteArrayOutputStream();
//把Bitmap中的像素信息写入流中
bitmapPic.compress(Bitmap.CompressFormat.PNG,100,baos);
ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
BitmapFactory.Options options = new BitmapFactory.Options();
options.outWidth = 100;
options.outHeight = 100;
BitmapFactory.decodeStream(isBm,null,options);
//把流中的像素信息读到Bitmap中
Bitmap inBitmap = Bitmap.createBitmap(options.outWidth, options.outHeight, Bitmap.Config.RGB_565);
```



调整分辨率

BitmapFactory的内部类Options中有一个inJustDecodeBounds参数，设置inJustDecodeBounds=true，可以在不给图片分配内存时读取图片的基本信息，读取并设置之后，再把该值改为false，然后再进行图片解析。
Options中还有一个inSampleSize参数，inSampleSize=1时表示按照原图片比例进行加载。inSampleSize=2表示按照原图片宽高1/2的比例进行加载。由于inSampleSize是按照比例来压缩原图的，因此需要计算合适的比例值。

```
public static Bitmap decodeResource(Resources res, int resId,
                                                     int reqWidth, int reqHeight) {
    // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(res, resId, options);
    // 计算inSampleSize值
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
    // 使用获取到的inSampleSize值再次解析图片
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeResource(res, resId, options);
}

public static int calculateInSampleSize(BitmapFactory.Options options,
                                        int reqWidth, int reqHeight) {
    // 源图片的高度和宽度
    final int height = options.outHeight;
    final int width = options.outWidth;
    // inSampleSize不能小于1
    int inSampleSize = 1;
    // 源图片的高或宽大于要求的
    if (height > reqHeight || width > reqWidth) {
        // 计算出实际宽高和目标宽高的比率
        final int heightRatio = Math.round((float) height / (float) reqHeight);
        final int widthRatio = Math.round((float) width / (float) reqWidth);
        // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
        // 一定都会大于等于目标的宽和高。
        inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
    }
    return inSampleSize;
}
```


inSampleSize是按照比例同时压缩原图宽高的的，我们也可以使用Matrix来按照不同的比例分别压缩或者放大原图。

```
        Matrix matrix = new Matrix();
        matrix.postScale(0.5f,2f);
        Bitmap bitmapNew = null;
        try {
            bitmapNew = Bitmap.createBitmap(bitmapPic2,0,0,bitmapPic2.getWidth(),bitmapPic2.getHeight(),matrix,true);
        }catch (OutOfMemoryError e){
            e.printStackTrace();
            bitmapNew = null;
            System.gc();
        }
```


综上，避免Bitmap出现OOM异常，思路应该是：大多数图片的原始像素都是ARGB_8888，为避免图片失真，应该先按照业务上对图片的需求，对图片进行分辨率的压缩，不满足需求的情况下再对图片进行像素压缩。或者业务上对图片高清度要求较低，也可以直接进行像素压缩。



以上是为了控制Bitmap在内存上的大小进行而进行的图片压缩。还有一种情况是，手机拍照后，一般图片都比较大，如果需要把图片进行上传，需要压缩图片，减少其在硬盘上的存储大小。这个压缩不影响Bitmap的加载内存，但是影响图片的质量，压缩过大的情况下，Bitmap拿到的图片就会失真严重。

```
    public static Bitmap compressbyQuality(Bitmap image, Bitmap.CompressFormat compressFormat) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(compressFormat, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int quality = 100;

        while ( baos.toByteArray().length / 1024 > 100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            if(quality > 10){
                quality -= 20;//每次都减少20
            }else {
                break;
            }
            image.compress(Bitmap.CompressFormat.JPEG, quality, baos);//这里压缩options%，把压缩后的数据存放到baos中

        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeStream(isBm, null, options);//把ByteArrayInputStream数据生成图片

        return bmp;
    }
```



Bitmap的缓存：

预防OOM异常：

```
        LruCache<String, Bitmap> mLruCache;
        //获取手机最大内存 单位 kb
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        //一般都将1/8设为LruCache的最大缓存
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(maxMemory / 8) {

            /**
             * 这个方法从源码中看出来是设置已用缓存的计算方式的。
             * 默认返回的值是1，也就是没缓存一张图片就将已用缓存大小加1.
             * 缓存图片看的是占用的内存的大小，每张图片的占用内存也是不一样的，
             * 一次不能这样算。
             * 因此要重写这个方法，手动将这里改为本次缓存的图片的大小。
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        mLruCache.put("key", BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        Bitmap bitmap = mLruCache.get("key");
```


Bitmap的Options内部类中inBitmap的复用，详细参见，一般是结合LruCache，SoftReference来使用

https://www.cnblogs.com/mengfanrong/p/4679342.html

DiskLruCache源码分析：

https://www.imooc.com/article/280381

https://blog.csdn.net/guolin_blog/article/details/28863651

Glide中的图片缓存源码分析，待研究.......

https://blog.csdn.net/guolin_blog/article/details/53759439