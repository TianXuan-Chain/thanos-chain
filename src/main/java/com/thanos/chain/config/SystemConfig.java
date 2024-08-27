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
package com.thanos.chain.config;

import com.thanos.chain.ledger.model.Genesis;
import com.thanos.chain.ledger.model.genesis.GenesisJson;
import com.thanos.chain.ledger.model.genesis.GenesisLoader;
import com.thanos.common.crypto.key.asymmetric.SecureKey;
import com.thanos.common.crypto.key.asymmetric.SecurePublicKey;
import com.thanos.common.crypto.key.symmetric.CipherKey;
import com.thanos.common.crypto.key.symmetric.DefaultCipherKey;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Utility class to retrieve property values from the thanos-chain.conf files
 * <p>
 * The properties are taken from different sources and merged in the following order
 * (the config option from the next source overrides option from previous):
 * - resource thanos-chain.conf : normally used as a reference config with default values
 * and shouldn't be changed
 * - system property : each config entry might be altered via -D VM option
 * - [user dir]/config/thanos-chain.conf
 * - config specified with the -Dx-chain.conf.file=[file.conf] VM option
 * - CLI options
 *
 * @author laiyiyu
 * @since 22.05.2014
 */
public class SystemConfig {

    //private static Logger logger = LoggerFactory.getLogger("general");

    private static SystemConfig CONFIG;

    private Map<String, String> generatedNodePrivateKey;

    public static SystemConfig getDefault() {

        if (CONFIG == null) {
            CONFIG = new SystemConfig();
        }
        return CONFIG;
    }

    public byte defaultP2PVersion() {
        return 1;
    }

    /**
     * Marks config accessor methods which need to be called (for value validation)
     * upon config creation or modification
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ValidateMe {
    }

    ;


    public Config config;

    // mutable options for tests
    private String databaseDir = null;

    private String projectVersion = null;

    private String projectVersionModifier = null;

    private String bindIp = null;

    private GenesisJson genesisJson;

    private Genesis genesis;

    private Boolean vmTrace;

    private Boolean persistNeedEncrypt;

    Boolean transferDataEncrypt;

    private byte[] nodeId;

    private final ClassLoader classLoader;
    //sign msg
    private SecureKey myKey;
    //encrypt/decrypt data
    private CipherKey encryptKey;

    private GenerateNodeIdStrategy generateNodeIdStrategy = null;

    public SystemConfig() {
        this("thanos-chain.conf");
    }

    public SystemConfig(File configFile) {
        this(ConfigFactory.parseFile(configFile));
    }

    public SystemConfig(String configResource) {
        this(ConfigFactory.parseResources(configResource));
    }

    public SystemConfig(Config apiConfig) {
        this(apiConfig, SystemConfig.class.getClassLoader());
    }

    public SystemConfig(Config apiConfig, ClassLoader classLoader) {
        try {
            this.classLoader = classLoader;

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            Config referenceConfig = ConfigFactory.parseResources("thanos-chain.conf");
            //logger.info("Config (" + (referenceConfig.entrySet().size() > 0 ? " yes " : " no  ") + "): default properties from resource 'thanos-chain.conf'");
            config = apiConfig;
            config = apiConfig
                    .withFallback(referenceConfig);

//            logger.debug("Config trace: " + config.root().render(ConfigRenderOptions.defaults().
//                    setComments(false).setJson(false)));

            config = javaSystemProperties.withFallback(config)
                    .resolve();     // substitute variables in config if any
            validateConfig();

            // There could be several files with the same name from other packages,
            // "version.properties" is a very common name
            List<InputStream> iStreams = loadResources("version.properties", this.getClass().getClassLoader());
            for (InputStream is : iStreams) {
                Properties props = new Properties();
                props.load(is);
                if (props.getProperty("versionNumber") == null || props.getProperty("databaseVersion") == null) {
                    continue;
                }
                this.projectVersion = props.getProperty("versionNumber");
                this.projectVersion = this.projectVersion.replaceAll("'", "");

                if (this.projectVersion == null) this.projectVersion = "-.-.-";

                this.projectVersionModifier = "master".equals(BuildInfo.buildBranch) ? "RELEASE" : "SNAPSHOT";

                this.generateNodeIdStrategy = new GetNodeIdFromPropsFile(databaseDir());
                break;
            }
        } catch (Exception e) {
            //logger.error("Can't read config.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads resources using given ClassLoader assuming, there could be several resources
     * with the same name
     */
    public static List<InputStream> loadResources(
            final String name, final ClassLoader classLoader) throws IOException {
        final List<InputStream> list = new ArrayList<InputStream>();
        final Enumeration<URL> systemResources =
                (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                        .getResources(name);
        while (systemResources.hasMoreElements()) {
            list.add(systemResources.nextElement().openStream());
        }
        return list;
    }

    public Config getConfig() {
        return config;
    }

    private void validateConfig() {
        for (Method method : getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(ValidateMe.class)) {
                    method.invoke(this);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error validating config method: " + method, e);
            }
        }
    }


