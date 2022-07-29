> 一个用于文件存储的分布式系统


### 快速上手
> 调度器也就是服务中心启动

```java
NodeConfig nodeConfig = new NodeConfig("/src/tracker_config.json");
TrackerApplication application = new TrackerApplication(nodeConfig);
try {
    Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
    application.start();
} catch (Exception e) {
    e.printStackTrace();
    log.info("Tracker Application Start Error!!");
    System.exit(1);
}
```
> **配置参考**

```json
{
  "name": "Netty-Tracker",
  "port": 8080,
  "isMaster": true,
  "isCluster": false,
  "servers": ["master:localhost:8090","slave:localhost:8080"]
}
```

> 存储节点启动

```java
NodeConfig nodeConfig = new NodeConfig("/src/storage_config.json");
StorageApplication application = new StorageApplication(nodeConfig);
try {
    Runtime.getRuntime().addShutdownHook(new Thread(application::shutdown));
    application.start();
} catch (Exception e) {
    log.info("Tracker Application Start Error!!");
    System.exit(1);
}
```
```json
{
  "name": "Netty-Storage",
  "port": 8091,
  "host": "localhost",
  "isCluster": false,
  "trackerPort": 8080,
  "trackerHost": "localhost"
}
```

- name	服务名称
- port	服务暴露端口
- isMaster	是否是主节点	单机运行可不配置
- isCluster	是否为集群模式，默认为flase
- servers		集群信息 [ 节点角色：ip：端口 ]
- trackerPort	注册中心/调度器的端口
- trackerHost	注册中心/调度器的地址

> 客户端测试

```java
NodeConfig nodeConfig = new NodeConfig("/src/client_config.json");
ClientApplication application = new ClientApplication(nodeConfig);
try {
    application.start();
    ClientFileService fileService = application.getFileService();
    Map<String, String> attr = new HashMap<>(Constants.MAP_SIZE);
    attr.put("aaa", "1222");
    fileService.put("/aaa/bbb/susu.jpg",new File(UPLOAD_LOCAL_PATH),-1,attr);
} catch (Exception e) {
    throw new RuntimeException(e);
}
```

---

### 系统组件
#### Tracker
是系统的调度器，或者说是注册中心，客户端和storage都向Tracker进行连接，有tracker节点来分配文件所存储的stroage节点，也由Tracker来维护文件目录树
#### Straoge
是系统的存储器，主要用来存储文件，进行IO操作，以及提供文件下载的服务
### 开发日志
> 版本 version 1.0.0

- [x] 单节点的服务注册
- [x] 单节点的心跳检测
- [x] 服务端的多端口异步绑定
- [x] 使用protobufer作为rpc中的网络数据传输格式
- [x]  netty消息解码和编码
- [x] 同步消息等待
- [x] Tracker 的 客户端管理器，storage集群
- [x] 文件目录树
- [x] 磁盘的缓存双刷
- [x] 记录创建文件夹，创建文件，删除文件的日志
- [x] 文件目录树的持久化
- [x] 多文件上传
- [x] 文件上传确认，达到客户端和storage和tracker之前的一致性
- [x] tracker集群实现，注册，互相同步数据
- [x] 垃圾清理机制



