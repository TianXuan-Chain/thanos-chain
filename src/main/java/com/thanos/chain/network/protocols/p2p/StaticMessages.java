/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.network.protocols.p2p;


import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.network.protocols.base.ReasonCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.
 *
 * @author Roman Mandeleil
 * @since 13.04.14
 */
public class StaticMessages {

    SystemConfig config;


    public final static PingMsg PING_MESSAGE = new PingMsg();
    public final static PongMsg PONG_MESSAGE = new PongMsg();
    public final static GetPeersMsg GET_PEERS_MESSAGE = new GetPeersMsg();
    public final static DisconnectMsg DISCONNECT_MESSAGE = new DisconnectMsg(ReasonCode.REQUESTED);


    public StaticMessages(SystemConfig config) {
        this.config = config;
    }

    public HelloMsg createHelloMessage(byte[] peerId) {
        return createHelloMessage(peerId, config.getGenesis().getShardingNum(), config.listenRpcPort());
    }
    public HelloMsg createHelloMessage(byte[] peerId, short shardingNum, int listenPort) {

        String helloAnnouncement = buildHelloAnnouncement();
        byte p2pVersion = config.defaultP2PVersion();
        return new HelloMsg(p2pVersion, helloAnnouncement, shardingNum, listenPort, peerId);
    }

    private String buildHelloAnnouncement() {
        String version = config.projectVersion();
        String numberVersion = version;
        Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)*");
        Matcher matcher = pattern.matcher(numberVersion);
        if (matcher.find()) {
            numberVersion = numberVersion.substring(matcher.start(), matcher.end());
        }
        String system = System.getProperty("os.name");
        if (system.contains(" "))
            system = system.substring(0, system.indexOf(" "));
        if (System.getProperty("java.vm.vendor").contains("Android"))
            system = "Android";
        String phrase = "Dev";

        return String.format("x-chain/v%s/%s/%s/Java/%s", numberVersion, system,
                config.projectVersionModifier().equalsIgnoreCase("release") ? "Release" : "Dev", phrase);
    }
}