    @ValidateMe
    public int getProposerType() {
        return config.getInt("consensus.proposerType");
    }

    @ValidateMe
    public int getContiguousRounds() {
        return config.getInt("consensus.contiguousRounds");
    }

    @ValidateMe
    public int getRoundTimeoutBaseMS() {
        return config.getInt("consensus.roundTimeoutBaseMS");
    }

    @ValidateMe
    public int getMaxPackSize() {
        return config.getInt("consensus.maxPackSize");
    }

    @ValidateMe
    public int getMaxPrunedEventsInMemory() {
        return config.getInt("consensus.maxPrunedEventsInMemory");
    }

    @ValidateMe
    public boolean reimportUnCommitEvent() {
        return config.hasPath("consensus.reimportUnCommitEvent") ?
                config.getBoolean("consensus.reimportUnCommitEvent") : true;
    }

    @ValidateMe
    public boolean dsCheck() {
        return config.hasPath("consensus.dsCheck") ?
                config.getBoolean("consensus.dsCheck") : true;
    }

    @ValidateMe
    public int getMaxCommitEventNumInMemory() {
        return config.getInt("consensus.maxCommitEventNumInMemory");
    }

    @ValidateMe
    public int getPoolLimit() {
        return config.getInt("consensus.poolLimit");
    }

    @ValidateMe
    public int comingQueueSize() {
        return config.hasPath("consensus.comingQueueSize") ? config.getInt("consensus.comingQueueSize") : 64;
    }

    @ValidateMe
    public boolean isTXValid() {
        return config.hasPath("consensus.txValid") ? config.getBoolean("consensus.txValid") : false;
    }

    @ValidateMe
    public long packageTimeSleep() {
        return config.hasPath("consensus.packageTimeSleep") ? config.getLong("consensus.packageTimeSleep") : 500;
    }

    @ValidateMe
    public boolean futureCheck() {
        return config.hasPath("consensus.futureCheck") ? config.getBoolean("consensus.futureCheck") : true;
    }

    @ValidateMe
    public int decodeProcessNum() {
        int processNum = Runtime.getRuntime().availableProcessors();
        int defaultNum = 8;
        if (processNum > 100) {
            defaultNum = 64;
        } else if (processNum > 64) {
            defaultNum = 32;
        } else if (processNum > 32) {
            defaultNum = 16;
        }
        return config.hasPath("consensus.decodeProcessNum") ? config.getInt("consensus.decodeProcessNum") : defaultNum;
    }

    @ValidateMe
    public int getParallelProcessorNum() {
        return config.getInt("consensus.parallelProcessorNum");
    }


    @ValidateMe
    public int getCheckTimeoutMS() {
        return config.hasPath("state.checkTimeoutMS") ? config.getInt("state.checkTimeoutMS") : 500;
    }

