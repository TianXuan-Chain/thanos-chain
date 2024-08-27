#!/usr/bin/env bash

# 若不指定confPath 则去 运行路径的同级目录下的resource/thanos-chain.conf 查找
# 整个JVM内存大小=年轻代大小 + 年老代大小 + 持久代大小
# -Xmx3550m：设置JVM最大可用内存为3550M。
# -Xms3550m：设置JVM促使内存为3550m。此值可以设置与-Xmx相同，以避免每次垃圾回收完成后JVM重新分配内存
# -Xmn2g：设置年轻代大小为2G,持久代一般固定大小为64m，所以增大年轻代后，将会减小年老代大小。此值对系统性能影响较大，Sun官方推荐配置为整个堆的3/8
# -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m
# java  -Xmx3550m -Xms3550m -Xmn2g -Xss4M -jar thanos-chain.jar
# eden from to ->  eden ratio=Y/(Y+1+1), -XX:SurvivorRatio=8，表明Y=8
java  -Xmx7100m -Xms7100m -Xmn4g -Xss4M -XX:SurvivorRatio=8 -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -jar thanos-chain.jar
#java  -Xmx3550m -Xms3550m -Xmn2g -Xss4M -DconfPath="C:\dev\project\self\x-chain\src\main\resources\thanos-chain.conf"  -jar x-chain.jar
#java  -Xmx3550m -Xms3550m -Xmn2g -Xss4M -DconfPath="C:\dev\project\self\x-chain-node\node1\thanos-chain.conf"  -jar x-chain.jar
