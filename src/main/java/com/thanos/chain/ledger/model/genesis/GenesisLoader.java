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
package com.thanos.chain.ledger.model.genesis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.thanos.chain.config.SystemConfig;
import com.thanos.common.utils.ByteUtil;
import com.thanos.chain.ledger.model.Genesis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;

import static com.thanos.common.utils.ByteUtil.bytesToBigInteger;
import static com.thanos.common.utils.ByteUtil.hexStringToBytes;

public class GenesisLoader {

    public static final int NONCE_LENGTH = 8;

    /**
     * Load genesis from passed location or from classpath `genesis` directory
     */
    public static GenesisJson loadGenesisJson(SystemConfig config, ClassLoader classLoader) throws RuntimeException {
        final String genesisFile = config.databaseDir() + File.separator + "genesis.json";

        // #1 try to find genesis at passed location
        if (genesisFile != null) {
            try (InputStream is = new FileInputStream(new File(genesisFile))) {
                return loadGenesisJson(is);
            } catch (Exception e) {
                new RuntimeException("Problem loading genesis file from " + genesisFile);
            }
        }

//        // #2 fall back to old genesis location at `src/main/resources/genesis` directory
//        InputStream is = classLoader.getResourceAsStream("genesis/" + genesisResource);
//        if (is != null) {
//            try {
//                return loadGenesisJson(is);
//            } catch (Exception e) {
//                showLoadError("Problem loading genesis file from resource directory", genesisFile, genesisResource);
//            }
//        } else {
//            showLoadError("Genesis file was not found in resource directory", genesisFile, genesisResource);
//        }

        return null;
    }


    public static Genesis parseGenesis(GenesisJson genesisJson) throws RuntimeException {
        try {
            Genesis genesis = createBlockForJson(genesisJson);

            //genesis.setPremine(generatePreMine(blockchainNetConfig, genesisJson.getAlloc()));

//            byte[] rootHash = generateRootHash(genesis.getPremine());
//            genesis.setStateRoot(rootHash);

            return genesis;
        } catch (Exception e) {
            new RuntimeException("Problem parsing genesis error:" + e.getMessage());
        }
        return null;
    }



    /**
     * Method used much in tests.
     */
    public static Genesis loadGenesis(InputStream resourceAsStream) {
        GenesisJson genesisJson = loadGenesisJson(resourceAsStream);
        return parseGenesis(genesisJson);
    }

    public static GenesisJson loadGenesisJson(InputStream genesisJsonIS) throws RuntimeException {
        String json = null;
        try {
            json = new String(ByteStreams.toByteArray(genesisJsonIS));

            ObjectMapper mapper = new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);

            GenesisJson genesisJson  = mapper.readValue(json, GenesisJson.class);
            return genesisJson;
        } catch (Exception e) {

            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private static Genesis createBlockForJson(GenesisJson genesisJson) {

        byte[] nonce       = prepareNonce(hexStringToBytes(genesisJson.nonce));
        byte[] mixHash     = hexStringToBytesValidate(genesisJson.mixhash, 32, false);
        byte[] coinbase    = hexStringToBytesValidate(genesisJson.coinbase, 20, false);

        byte[] timestampBytes = hexStringToBytesValidate(genesisJson.timestamp, 8, true);
        long   timestamp         = ByteUtil.byteArrayToLong(timestampBytes);

        byte[] parentHash  = hexStringToBytesValidate(genesisJson.parentHash, 32, false);

        byte[] gasLimitBytes    = hexStringToBytesValidate(genesisJson.gasLimit, 8, true);
        long   gasLimit         = ByteUtil.byteArrayToLong(gasLimitBytes);

        return new Genesis(coinbase,
                genesisJson.getStartEventNumber(), 0, genesisJson.maxShardingNum, genesisJson.shardingNum);
    }



    private static byte[] hexStringToBytesValidate(String hex, int bytes, boolean notGreater) {
        byte[] ret = hexStringToBytes(hex);
        if (notGreater) {
            if (ret.length > bytes) {
                throw new RuntimeException("Wrong value length: " + hex + ", expected length < " + bytes + " bytes");
            }
        } else {
            if (ret.length != bytes) {
                throw new RuntimeException("Wrong value length: " + hex + ", expected length " + bytes + " bytes");
            }
        }
        return ret;
    }

    /**
     * Prepares nonce to be correct length
     * @param nonceUnchecked    unchecked, user-provided nonce
     * @return  correct nonce
     * @throws RuntimeException when nonce is too long
     */
    private static byte[] prepareNonce(byte[] nonceUnchecked) {
        if (nonceUnchecked.length > 8) {
            throw new RuntimeException(String.format("Invalid nonce, should be %s length", NONCE_LENGTH));
        } else if (nonceUnchecked.length == 8) {
            return nonceUnchecked;
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        int diff = NONCE_LENGTH - nonceUnchecked.length;
        for (int i = diff; i < NONCE_LENGTH; ++i) {
            nonce[i] = nonceUnchecked[i - diff];
        }
        return nonce;
    }



    /**
     * @param rawValue either hex started with 0x or dec
     * return BigInteger
     */
    private static BigInteger parseHexOrDec(String rawValue) {
        if (rawValue != null) {
            return rawValue.startsWith("0x") ? bytesToBigInteger(hexStringToBytes(rawValue)) : new BigInteger(rawValue);
        } else {
            return BigInteger.ZERO;
        }
    }

//    public static byte[] generateRootHash(Map<ByteArrayWrapper, PremineAccount> premine){
//
//        Trie<byte[]> state = new SecureTrie((byte[]) null);
//
//        for (ByteArrayWrapper key : premine.keySet()) {
//            state.put(key.getData(), premine.get(key).accountState.rlpEncoded());
//        }
//
//        return state.getRootHash();
//    }
}