    @ValidateMe
    public int getMaxCommitBlockInMemory() {
        return config.hasPath("state.maxCommitBlockInMemory") ? config.getInt("state.maxCommitBlockInMemory") : 16;
    }

    @ValidateMe
    public boolean pushBlock() {
        return config.hasPath("state.pushBlock") ? config.getBoolean("state.pushBlock") : true;
    }

    @ValidateMe
    public List<String> peerDiscoveryIPList() {
        return config.getStringList("network.peer.discovery.ip.list");
    }

//    @ValidateMe
//    public Integer blockQueueSize() {
//        return config.getInt("cache.blockQueueSize") * 1024 * 1024;
//    }

    @ValidateMe
    public Integer peerChannelReadTimeout() {
        return config.getInt("network.peer.channel.read.timeout");
    }

    @ValidateMe
    public String databaseDir() {
        return databaseDir == null ? config.getString("resource.database.dir") : databaseDir;
    }

    @ValidateMe
    public String logConfigPath() {
        return config.getString("resource.logConfigPath");
    }

    @ValidateMe
    public String projectVersion() {
        return projectVersion;
    }


    @ValidateMe
    public String projectVersionModifier() {
        return projectVersionModifier;
    }


    @ValidateMe
    public boolean vmTrace() {
        if (vmTrace != null) {
            return vmTrace;
        }

        vmTrace = config.hasPath("vm.structured.trace") ? config.getBoolean("vm.structured.trace") : false;
        return vmTrace;
    }

    //@ValidateMe
    public String vmTraceDir() {
        return config.getString("vm.structured.dir");
    }

    public String customSolcPath() {
        return null;
    }

    private Map<String, String> generatedCaNodeInfoMap() {
        if (generatedNodePrivateKey == null) {
            generatedNodePrivateKey = generateNodeIdStrategy.getCaNodeInfoMap();
        }
        return generatedNodePrivateKey;
    }

    public CipherKey getCipherKey() {
        if (encryptKey != null) {
            return encryptKey;
        }
        if (dataNeedEncrypt()) {
            Map<String, String> nodeInfoMap = generatedCaNodeInfoMap();
            String keyStr = nodeInfoMap.get("nodeEncryptKey");
            String encryptAlg = getEncryptAlgName();
            if (StringUtils.isBlank(keyStr) || StringUtils.isBlank(encryptAlg)) {
                throw new RuntimeException("Should encrypt data. but missing encrypt key or algorithm.");
            }
            this.encryptKey = CipherKey.fromKeyBytes(Hex.decode(keyStr), encryptAlg);
        } else {
            this.encryptKey = new DefaultCipherKey();
        }
        return encryptKey;
    }


    public SecureKey getMyKey() {
        if (myKey != null) {
            return myKey;
        }

        Map<String, String> caNodeInfoMap = generatedCaNodeInfoMap();
        String privateKey = caNodeInfoMap.get("nodeIdPrivateKey");
        String name = caNodeInfoMap.get("name");
        String agency = caNodeInfoMap.get("agency");
        String caHash = caNodeInfoMap.get("caHash");

        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(agency) || StringUtils.isEmpty(caHash)) {
            throw new RuntimeException("be short of ca node info!");
        }

