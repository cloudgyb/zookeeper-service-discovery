# 基于Zookeeper Java API实现服务注册与发现功能
通过使用zookeeper提供的java API更好的学习和理解zookeeper。

# 需求描述
大致满足服务注册与发现的基本功能

- 服务的注册功能
- 服务上线自动注册到zookeeper
- 服务离线自动从zookeeper中删除
- 服务动态发现功能
    - 服务上线自动拉取zookeeper中已经注册的服务信息。
    - 有新的服务注册到zookeeper，自动拉取更新本地注册表。
    - 有服务离线自动删除本地注册表中该服务的信息。
- 保证服务信息数据的最终一致性
  
![架构图](https://img-blog.csdnimg.cn/20210227153112358.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2d5YnNoZW4=,size_16,color_FFFFFF,t_70 =450x300)
# 设计思路
## 相关概念
- 命名空间(namesapce)： 用于划分不同的服务群。
- 服务群：多个相关联的服务可以组成服务群，一个服务群使用一个唯一的命名空间(namespace)与其他服务群隔离，便于管理。
- 服务注册：将服务实例信息注册到对应的命名空间中。
- 服务发现：从zookeeper注册中心对应的命名空间拉取服务信息，并保存到本地注册表。

![image](https://img-blog.csdnimg.cn/20210227160009439.png)

## 实现方案
整个系统分为两个模块：服务注册模块和服务发现模块。
服务系统要注册到zookeeper，首先需要知道zookeeper的地址信息，有关zookeeper的配置信息配置到**zookeeper.properties**中。
使用application.properties保存服务相关配置，这些配置是注册时服务实例信息的重要来源。
系统使用log4j输出日志，使用log4j.properties配置日志。
+ zookeeper.properties 包含以下配置项
    ```properties
    zk.server.addr=localhost:2181       # zookeeper地址
    zk.server.session.timeout=5000            
    zk.server.namespace=/service      #服务群的命名空间(namespace)，服务将注册到该znode节点下
    ```
+ application.properties 包含一下配置项
     ```properties
     application.name=common-service #应用名称，作为服务名
  server.port=8093   #该服务的端口
     ```
+ log4j.properties参考log4j相关配置

### 服务注册实现方案
当服务启动时，读取配置，初始化zookeeper客户端。首先要确保配置的namespace在zookeeper中存在对应的znode节点，如果不存在则先创建。然后判断application.name（也作为serviceName）值为path的znode节点是否存在，不存在则调用zookeeper api创建以application.name值为path的znode节点，此节点将作为该服务的注册路径，接下来该服务的所有实例都注册到该节点下。然后以application.name（也作为serviceName）值为path在/namespace/serverName路径下创建服务实例对应path并写入该服务实例的数据，需要注意以临时序列号的方式创建该znode，至此服务注册完成。
大致总结:
1. 以持久化方式创建/namespace的zookeeper znode作为服务群命名空间。
2. 以持久化方式创建/namespace/serviceName的znode作为服务集群的注册路径
3. 当服务注册时，以临时序列号方式方式创建/namesapce/serviceName/serviceName0000000000的znode，并将服务信息写入到该节点下。以临时序列号方式方式创建该节点是因为当服务断开后会自动删除该节点，这样zookeeper会通知监听该znode的服务，动态的更新服务注册表，这样就实现了服务的动态发现功能。

当多个服务的多个实例注册完成后zookeeper znode如下：<br>
![image](https://img-blog.csdnimg.cn/20210227164835718.png)
服务注册完成后，使用zookeeper的api watch /namespace节点即可监听该路径的变化。

### 服务发现实现方案
服务启动后，会主动调用zookeeper的api遍历/namesapce路径，获取所有的children节点，将数据保存在该服务下，这样就发现了所有的服务信息。在服务启动时，已经监听了/namespace znode节点，所以以后如果有新的服务注册到zookeeper或者有服务断开zookeeper，将会通知该服务，该服务会更新自己的注册表。这样服务的动态发现就实现了。


# 总结
基本实现了服务注册和发现的功能，但是代码中有很多设计不合理的地方，很多需要优化的地方，仅供学习。<br>
CSDN 博客：[https://blog.csdn.net/gybshen/article/details/114175549](https://blog.csdn.net/gybshen/article/details/114175549)
