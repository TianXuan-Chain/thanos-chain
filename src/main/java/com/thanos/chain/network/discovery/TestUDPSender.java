package com.thanos.chain.network.discovery;

import com.thanos.chain.config.SystemConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.List;

/**
 * 类TestSender.java的实现描述：
 *
 * @Author laiyiyu create on 2020-02-11 18:40:52
 */
public class TestUDPSender {

    public static void main(String[] args) throws Exception {
        SystemConfig systemConfig = new SystemConfig();

        DatagramChannel channel = DatagramChannel.open();

        sendRegister(channel, systemConfig);
        Thread.sleep(1000);

        sendFindShardingNodes(channel, systemConfig);

        Thread.sleep(1000);


        sendShardingNodes(channel, systemConfig);

    }

    private static void sendRegister(DatagramChannel channel, SystemConfig systemConfig) throws IOException {
        byte[] registerbytes = createRegisterMessageBytes(systemConfig);
        ByteBuffer buf=ByteBuffer.allocate(registerbytes.length);
        buf.clear();
        buf.put(registerbytes);
        buf.flip();
        /*发送UDP数据包*/
        channel.send(buf, new InetSocketAddress("127.0.0.1",3030));

    }

    private static void sendPing(DatagramChannel channel, SystemConfig systemConfig) throws IOException {
        byte[] pingbytes = createPingMsgBytes(systemConfig);
        ByteBuffer buf=ByteBuffer.allocate(pingbytes.length);
        buf.clear();
        buf.put(pingbytes);
        buf.flip();
        /*发送UDP数据包*/
        channel.send(buf, new InetSocketAddress("127.0.0.1",3030));
    }

    private static void sendFindShardingNodes(DatagramChannel channel, SystemConfig systemConfig) throws IOException {
        byte[] findShardingNodesbytes = createFindShardingNodesMsgBytes(systemConfig);
        ByteBuffer buf=ByteBuffer.allocate(findShardingNodesbytes.length);
        buf.clear();
        buf.put(findShardingNodesbytes);
        buf.flip();
        /*发送UDP数据包*/
        channel.send(buf, new InetSocketAddress("127.0.0.1",3030));
    }

    private static void sendShardingNodes(DatagramChannel channel, SystemConfig systemConfig) throws IOException {
        byte[] shardingNodesbytes = createShardingNodesMsgBytes(systemConfig);
        ByteBuffer buf=ByteBuffer.allocate(shardingNodesbytes.length);
        buf.clear();
        buf.put(shardingNodesbytes);
        buf.flip();
        /*发送UDP数据包*/
        channel.send(buf, new InetSocketAddress("127.0.0.1",3030));
    }

    private static byte[] createRegisterMessageBytes(SystemConfig systemConfig) {
        byte[] nodeId = systemConfig.getMyKey().getNodeId();
        Node selfNode = new Node(nodeId, "127.0.0.1", 8080);
        selfNode.setShardingNum((short)9);
        RegisterMessage pingMessage = RegisterMessage.create(selfNode, systemConfig.getMyKey(), 30303);
        return pingMessage.getPacket();
    }

    private static byte[] createPingMsgBytes(SystemConfig systemConfig) {
        byte[] nodeId = systemConfig.getMyKey().getNodeId();
        Node from = new Node(nodeId, "127.0.0.1", 8080);
        Node to = new Node(nodeId, "10.0.3.1", 9090);
        PingMessage pingMessage = PingMessage.create(from, to, systemConfig.getMyKey());
        return pingMessage.getPacket();
    }

    private static byte[] createFindShardingNodesMsgBytes(SystemConfig systemConfig) {
        FindShardingNodesMessage findShardingNodesMessage = FindShardingNodesMessage.create((short) 9, systemConfig.getMyKey());
        return findShardingNodesMessage.getPacket();
    }

    private static byte[] createShardingNodesMsgBytes(SystemConfig systemConfig) {
        byte[] nodeId = systemConfig.getMyKey().getNodeId();
        Node node1 = new Node(nodeId, "127.0.0.1", 8080, (short)12);
        Node node2 = new Node(nodeId, "10.0.3.1", 8080, (short)498);
        List<Node> nodes = Arrays.asList(node1, node2);
        ShardingNodesMessage shardingNodesMessage = ShardingNodesMessage.create(nodes, systemConfig.getMyKey());
        return shardingNodesMessage.getPacket();
    }
}