        this.myKey = SecureKey.fromPrivate(Hex.decode(privateKey));
        return myKey;
    }

    public void setMyKey(SecureKey myKey) {
        this.myKey = myKey;
    }

    /**
     * Home NodeID calculated from 'peer.privateKey' property
     */
    public byte[] getNodeId() {
        if (this.nodeId == null) {
            this.nodeId = getMyKey().getNodeId();
            //this.nodeId = getMyKey().doGetPubKey();
        }
        return this.nodeId;
    }


    @ValidateMe
    public boolean transferDataEncrypt() {
        if (transferDataEncrypt == null) {

            transferDataEncrypt = config.hasPath("network.transferDataEncrypt") ? config.getInt("network.transferDataEncrypt") == 1 : false;
        }


        return transferDataEncrypt;
    }

    @ValidateMe
    public int listenDiscoveryPortPort() {
        return config.getInt("network.peer.listen.discoveryPort");
    }

    @ValidateMe
    public int listenRpcPort() {
        return config.getInt("network.peer.listen.rpcPort");
    }

    @ValidateMe
    public Integer getGatewayLocalListenAddress() {
        return config.getInt("network.gateway.localListenAddress");
    }

    @ValidateMe
    public String getGatewayRemoteServiceAddress() {
        return config.getString("network.gateway.remoteServiceAddress");
    }

    @ValidateMe
    public Integer getPushTxsQueueSize() {
        return config.getInt("network.gateway.pushTxsQueueSize");
    }

    @ValidateMe
    public int getHandleReqProcessNum() {
        return config.hasPath("network.gateway.handleReqProcessNum") ? config.getInt("network.gateway.handleReqProcessNum") : 256;
    }


    /**
     * This can be a blocking call with long timeout (thus no ValidateMe)
     */
    public String bindIp() {
        if (!config.hasPath("network.peer.bind.ip") || config.getString("network.peer.bind.ip").trim().isEmpty()) {
            if (bindIp == null) {
                //logger.info("External IP wasn't set, using checkip.amazonaws.com to identify it...");
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            new URL("http://checkip.amazonaws.com").openStream()));
                    bindIp = in.readLine();
                    if (bindIp == null || bindIp.trim().isEmpty()) {
                        throw new IOException("Invalid address: '" + bindIp + "'");
                    }
                    try {
                        InetAddress.getByName(bindIp);
                    } catch (Exception e) {
                        throw new IOException("Invalid address: '" + bindIp + "'");
                    }
                    //logger.info("External address identified: {}", bindIp);
                } catch (IOException e) {
                    bindIp = "0.0.0.0";
                    //logger.warn("Can't get bindIp IP. Fall back to peer.bind.ip: " + bindIp + " :" + e);
                }
            }
            return bindIp;

        } else {
            return config.getString("network.peer.bind.ip").trim();
        }
    }

    public String rpcIp() {

        return config.getString("network.peer.rpc.ip").trim();

    }

    @ValidateMe
    public boolean epollSupport() {

        return config.hasPath("network.epollSupport") ? config.getBoolean("network.epollSupport") : false;

    }

    @ValidateMe
    public boolean nettyPoolByteBuf() {
        return config.hasPath("network.nettyPoolByteBuf") ? config.getBoolean("network.nettyPoolByteBuf") : false;

    }

    @ValidateMe
    public boolean dataNeedEncrypt() {
        if (persistNeedEncrypt == null) {
            persistNeedEncrypt = config.hasPath("resource.database.needEncrypt") ? config.getBoolean("resource.database.needEncrypt") : false;
        } else {
            return persistNeedEncrypt;
        }

        return persistNeedEncrypt;
    }

    public String getEncryptAlgName() {
        return config.getString("resource.database.encryptAlg");
    }


    //--------------start db--------------
    public int getLedgerMaxOpenFiles() {
        return config.hasPath("db.ledger.maxOpenFiles") ? config.getInt("db.ledger.maxOpenFiles") : 1000;
    }

    public int getLedgerMaxThreads() {
        return config.hasPath("db.ledger.maxThreads") ? config.getInt("db.ledger.maxThreads") : 4;
    }

    public int getLedgerWriteBufferSize() {
        return config.hasPath("db.ledger.writeBufferSize") ? config.getInt("db.ledger.writeBufferSize") : 64;
    }

    public int getLedgerContractStateCacheSize() {
        return config.hasPath("db.ledger.contractStateCacheSize") ? config.getInt("db.ledger.contractStateCacheSize") : 100000;
    }


    public int getConsensusChainCaContractStateCacheSize() {
        return config.hasPath("db.ledger.caContractStateCacheSize") ? config.getInt("db.ledger.contractStateCacheSize") : 1000;
    }

    //-----------

    public int getConsensusChainMaxOpenFiles() {
        return config.hasPath("db.consensusChain.maxOpenFiles") ? config.getInt("db.ledger.maxOpenFiles") : 1000;
    }

    public int getConsensusChainMaxThreads() {
        return config.hasPath("db.consensusChain.maxThreads") ? config.getInt("db.ledger.maxThreads") : 4;
    }

    public int getConsensusChainWriteBufferSize() {
        return config.hasPath("db.consensusChain.writeBufferSize") ? config.getInt("db.ledger.writeBufferSize") : 64;
    }

    public boolean getConsensusChainBloomFilterFlag() {
        return config.hasPath("db.consensusChain.bloomFilterFlag") ? config.getBoolean("db.ledger.bloomFilterFlag") : true;
    }
    //------------------------

    public int getConsensusMaxOpenFiles() {
        return config.hasPath("db.consensus.maxOpenFiles") ? config.getInt("db.consensus.maxOpenFiles") : 1000;
    }

    public int getConsensusMaxThreads() {
        return config.hasPath("db.consensus.maxThreads") ? config.getInt("db.consensus.maxThreads") : 4;
    }

    public int getConsensusWriteBufferSize() {
        return config.hasPath("db.consensus.writeBufferSize") ? config.getInt("db.consensus.writeBufferSize") : 64;
    }

    //-----------

    public int getLedgerIndexMaxOpenFiles() {
        return config.hasPath("db.ledgerIndex.maxOpenFiles") ? config.getInt("db.ledgerIndex.maxOpenFiles") : 1000;
    }

    public int getLedgerIndexMaxThreads() {
        return config.hasPath("db.ledgerIndex.maxThreads") ? config.getInt("db.ledgerIndex.maxThreads") : 4;
    }

    public int getLedgerIndexWriteBufferSize() {
        return config.hasPath("db.ledgerIndex.writeBufferSize") ? config.getInt("db.ledgerIndex.writeBufferSize") : 64;
    }


    //--------------end   db--------------


    @ValidateMe
    public String getCertsPath() {
        return config.getString("tls.certsPath");
    }

    @ValidateMe
    public String getKeyPath() {
        return config.getString("tls.keyPath");
    }

