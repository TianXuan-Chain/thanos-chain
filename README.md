# 介绍
天玄区块链中的节点程序主要由两部分组成：

* 节点应用服务
* 节点网关

本仓库代码为节点应用服务部分，主要任务是负责共识、执行、存储等。

# 编译
拉取仓库后，进入文件夹，执行下面指令
```sh
mvn clean install -Dmaven.test.skip=true
```
指令执行成功后，会在 `target` 文件夹内产生 `thanos-chain.jar` 文件。

注意， 节点应用服务编译打包需要依赖 thanos-common.jar 包。

# 教程
打包编译教程：
* [在线文档 - 打包可执行文件](https://tianxuan.blockchain.163.com/installation-manual/tianxaun-chain/executable-file.html)
* [文档仓库 - 打包可执行文件](https://github.com/TianXuan-Chain/tianxuan-docs/blob/new-pages/tools/blockchain-browser/installation-manual/tianxaun-chain/executable-file.md)

安装部署教程：
* [在线文档 - 打包可执行文件](https://tianxuan.blockchain.163.com/installation-manual/tianxaun-chain/)
* [文档仓库 - 天玄节点应用服务](https://github.com/TianXuan-Chain/tianxuan-docs/tree/main/installation-manual/tianxaun-chain)

# License
Apache 2.0
