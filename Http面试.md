# Http面试

1. http1.0,http1.1,http2，webSocket的区别

HTTP1.1相比与HTTP1.0

错误通知的管理，在HTTP1.1中新增了24个错误状态响应码，如409（Conflict）表示请求的资源与资源的当前状态发生冲突；410（Gone）表示服务器上的某个资源被永久性的删除。

Host头处理，在HTTP1.0中认为每台服务器都绑定一个唯一的IP地址，因此，请求消息中的URL并没有传递主机名（hostname）。但随着虚拟主机技术的发展，在一台物理服务器上可以存在多个虚拟主机（Multi-homed Web Servers），并且它们共享一个IP地址。HTTP1.1的请求消息和响应消息都应支持Host头域，且请求消息中如果没有Host头域会报告一个错误（400 Bad Request）。

长连接，HTTP 1.1支持长连接（PersistentConnection）和请求的流水线（Pipelining）处理，在一个TCP连接上可以传送多个HTTP请求和响应，减少了建立和关闭连接的消耗和延迟，在HTTP1.1中默认开启Connection： keep-alive，一定程度上弥补了HTTP1.0每次请求都要创建连接的缺点。

HTTP2相比于HTTP1.1：

服务端推送（server push）：

header压缩：通讯双方维护一张表记录每次建立TCP时的header信息，之后的每次请求不需要在携带header信息，只需要携带一个标记即可。

多路复用：其实就是socket通道的复用(一定时间内不会主动关闭socket通道)，同一个链接的请求共用着一个socket通道，也就是TCP通道。发送方的request有一个id，请求到达接收端后，接收端根据id归属到不同的服务器端请求里面

二进制格式（帧+TLS/SSL）：HTTP1传送的文本数据，一次性传输一个完整的报文数据。HTTP2会把数据分割成一个个帧数据，根据TCP滑动窗口的大小传送多个帧数据。

HTTP1.1的长连接和HTTP2多路复用的区别：HTTP1.1的一个TCP通道中可以请求多个request，但是必须对应返回响应的response，才可以继续下一个request的请求。一个request拿不到response会导致后续的request阻塞。而HTTP2可以在同一个TCP通道中同时请求多个request，请求之间不会阻塞，根据返回的ack和tcp窗口的大小决定下一次发送多少个request。

webSocket

webSocket是一个协议，与HTTP1协议有部分重叠，主要是为了解决持久性HTTP连接。HTTP1.1的长连接是在同一个TCP通道内重复发起request，本质上还是由客户端发起，无法做到双工通讯。HTTP2的多路复用也是在同一个TCP内反复发起request，优化了header头部信息，其传输数据可以是二进制也可以是文本。它的服务端推送只能在浏览器上实现，在应用程序是做不到的双工通讯的。
webSocket通过发起一次HTTP请求，告知服务端建立Websocket协议，服务端同意并响应后，socket通道就建立成功了，之后这个socket通道就不会关闭，除非客户端或者服务端主动发起关闭。在实际应用中，webSocket需要先发起一次HTTP请求建立socket连接，之后才能交换数据。
https://www.zhihu.com/question/20215561


https://www.cnblogs.com/heluan/p/8620312.html

2. TCP滑动窗口

TCP和UDP都应用在传输层，TCP属于可靠性传输，UDP属于不可靠传输。TCP的可靠传输依赖于其细节的实现，比如滑动窗口适应系统，超时重传机制，累计ACK等。

滑动窗口解决了数据的乱序，数据的吞吐量问题。

https://blog.csdn.net/wdscq1234/article/details/52444277


3. keep-alive

socket通道建立后，如果代码中不主动close或者程序关闭，socket通道是一直都在的，占据着cpu和内存资源，socket通道本身就是一个持久性连接。socket中提供了keepalive字段，程序使用者可以设置keepalive字段为true，这样socket通道就会在空闲时发送探测包，探测包的发送条件是2个小时内双方仍然没有数据传输。这个2小时的时限无法修改，依赖于系统配置，除非我们去修改系统配置。
keepalive只是为了防止连接的双方发生意外而通知不到对方，导致一方还持有连接，占用资源。