//    public String getKeyStorePath() {
//        return config.getString("tls.keystorePath");
//    }
//
//    public String getKeyStorePwd() {
//        return config.getString("tls.keystorePwd");
//    }
//
//    public String getTrustStorePath() {
//        return config.getString("tls.truststorePath");
//    }
//
//    public String getTrustStorePwd() {
//        return config.getString("tls.truststorePwd");
//    }

//    @ValidateMe
//    public String getCryptoProviderName() {
//        return config.getString("crypto.providerName");
//    }
//
//    @ValidateMe
//    public String getHash256AlgName() {
//        return config.getString("crypto.hash.alg256");
//    }
//
//    @ValidateMe
//    public String getHash512AlgName() {
//        return config.getString("crypto.hash.alg512");
//    }

//    @ValidateMe
//    public List<BootNode> getBootNodes() {
//        if (this.activePeerList == null) {
//            activePeerList = peerActive();
//        }
//        return activePeerList;
//    }

//    private List<BootNode> peerActive() {
//        if (!config.hasPath("peer.active")) {
//            return Collections.EMPTY_LIST;
//        }
//        List<BootNode> ret = new ArrayList<>();
//        List<? extends ConfigObject> list = config.getObjectList("peer.active");
//        for (ConfigObject configObject : list) {
//            Node n;
//            if (configObject.get("url") != null) {
//                String url = configObject.toConfig().getString("url");
//                n = new Node(url.startsWith("enode://") ? url : "enode://" + url);
//            } else if (configObject.get("ip") != null) {
//                String ip = configObject.toConfig().getString("ip");
//                int port = configObject.toConfig().getInt("port");
//                byte[] nodeId;
//                if (configObject.toConfig().hasPath("nodeId")) {
//                    nodeId = Hex.decode(configObject.toConfig().getString("nodeId").trim());
//                    if (nodeId.length != 64) {
//                        throw new RuntimeException("Invalid config nodeId '" + nodeId + "' at " + configObject);
//                    }
//                } else {
//                    if (configObject.toConfig().hasPath("nodeName")) {
//                        String nodeName = configObject.toConfig().getString("nodeName").trim();
//                        // FIXME should be keccak-512 here ?
//                        nodeId = ECKey.fromPrivate(sha3(nodeName.getBytes())).getCaHash();
//                    } else {
//                        throw new RuntimeException("Either nodeId or nodeName should be specified: " + configObject);
//                    }
//                }
//                n = new Node(nodeId, ip, port);
//            } else {
//                throw new RuntimeException("Unexpected element within 'peer.active' config list: " + configObject);
//            }

