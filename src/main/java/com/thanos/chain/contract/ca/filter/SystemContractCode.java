package com.thanos.chain.contract.ca.filter;

import com.thanos.common.utils.Numeric;

/**
 * SystemContractCode.java description：
 *
 * @Author laiyiyu create on 2021-04-20 15:54:35
 */
public final class SystemContractCode {

    public static final byte[] INVOKE_ETH_CONTRACT_AUTH_FILTER_ADDR = Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000001");

    public static final String INVOKE_ETH_CONTRACT_AUTH_FILTER_CODE = "" +
          "package com.thanos.chain.contract.ca.filter.impl;\n" +
            "\n" +
            "import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;\n" +
            "import com.thanos.chain.contract.ca.filter.AbstractGlobalFilter;\n" +
            "import com.thanos.chain.ledger.model.eth.EthTransaction;\n" +
            "import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;\n" +
            "import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;\n" +
            "import com.thanos.common.utils.ByteUtil;\n" +
            "import org.spongycastle.util.encoders.Hex;\n" +
            "\n" +
            "/**\n" +
            " * InvokeEthContractAuthFilter.java description：\n" +
            " *\n" +
            " * @Author laiyiyu create on 2021-04-12 10:10:19\n" +
            " */\n" +
            "public class InvokeEthContractAuthFilter extends AbstractGlobalFilter {\n" +
            "\n" +
            "    //private static final Logger logger = LoggerFactory.getLogger(\"ca\");\n" +
            "\n" +
            "    private static  byte[] BLACK_KEY = \"BLACK_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] CONTRACT_INVOKE_BLACK_KEY = \"CONTRACT_INVOKE_BLACK_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_BLACK_KEY_MAP_PREFIX = \"DEPLOY_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_WHITE_KEY_MAP_PREFIX = \"DEPLOY_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_BLACK_KEY_MAP_PREFIX = \"INVOKE_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_WHITE_KEY_MAP_PREFIX = \"INVOKE_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_DEPLOY_KEY = \"ENABLE_DEPLOY_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_INVOKE_KEY = \"ENABLE_INVOKE_KEY\".getBytes();\n" +
            "\n" +
            "    //default: false\n" +
            "    volatile boolean black;\n" +
            "\n" +
            "    volatile boolean enableDeploy;\n" +
            "\n" +
            "    volatile boolean enableInvoke;\n" +
            "\n" +
            "\n" +
            "    public InvokeEthContractAuthFilter(byte[] address, GlobalStateRepositoryImpl stateRepository) {\n" +
            "        super(address, stateRepository);\n" +
            "        this.refreshState();\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doDeployFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableDeploy) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "        if (black) {\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "            //白名单\n" +
            "            if (isAuthManager(ethTransaction.getSender())) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            }\n" +
            "\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "\n" +
            "                String error = String.format(\"white mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doInvokeFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableInvoke) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "\n" +
            "        //logger.info(\"doInvokeFilter will check!!!!!!!\");\n" +
            "\n" +
            "        byte[] contractInvokeBlackKey = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, ethTransaction.getReceiveAddress());\n" +
            "        CaContractStateValue contractInvokeBlack = getCaContractStateValue(contractInvokeBlackKey);\n" +
            "        boolean contactBlack;\n" +
            "        if (contractInvokeBlack != null) {\n" +
            "            contactBlack = ByteUtil.byteArrayToInt(contractInvokeBlack.valueBytes()) == 1? true : false;\n" +
            "        } else {\n" +
            "            contactBlack = true;\n" +
            "        }\n" +
            "\n" +
            "        if (contactBlack) {\n" +
            "            //logger.info(\"contactBlack is true\");\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "            //logger.info(\"contactBlack is false\");\n" +
            "            //白名单\n" +
            "\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"white mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //===================user deploy authority=============================================\n" +
            "    public void setDeployBlack(byte[] userAddress, int opcode) {\n" +
            "\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, userAddress);\n" +
            "        logger.debug(\"setDeployBlack [{}]-[{}]\", opcode, Hex.toHexString(userAddress));\n" +
            "        if (opcode == 0) {\n" +
            "            writeCaContractStateValue(key, key);\n" +
            "        } else {\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void setDeployWhite(byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            writeCaContractStateValue(key, key);\n" +
            "        } else {\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    //===================user invoke authority=============================================\n" +
            "    public void setInvokeBlack(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        logger.debug(\"setInvokeBlack [{}-{}]-[{}]\", opcode, Hex.toHexString(contractAddress), Hex.toHexString(userAddress));\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void setInvokeWhite(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //====================filter attribute============================================\n" +
            "    public void setBlack(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(BLACK_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setContractInvokeBlack(byte[] addr, boolean flag) {\n" +
            "        logger.debug(\"setContractInvokeBlack [{}]-[{}]\", flag, Hex.toHexString(addr));\n" +
            "        byte[] key = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, addr);\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(key, value);\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    public void setEnableDeploy(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_DEPLOY_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setEnableInvoke(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_INVOKE_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "\n" +
            "    @Override\n" +
            "    public void refreshState() {\n" +
            "        CaContractStateValue blackValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (blackValue != null) {\n" +
            "            byte[] blackBytes = blackValue.valueBytes();\n" +
            "            this.black = ByteUtil.byteArrayToInt(blackBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            black = false;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableDeployValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (enableDeployValue != null) {\n" +
            "            byte[] enableDeployBytes = enableDeployValue.valueBytes();\n" +
            "            this.enableDeploy = ByteUtil.byteArrayToInt(enableDeployBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            enableDeploy = true;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableInvokeValue = getCaContractStateValue(ENABLE_INVOKE_KEY);\n" +
            "        if (enableInvokeValue != null) {\n" +
            "            byte[] enableInvokeBytes = getCaContractStateValue(ENABLE_INVOKE_KEY).valueBytes();\n" +
            "            this.enableInvoke = ByteUtil.byteArrayToInt(enableInvokeBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            this.enableInvoke = false;\n" +
            "        }\n" +
            "\n" +
            "        logger.debug(\"InvokerEthContractAuthFilter refreshState, black[{}], enableDeploy[{}], enableInvoke[{}]\", black, this.enableDeploy, this.enableInvoke);\n" +
            "    }\n" +
            "}\n";


    public static final String TEST1_1 = "" +
            "package com.thanos.chain.contract.ca.filter.impl;\n" +
            "\n" +
            "import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;\n" +
            "import com.thanos.chain.contract.ca.filter.AbstractGlobalFilter;\n" +
            "import com.thanos.chain.ledger.model.eth.EthTransaction;\n" +
            "import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;\n" +
            "import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;\n" +
            "import com.thanos.common.utils.ByteUtil;\n" +
            "import org.spongycastle.util.encoders.Hex;\n" +
            "\n" +
            "/**\n" +
            " * InvokeEthContractAuthFilter.java description：\n" +
            " *\n" +
            " * @Author laiyiyu create on 2021-04-12 10:10:19\n" +
            " */\n" +
            "public class InvokeEthContractAuthFilterTest extends AbstractGlobalFilter {\n" +
            "\n" +
            "    //private static final Logger logger = LoggerFactory.getLogger(\"ca\");\n" +
            "\n" +
            "    private static  byte[] BLACK_KEY = \"BLACK_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] CONTRACT_INVOKE_BLACK_KEY = \"CONTRACT_INVOKE_BLACK_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_BLACK_KEY_MAP_PREFIX = \"DEPLOY_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_WHITE_KEY_MAP_PREFIX = \"DEPLOY_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_BLACK_KEY_MAP_PREFIX = \"INVOKE_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_WHITE_KEY_MAP_PREFIX = \"INVOKE_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_DEPLOY_KEY = \"ENABLE_DEPLOY_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_INVOKE_KEY = \"ENABLE_INVOKE_KEY\".getBytes();\n" +
            "\n" +
            "    //default: false\n" +
            "    volatile boolean black;\n" +
            "\n" +
            "    volatile boolean enableDeploy;\n" +
            "\n" +
            "    volatile boolean enableInvoke;\n" +
            "    Helper helper;\n" +
            "\n" +
            "\n" +
            "    public InvokeEthContractAuthFilterTest(byte[] address, GlobalStateRepositoryImpl stateRepository) {\n" +
            "        super(address, stateRepository);\n" +
            "        this.refreshState();\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doDeployFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableDeploy) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "        if (black) {\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "            //白名单\n" +
            "\n" +
            "\n" +
            "            if (isAuthManager(ethTransaction.getSender())) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            }\n" +
            "\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "\n" +
            "                String error = String.format(\"white mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doInvokeFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableInvoke) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "\n" +
            "        //logger.info(\"doInvokeFilter will check!!!!!!!\");\n" +
            "\n" +
            "        byte[] contractInvokeBlackKey = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, ethTransaction.getReceiveAddress());\n" +
            "        CaContractStateValue contractInvokeBlack = getCaContractStateValue(contractInvokeBlackKey);\n" +
            "        boolean contactBlack;\n" +
            "        if (contractInvokeBlack != null) {\n" +
            "            contactBlack = ByteUtil.byteArrayToInt(contractInvokeBlack.valueBytes()) == 1? true : false;\n" +
            "        } else {\n" +
            "            contactBlack = true;\n" +
            "        }\n" +
            "\n" +
            "        if (contactBlack) {\n" +
            "            //logger.info(\"contactBlack is true\");\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "            //logger.info(\"contactBlack is false\");\n" +
            "            //白名单\n" +
            "\n" +
            "            if (isAuthManager(ethTransaction.getSender())) {\n" +
            "                //logger.info(\"doInvokeFilter will not check auth manager!!!!!!!\");\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            }\n" +
            "\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"white mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //===================user deploy authority=============================================\n" +
            "    public void setDeployBlack(byte[] userAddress, int opcode) {\n" +
            "\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, userAddress);\n" +
            "        logger.debug(\"setDeployBlack [{}]-[{}]\", opcode, Hex.toHexString(userAddress));\n" +
            "        if (opcode == 0) {\n" +
            "            writeCaContractStateValue(key, key);\n" +
            "        } else {\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void setDeployWhite(byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            writeCaContractStateValue(key, key);\n" +
            "        } else {\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    //===================user invoke authority=============================================\n" +
            "    public void setInvokeBlack(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        logger.debug(\"setInvokeBlack [{}-{}]-[{}]\", opcode, Hex.toHexString(contractAddress), Hex.toHexString(userAddress));\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void setInvokeWhite(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //====================filter attribute============================================\n" +
            "    public void setBlack(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(BLACK_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setContractInvokeBlack(byte[] addr, boolean flag) {\n" +
            "        logger.debug(\"setContractInvokeBlack [{}]-[{}]\", flag, Hex.toHexString(addr));\n" +
            "        byte[] key = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, addr);\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(key, value);\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    public void setEnableDeploy(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_DEPLOY_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setEnableInvoke(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_INVOKE_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "\n" +
            "    @Override\n" +
            "    public void refreshState() {\n" +
            "        CaContractStateValue blackValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (blackValue != null) {\n" +
            "            byte[] blackBytes = blackValue.valueBytes();\n" +
            "            this.black = ByteUtil.byteArrayToInt(blackBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            black = false;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableDeployValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (enableDeployValue != null) {\n" +
            "            byte[] enableDeployBytes = enableDeployValue.valueBytes();\n" +
            "            this.enableDeploy = ByteUtil.byteArrayToInt(enableDeployBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            enableDeploy = true;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableInvokeValue = getCaContractStateValue(ENABLE_INVOKE_KEY);\n" +
            "        if (enableInvokeValue != null) {\n" +
            "            byte[] enableInvokeBytes = getCaContractStateValue(ENABLE_INVOKE_KEY).valueBytes();\n" +
            "            this.enableInvoke = ByteUtil.byteArrayToInt(enableInvokeBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            this.enableInvoke = false;\n" +
            "        }\n" +
            "\n" +
            "        logger.debug(\"InvokerEthContractAuthFilter refreshState, black[{}], enableDeploy[{}], enableInvoke[{}]\", black, this.enableDeploy, this.enableInvoke);\n" +
            "    }\n" +
            "}\n";

    public static final String TEST1_2 = "package com.thanos.chain.contract.ca.filter.impl;\n" +
            "\n" +
            "/**\n" +
            " * Helper.java description：\n" +
            " *\n" +
            " * @Author laiyiyu create on 2021-04-21 15:58:52\n" +
            " */\n" +
            "public class Helper {\n" +
            "}\n";

    public static final String TEST2 = "" +
            "package com.thanos.chain.contract.ca.filter.impl;\n" +
            "\n" +
            "import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;\n" +
            "import com.thanos.chain.contract.ca.filter.AbstractGlobalFilter;\n" +
            "import com.thanos.chain.ledger.model.eth.EthTransaction;\n" +
            "import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;\n" +
            "import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;\n" +
            "import com.thanos.common.utils.ByteUtil;\n" +
            "import org.spongycastle.util.encoders.Hex;\n" +
            "\n" +
            "/**\n" +
            " * InvokeEthContractAuthFilter.java description：\n" +
            " *\n" +
            " * @Author laiyiyu create on 2021-04-12 10:10:19\n" +
            " */\n" +
            "public class InvokeEthContractAuthFilterTest2 extends AbstractGlobalFilter {\n" +
            "\n" +
            "    private static  byte[] BLACK_KEY = \"BLACK_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_BLACK_KEY_MAP_PREFIX = \"DEPLOY_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] DEPLOY_WHITE_KEY_MAP_PREFIX = \"DEPLOY_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_BLACK_KEY_MAP_PREFIX = \"INVOKE_BLACK_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] INVOKE_WHITE_KEY_MAP_PREFIX = \"INVOKE_WHITE_KEY_MAP_PREFIX\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_DEPLOY_KEY = \"ENABLE_DEPLOY_KEY\".getBytes();\n" +
            "\n" +
            "    private static  byte[] ENABLE_INVOKE_KEY = \"ENABLE_INVOKE_KEY\".getBytes();\n" +
            "\n" +
            "    //default: false\n" +
            "    boolean black;\n" +
            "\n" +
            "    boolean enableDeploy;\n" +
            "\n" +
            "    boolean enableInvoke;\n" +
            "\n" +
            "\n" +
            "    public InvokeEthContractAuthFilterTest2(byte[] address, GlobalStateRepositoryImpl stateRepository) {\n" +
            "        super(address, stateRepository);\n" +
            "\n" +
            "        CaContractStateValue blackValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (blackValue != null) {\n" +
            "            byte[] blackBytes = blackValue.valueBytes();\n" +
            "            this.black = ByteUtil.byteArrayToInt(blackBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            black = false;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableDeployValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);\n" +
            "        if (enableDeployValue != null) {\n" +
            "            byte[] enableDeployBytes = enableDeployValue.valueBytes();\n" +
            "            this.enableDeploy = ByteUtil.byteArrayToInt(enableDeployBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            enableDeploy = true;\n" +
            "        }\n" +
            "\n" +
            "        CaContractStateValue enableInvokeValue = getCaContractStateValue(ENABLE_INVOKE_KEY);\n" +
            "        if (enableInvokeValue != null) {\n" +
            "            byte[] enableInvokeBytes = getCaContractStateValue(ENABLE_INVOKE_KEY).valueBytes();\n" +
            "            this.enableInvoke = ByteUtil.byteArrayToInt(enableInvokeBytes) == 1? true : false;\n" +
            "        } else {\n" +
            "            this.enableInvoke = false;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doDeployFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableInvoke) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "        if (isAuthManager(ethTransaction.getSender())) {\n" +
            "            return ProcessResult.SUCCESSFUL;\n" +
            "        }\n" +
            "\n" +
            "        if (black) {\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "            //白名单\n" +
            "            byte[] userKey = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, ethTransaction.getSender());\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"white mode, user[%s] do not has deploy authority!\", Hex.toHexString(ethTransaction.getSender()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected ProcessResult doInvokeFilter(EthTransaction ethTransaction) {\n" +
            "        if (!enableInvoke) return ProcessResult.SUCCESSFUL;\n" +
            "\n" +
            "        if (isAuthManager(ethTransaction.getSender())) {\n" +
            "            return ProcessResult.SUCCESSFUL;\n" +
            "        }\n" +
            "\n" +
            "        if (black) {\n" +
            "            //黑名单\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, ethTransaction.getContractAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user == null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"black mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "\n" +
            "        } else {\n" +
            "\n" +
            "            //白名单\n" +
            "            byte[] userKey = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, ethTransaction.getContractAddress(), ethTransaction.getSender());;\n" +
            "            CaContractStateValue user = getCaContractStateValue(userKey);\n" +
            "            if (user != null) {\n" +
            "                return ProcessResult.SUCCESSFUL;\n" +
            "            } else {\n" +
            "                String error = String.format(\"white mode, user[%s] do not has contract[%s] invoke authority!\", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));\n" +
            "                return ProcessResult.ofError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //===================user deploy authority=============================================\n" +
            "    public void setDeployBlack(byte[] userAddress) {\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, userAddress);\n" +
            "        writeCaContractStateValue(key, key);\n" +
            "    }\n" +
            "\n" +
            "    public void setDeployWhite(byte[] userAddress) {\n" +
            "        byte[] key = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, userAddress);\n" +
            "        writeCaContractStateValue(key, key);\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //===================user invoke authority=============================================\n" +
            "    public void setInvokeBlack(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void setInvokeWhite(byte[] contractAddress, byte[] userAddress, int opcode) {\n" +
            "        byte[] key = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, contractAddress, userAddress);\n" +
            "        if (opcode == 0) {\n" +
            "            //do add\n" +
            "            writeCaContractStateValue(key, userAddress);\n" +
            "        } else {\n" +
            "            //do delete\n" +
            "            deleteCaContractStateValue(key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    //====================filter attribute============================================\n" +
            "    public void setBlack(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(BLACK_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setEnableDeploy(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_DEPLOY_KEY, value);\n" +
            "    }\n" +
            "\n" +
            "    public void setEnableInvoke(boolean flag) {\n" +
            "        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);\n" +
            "        writeCaContractStateValue(ENABLE_INVOKE_KEY, value);\n" +
            "    }\n" +
            "}\n";


}
