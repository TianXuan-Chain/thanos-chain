package com.thanos.chain;

import com.thanos.chain.config.ConfigResourceUtil;
import com.thanos.chain.config.SystemConfig;
import com.thanos.chain.initializer.ComponentInitializer;
import com.typesafe.config.ConfigRenderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类Main.java的实现描述：
 *
 * @Author laiyiyu create on 2019-11-27 15:27:48
 */
public class Main {

    public static void main(String[] args) {
        SystemConfig systemConfig = ConfigResourceUtil.loadSystemConfig();
        ConfigResourceUtil.loadLogConfig(systemConfig.logConfigPath());
        Logger logger = LoggerFactory.getLogger("main");
        logger.info("Config trace: " + systemConfig.config.root().render(ConfigRenderOptions.defaults().setComments(false).setJson(false)));
        ComponentInitializer.init(systemConfig);
        logger.info("Main start success!!!:");
    }
}
