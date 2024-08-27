//package com.thanos.chain.contract.ca.filter.impl;
//
//import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
//import com.thanos.chain.contract.ca.filter.AbstractGlobalFilter;
//import com.thanos.chain.ledger.model.eth.EthTransaction;
//import com.thanos.chain.ledger.model.event.ca.CaContractStateValue;
//import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
//import com.thanos.common.utils.ByteUtil;
//import org.spongycastle.util.encoders.Hex;
//
///**
// * InvokeEthContractAuthFilter.java description：
// *
// * @Author laiyiyu create on 2021-04-12 10:10:19
// */
//public class InvokeEthContractAuthFilter extends AbstractGlobalFilter {
//
//    //private static final Logger logger = LoggerFactory.getLogger("ca");
//
//    private static  byte[] BLACK_KEY = "BLACK_KEY".getBytes();
//
//    private static  byte[] CONTRACT_INVOKE_BLACK_KEY = "CONTRACT_INVOKE_BLACK_KEY".getBytes();
//
//    private static  byte[] DEPLOY_BLACK_KEY_MAP_PREFIX = "DEPLOY_BLACK_KEY_MAP_PREFIX".getBytes();
//
//    private static  byte[] DEPLOY_WHITE_KEY_MAP_PREFIX = "DEPLOY_WHITE_KEY_MAP_PREFIX".getBytes();
//
//    private static  byte[] INVOKE_BLACK_KEY_MAP_PREFIX = "INVOKE_BLACK_KEY_MAP_PREFIX".getBytes();
//
//    private static  byte[] INVOKE_WHITE_KEY_MAP_PREFIX = "INVOKE_WHITE_KEY_MAP_PREFIX".getBytes();
//
//    private static  byte[] ENABLE_DEPLOY_KEY = "ENABLE_DEPLOY_KEY".getBytes();
//
//    private static  byte[] ENABLE_INVOKE_KEY = "ENABLE_INVOKE_KEY".getBytes();
//
//    //default: false
//    volatile boolean black;
//
//    volatile boolean enableDeploy;
//
//    volatile boolean enableInvoke;
//
//
//    public InvokeEthContractAuthFilter(byte[] address, GlobalStateRepositoryImpl stateRepository) {
//        super(address, stateRepository);
//        this.refreshState();
//    }
//
//    @Override
//    protected ProcessResult doDeployFilter(EthTransaction ethTransaction) {
//        if (!enableDeploy) return ProcessResult.SUCCESSFUL;
//
//        if (black) {
//            //黑名单
//            byte[] userKey = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, ethTransaction.getSender());
//            CaContractStateValue user = getCaContractStateValue(userKey);
//            if (user == null) {
//                return ProcessResult.SUCCESSFUL;
//            } else {
//                String error = String.format("black mode, user[%s] do not has deploy authority!", Hex.toHexString(ethTransaction.getSender()));
//                return ProcessResult.ofError(error);
//            }
//
//        } else {
//            //白名单
//            if (isAuthManager(ethTransaction.getSender())) {
//                return ProcessResult.SUCCESSFUL;
//            }
//
//            byte[] userKey = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, ethTransaction.getSender());
//            CaContractStateValue user = getCaContractStateValue(userKey);
//            if (user != null) {
//                return ProcessResult.SUCCESSFUL;
//            } else {
//
//                String error = String.format("white mode, user[%s] do not has deploy authority!", Hex.toHexString(ethTransaction.getSender()));
//                return ProcessResult.ofError(error);
//            }
//        }
//
//    }
//
//    @Override
//    protected ProcessResult doInvokeFilter(EthTransaction ethTransaction) {
//        if (!enableInvoke) return ProcessResult.SUCCESSFUL;
//
//
//        //logger.info("doInvokeFilter will check!!!!!!!");
//
//        byte[] contractInvokeBlackKey = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, ethTransaction.getReceiveAddress());
//        CaContractStateValue contractInvokeBlack = getCaContractStateValue(contractInvokeBlackKey);
//        boolean contactBlack;
//        if (contractInvokeBlack != null) {
//            contactBlack = ByteUtil.byteArrayToInt(contractInvokeBlack.valueBytes()) == 1? true : false;
//        } else {
//            contactBlack = true;
//        }
//
//        if (contactBlack) {
//            //logger.info("contactBlack is true");
//            //黑名单
//            byte[] userKey = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;
//            CaContractStateValue user = getCaContractStateValue(userKey);
//            if (user == null) {
//                return ProcessResult.SUCCESSFUL;
//            } else {
//                String error = String.format("black mode, user[%s] do not has contract[%s] invoke authority!", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));
//                return ProcessResult.ofError(error);
//            }
//
//        } else {
//            //logger.info("contactBlack is false");
//            //白名单
//
//            byte[] userKey = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, ethTransaction.getReceiveAddress(), ethTransaction.getSender());;
//            CaContractStateValue user = getCaContractStateValue(userKey);
//            if (user != null) {
//                return ProcessResult.SUCCESSFUL;
//            } else {
//                String error = String.format("white mode, user[%s] do not has contract[%s] invoke authority!", Hex.toHexString(ethTransaction.getSender()), Hex.toHexString(ethTransaction.getContractAddress()));
//                return ProcessResult.ofError(error);
//            }
//        }
//
//    }
//
//
//    //===================user deploy authority=============================================
//    public void setDeployBlack(byte[] userAddress, int opcode) {
//
//        byte[] key = ByteUtil.merge(DEPLOY_BLACK_KEY_MAP_PREFIX, userAddress);
//        logger.debug("setDeployBlack [{}]-[{}]", opcode, Hex.toHexString(userAddress));
//        if (opcode == 0) {
//            writeCaContractStateValue(key, key);
//        } else {
//            deleteCaContractStateValue(key);
//        }
//
//    }
//
//    public void setDeployWhite(byte[] userAddress, int opcode) {
//        byte[] key = ByteUtil.merge(DEPLOY_WHITE_KEY_MAP_PREFIX, userAddress);
//        if (opcode == 0) {
//            writeCaContractStateValue(key, key);
//        } else {
//            deleteCaContractStateValue(key);
//        }
//    }
//
//    //===================user invoke authority=============================================
//    public void setInvokeBlack(byte[] contractAddress, byte[] userAddress, int opcode) {
//        logger.debug("setInvokeBlack [{}-{}]-[{}]", opcode, Hex.toHexString(contractAddress), Hex.toHexString(userAddress));
//        byte[] key = ByteUtil.merge(INVOKE_BLACK_KEY_MAP_PREFIX, contractAddress, userAddress);
//        if (opcode == 0) {
//            //do add
//            writeCaContractStateValue(key, userAddress);
//        } else {
//            //do delete
//            deleteCaContractStateValue(key);
//        }
//    }
//
//    public void setInvokeWhite(byte[] contractAddress, byte[] userAddress, int opcode) {
//        byte[] key = ByteUtil.merge(INVOKE_WHITE_KEY_MAP_PREFIX, contractAddress, userAddress);
//        if (opcode == 0) {
//            //do add
//            writeCaContractStateValue(key, userAddress);
//        } else {
//            //do delete
//            deleteCaContractStateValue(key);
//        }
//
//    }
//
//
//    //====================filter attribute============================================
//    public void setBlack(boolean flag) {
//        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);
//        writeCaContractStateValue(BLACK_KEY, value);
//    }
//
//    public void setContractInvokeBlack(byte[] addr, boolean flag) {
//        logger.debug("setContractInvokeBlack [{}]-[{}]", flag, Hex.toHexString(addr));
//        byte[] key = ByteUtil.merge(CONTRACT_INVOKE_BLACK_KEY, addr);
//        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);
//        writeCaContractStateValue(key, value);
//    }
//
//
//    public void setEnableDeploy(boolean flag) {
//        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);
//        writeCaContractStateValue(ENABLE_DEPLOY_KEY, value);
//    }
//
//    public void setEnableInvoke(boolean flag) {
//        byte[] value = flag? ByteUtil.intToBytes(1): ByteUtil.intToBytes(0);
//        writeCaContractStateValue(ENABLE_INVOKE_KEY, value);
//    }
//
//
//
//    @Override
//    public void refreshState() {
//        CaContractStateValue blackValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);
//        if (blackValue != null) {
//            byte[] blackBytes = blackValue.valueBytes();
//            this.black = ByteUtil.byteArrayToInt(blackBytes) == 1? true : false;
//        } else {
//            black = false;
//        }
//
//        CaContractStateValue enableDeployValue = getCaContractStateValue(ENABLE_DEPLOY_KEY);
//        if (enableDeployValue != null) {
//            byte[] enableDeployBytes = enableDeployValue.valueBytes();
//            this.enableDeploy = ByteUtil.byteArrayToInt(enableDeployBytes) == 1? true : false;
//        } else {
//            enableDeploy = true;
//        }
//
//        CaContractStateValue enableInvokeValue = getCaContractStateValue(ENABLE_INVOKE_KEY);
//        if (enableInvokeValue != null) {
//            byte[] enableInvokeBytes = getCaContractStateValue(ENABLE_INVOKE_KEY).valueBytes();
//            this.enableInvoke = ByteUtil.byteArrayToInt(enableInvokeBytes) == 1? true : false;
//        } else {
//            this.enableInvoke = false;
//        }
//
//        logger.debug("InvokerEthContractAuthFilter refreshState, black[{}], enableDeploy[{}], enableInvoke[{}]", black, this.enableDeploy, this.enableInvoke);
//    }
//}