//            String ip = configObject.toConfig().getString("ip");
//            int port = configObject.toConfig().getInt("port");
//            BootNode bootNode = new BootNode(ip, port);
//            ret.add(bootNode);
//
//        }
//        return ret;
//    }

    public GenesisJson getGenesisJson() {
        if (genesisJson == null) {
            genesisJson = GenesisLoader.loadGenesisJson(this, classLoader);
        }
        return genesisJson;
    }

    public Genesis getGenesis() {
        if (genesis == null) {
            genesis = GenesisLoader.parseGenesis(getGenesisJson());
        }
        return genesis;
    }

    public static void main(String[] args) {

        SystemConfig config = new SystemConfig();

        System.out.println("nodeIdPrivateKey:" + Hex.toHexString(config.getMyKey().getPrivKeyBytes()));
        System.out.println("nodeId:" + Hex.toHexString(config.getMyKey().getNodeId()));
        System.out.println("publicKey:" + Hex.toHexString(config.getMyKey().getPubKey()));
        System.out.println("publicKey recovery from:" + SecurePublicKey.generate(config.getMyKey().getPubKey()).toString());
        System.out.println("address:" + Hex.toHexString(config.getMyKey().getAddress()));
        System.out.println("----------------------------------------");


        SecureKey key1 = SecureKey.getInstance("ECDSA", 1);

        System.out.println("nodeIdPrivateKey:" + Hex.toHexString(key1.getPrivKeyBytes()));
        System.out.println("nodeId:" + Hex.toHexString(key1.getNodeId()));
        System.out.println("pk:" + Hex.toHexString(key1.getPubKey()));
        System.out.println("----------------------------------------");
        System.out.println("pk length:" + key1.getPubKey().length);

        SecureKey key2 = SecureKey.getInstance("ECDSA", 1);

        System.out.println("nodeIdPrivateKey:" + Hex.toHexString(key2.getPrivKeyBytes()));
        System.out.println("nodeId:" + Hex.toHexString(key2.getNodeId()));
        System.out.println("pk:" + Hex.toHexString(key2.getPubKey()));
        System.out.println("----------------------------------------");

        SecureKey key3 = SecureKey.getInstance("ECDSA", 1);

        System.out.println("nodeIdPrivateKey:" + Hex.toHexString(key3.getPrivKeyBytes()));
        System.out.println("nodeId:" + Hex.toHexString(key3.getNodeId()));
        System.out.println("pk:" + Hex.toHexString(key3.getPubKey()));
        System.out.println("----------------------------------------");


        SecureKey key4 = SecureKey.fromPrivate(Hex.decode("bde09e67207daa157f510742ea77b7896782729c4621a0884fd018a1ee298fbe"));
        System.out.println("nodeIdPrivateKey:" + Hex.toHexString(key4.getPrivKeyBytes()));
        System.out.println("nodeId:" + Hex.toHexString(key4.getNodeId()));
        System.out.println("pk:" + Hex.toHexString(key4.getPubKey()));
        System.out.println("----------------------------------------");
    }

}

