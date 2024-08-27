package com.thanos.chain.contract.ca.filter;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.resolver.CaJavaCompiler;
import com.thanos.chain.contract.ca.resolver.FilterInvokeParameterResolver;
import com.thanos.chain.contract.ca.resolver.FilterInvokeResultResolver;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.storage.db.GlobalStateRepositoryImpl;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.HashUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * FilterInvoker.java description：
 *
 * @Author laiyiyu create on 2021-04-26 14:19:11
 */
public class FilterInvoker {

    private static final Logger logger = LoggerFactory.getLogger("ca");

    ByteArrayWrapper addressNamespace;

    CaContractCode caContractCode;

    CaJavaCompiler caJavaCompiler;

    GlobalStateRepositoryImpl stateRepository;

    AbstractGlobalFilter filter;

    Constructor filterContractor;

    Map<ByteArrayWrapper, Method> invokeId2MethodTable;

    public FilterInvoker(CaContractCode caContractCode, GlobalStateRepositoryImpl stateRepository) {
        this.addressNamespace = new ByteArrayWrapper(caContractCode.getCodeAddress());
        this.caContractCode = caContractCode;
        this.stateRepository = stateRepository;
        init();
    }

    private void init() {
        try {
            caJavaCompiler = new CaJavaCompiler(caContractCode.getCodeAddress());
            ProcessResult result = caJavaCompiler.compile(caContractCode);
            if (!result.isSuccess()) {
                System.exit(1);
            }

            Class filterClass = Class.forName(caContractCode.getFilterMainClassName(), true, caJavaCompiler.classLoader);
            filterContractor = filterClass.getDeclaredConstructor(new Class[]{byte[].class, GlobalStateRepositoryImpl.class});
            filterContractor.setAccessible(true);
            filter = (AbstractGlobalFilter)filterContractor.newInstance(new Object[]{caContractCode.getCodeAddress(), this.stateRepository});
            parseMethod(filterClass);
        } catch (Exception e) {
            logger.error("FilterInvoker init error:{}", ExceptionUtils.getStackTrace(e));
            //e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void parseMethod(Class filterClass) throws UnsupportedEncodingException {
        Method[] declaredMethods = filterClass.getDeclaredMethods();
        invokeId2MethodTable = new HashMap<>(declaredMethods.length);

        for(Method method: declaredMethods) {
            if (method.getModifiers() != 1) {
                //ignore the not public method
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();

            byte[] parameterArr = new byte[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterArr[i] = FilterInvokeParameterResolver.getCode(parameterTypes[i]).byteValue();
            }

            byte[] methodId = HashUtil.sha3(method.getName().getBytes("UTF-8"), parameterArr);

            logger.info("parseMethod:" + method.getName() + " id:" + Hex.toHexString(methodId));

            invokeId2MethodTable.put(new ByteArrayWrapper(methodId), method);
        }
    }

    public AbstractGlobalFilter getFilter() {
        return filter;
    }

    public void refreshFilterState() {
        if (filter != null) {
            filter.refreshState();
        }
    }

    public ProcessResult<byte[]> invoke(GlobalStateRepositoryImpl globalStateRepositoryTrack, byte[] methodId, byte[] input) {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        try {
            Method method = invokeId2MethodTable.get(new ByteArrayWrapper(methodId));
            if (method == null) {
                return ProcessResult.ofError(String.format("methodId[%s] is map a null method!", Hex.toHexString(methodId)));
            }
            Object[] parameterInput = FilterInvokeParameterResolver.ParseMethodParams(method, input);

            logger.info("invoke[{}], input{}", method.getName(), Arrays.toString(parameterInput));
            Thread.currentThread().setContextClassLoader(caJavaCompiler.classLoader);
            AbstractGlobalFilter tempInvokeFilter = (AbstractGlobalFilter)filterContractor.newInstance(new Object[]{ByteUtil.copyFrom(caContractCode.getCodeAddress()), globalStateRepositoryTrack});
            Object invokeResult = method.invoke(tempInvokeFilter, parameterInput);
            return ProcessResult.ofSuccess(FilterInvokeResultResolver.encodeResult(method.getReturnType(), invokeResult));
        } catch (Exception e) {
            return ProcessResult.ofError(e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }
}
