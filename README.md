# x-downloader 分布式多任务下载器

#### 介绍
x-downloader 是一款分布式下载器，目前支持HTTP、FTP 文件下载（其中集成了m3u8文件的下载）。

下载器存在多个分支:

biz_rpc_dev分支：基于netty的rpc分布式下载器(进行中)

nacos_mq分支：基于nacos和rabitmq的分布式下载器(已完成)


本下载器采用调度和分配策略，结合生产者消费者模型，
天然支持

动态调整下载速率(同时下载文件数)、

查看下载进度、优先级下载(mq+内部优先队列)、

断点下载、动态横向扩容(增加下载节点)

等功能。

#### 架构
架构说明
### RPC版本
架构图的在线地址：https://www.processon.com/view/link/61e5236563768906b3e9a052

### 最近更新


### 调用远程服务足够简单(类似于feign)

#### 底层原理：
- 将被@RpcClient修饰的接口，通过动态代理生成可以自动netty数据的bean，并注入到springIOC容器中
- 通过获取bean，并执行bean方法时，将会触发增强的代理bean，并将方法的名称和参数通过netty发送给目标
- 目标类获取RPC信息后，通过反射在容器中找到目标方法并执行
```java
//例子：服务A调用服务B的register方法
//服务A
//远程服务接口
@RpcClient("ServiceB")
public interface ServiceB {
    void register(BasicFrame<Object> frame);
}

class Test {
    //或者通过@Autowired
    @Autowired
    private ServiceB serviceb;

    //使用远程服务
    public void test() {
      serviceb.register(new BasicFrame());
    }
}
//服务B
//远程目标方法
@RpcComponent("ServiceB")
public class ServiceB {
    @RpcHandler("register")
    public void register(BasicFrame<Object> frame) {
        //打印BasicFrame数据
        log.info(frame);
    }
}

```

#### 技术点


##### 8.任务调度模型
![](./readme/image/dispatcher.png)

##### 7.多任务处理模型
![](./readme/image/multiply%20task%20thread%20group%20handler.png)


##### 6.客户端组
![](./readme/image/clientGroup.png)

##### 5.三种架构模式
![](./readme/image/架构模式.png)

##### 4.服务端高可用方案
![](./readme/image/服务端HA.png)

##### 3.快慢缓冲队列
![](./readme/image/快慢消费.png)

##### 2.时空消费队列
![](./readme/image/时空生产-缓冲队列.png)

##### 1.分配路由表
![](./readme/image/routepage.png)


### Nacos +RabbitMq 版本
![](./readme/image/x-downloader架构图.png)


#### 使用说明

任务参数说明

```json
{
	 timestamp: 1600000000, //时间戳 
	 task: { //下载任务
		id: 1001, //任务id     
		retryCount: 0, //重试次数   
		sourceFtpUrl: "ftp://xxx", //源FTP地址 
		targetFtpUrl: "ftp://ttt", //目标FTP地址     
		fileType: 1, //文件类型     
		priority: 10, //优先级     
		notifyUrl: "http://servername/callback/xxx", //通知地址     
		fileCode: "BDNxxx", //文件code     
		status: 1 ,//下载任务状态     
		startTime: 160000, //下载开始时间     
		finishTime: 160000, //下载完成时间     
		duraltion: 10000, //下载持续时常     
		taskServerInstanceId: "ip + port + xx" //nacos 组件唯一id 
	}
}
```




#### 参与贡献



#### 下个版本解决的问题
1.【downloader】下载节点异常时，缺少一个 队列剩余任务 自动再分配的处理
2.【dispatcher】下载节点异常时，缺少一个 重试任务队列 自动再分配的处理
3.【dispatcher】在不依赖xxljob的情况下，现有方案还不够完美
4.缺少一个全任务流程追踪的可视化过程
