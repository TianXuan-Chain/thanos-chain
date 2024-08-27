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
package com.thanos.chain.network.protocols.rlpx;

import com.thanos.common.crypto.key.asymmetric.ec.ECKeyOld;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.spongycastle.math.ec.ECPoint;

import static com.thanos.common.utils.ByteUtil.toHexString;


/**
 * Auth Response message defined by EIP-8
 *
 * @author mkalinin
 * @since 17.02.2016
 */
public class AuthResponseMessageV4 {

    ECPoint ephemeralPublicKey; // 64 bytes - uncompressed and no type byte
    byte[] nonce; // 32 bytes
    int version = 4; // 4 bytes

    static AuthResponseMessageV4 decode(byte[] wire) {

        AuthResponseMessageV4 message = new AuthResponseMessageV4();

        RLPList params = (RLPList) RLP.decode2OneItem(wire, 0);

        byte[] pubKeyBytes = params.get(0).getRLPData();

        byte[] bytes = new byte[65];
        System.arraycopy(pubKeyBytes, 0, bytes, 1, 64);
        bytes[0] = 0x04; // uncompressed
        message.ephemeralPublicKey = ECKeyOld.CURVE.getCurve().decodePoint(bytes);

        message.nonce = params.get(1).getRLPData();

        byte[] versionBytes = params.get(2).getRLPData();
        message.version = ByteUtil.byteArrayToInt(versionBytes);

        return message;
    }

    public byte[] encode() {

        byte[] publicKey = new byte[64];
        System.arraycopy(ephemeralPublicKey.getEncoded(false), 1, publicKey, 0, publicKey.length);

        byte[] publicBytes = RLP.encode(publicKey);
        byte[] nonceBytes = RLP.encode(nonce);
        byte[] versionBytes = RLP.encodeInt(version);

        return RLP.encodeList(publicBytes, nonceBytes, versionBytes);
    }

    @Override
    public String toString() {
        return "AuthResponseMessage{" +
                "\n  ephemeralPublicKey=" + ephemeralPublicKey +
                "\n  nonce=" + toHexString(nonce) +
                "\n  version=" + version +
                '}';
    }
}