HTTP中的keep-alive则是针对socket通道的复用。如果设置了keep-alive为true，那么就复用。

https://www.cnblogs.com/xiao-tao/p/9718017.html
https://zhuanlan.zhihu.com/p/224595048

4. HTTP的报文格式

请求行（请求方法GET/POST，URL，协议版本），请求头（Host，编码等键值对信息），请求正文
状态行（状态码，状态描述，协议版本），响应头(编码，正文类型等键值对信息)，响应正文

200 OK  请求成功。一般用于GET与POST请求
404 Not Found   服务器无法根据客户端的请求找到资源（网页）。通过此代码，网站设计人员可设置"您所请求的资源无法找到"的个性页面
502 Bad Gateway 作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应

https://www.pianshen.com/article/1601187790/

5. get/post的区别

浏览器中的区别：
get请求会缓存，回退时不会重复请求。post相反
get会收藏为书签，会保留在历史记录中。post相反
get的长度在部分浏览器中被限制。post相反

浏览器和程序设计中都存在的区别：
get暴露请求参数。post请求参数在body中
get的参数类型只能是ASCII字符，并且要按照application/x-www-form-urlencoded的编码方式以键值对形式出现。post支持多种参数类型比如二进制，所有参数都可以支持各种编码。
get

编码方式：
常见的媒体格式类型如下：

text/html ： HTML格式
text/plain ：纯文本格式
text/xml ： XML数据格式
image/gif ：gif图片格式
image/jpeg ：jpg图片格式
image/png：png图片格式
以application开头的媒体格式类型：

application/xhtml+xml ：XHTML格式
application/xml： XML数据格式
application/atom+xml ：Atom XML聚合格式
application/json： JSON数据格式
application/pdf：pdf格式
application/msword ： Word文档格式
application/octet-stream ： 二进制流数据（如常见的文件下载）
application/x-www-form-urlencoded ： <form encType=””>中默认的encType，form表单数据被编码为key/value格式发送到服务器（表单默认的提交数据的格式）
multipart/form-data ： 需要在表单中进行文件上传时，就需要使用该格式

https://www.pianshen.com/article/1838102223/

6. 校验证书

服务器 用RSA生成公钥和私钥
把公钥放在证书里发送给客户端，私钥自己保存
客户端首先向一个权威的服务器检查证书的合法性，如果证书合法，客户端产生一段随机数，这个随机数就作为通信的密钥，我们称之为对称密钥，用公钥加密这段随机数，然后发送到服务器
服务器用密钥解密获取对称密钥，然后，双方就已对称密钥进行加密解密通信了

常见三种加密（MD5、非对称加密，对称加密）

https://blog.csdn.net/qq_34827674/article/details/112589634
https://blog.csdn.net/lk2684753/article/details/100160856
https://blog.csdn.net/z157794218/article/details/45476723?locationNum=4&fps=1
https://www.cnblogs.com/shoshana-kong/p/10934550.

7. TCP与UPD的区别

https://www.cnblogs.com/williamjie/p/9390164.html

8. UDP组播放

https://www.cnblogs.com/schips/p/12552534.html

9. TLS的四次握手过程

https://blog.csdn.net/qq_34827674/article/details/112589634

10. http的三次握手，四次断开过程

https://baijiahao.baidu.com/s?id=1654225744653405133&wfr=spider&for=pc

11. Retrofit

https://www.cnblogs.com/guanxinjing/p/11594249.html
https://blog.csdn.net/csdn_aiyang/article/details/80692384

12. RxJava

https://gank.io/post/560e15be2dca930e00da1083
https://juejin.cn/post/6844903838978146317

13. Volley

https://www.cnblogs.com/summers/p/4388471.html
https://blog.csdn.net/Virgil_K2017/article/details/89311971

14. HttpURLConnection

https://www.jianshu.com/p/35ecbc09c160?ivk_sa=1024320u

15. okhttp的网络请求缓存

是否缓存应该是由双方商定，如果只是客户端设置了开启缓存，但是服务端返回的response设置了不可缓存，那response还是不能保存在cache中。

https://www.jianshu.com/p/3f181c43b42b
https://blog.csdn.net/hesong1120/article/details/78584028

16. 


