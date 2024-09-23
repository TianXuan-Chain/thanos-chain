# 介绍
天玄区块链中的节点程序主要由两部分组成：

* 节点应用服务
* 节点网关

本仓库代码为节点应用服务部分，主要任务是负责共识、执行、存储等。

# 编译
```sh
mvn clean install -Dmaven.test.skip=true
```

注意， 节点网关编译打包需要依赖 thanos-common.jar 包。详细的打包教程可见：[打包可执行文件](https://github.com/TianXuan-Chain/tianxuan-docs/blob/new-pages/tools/blockchain-browser/installation-manual/tianxaun-chain/executable-file.md)
