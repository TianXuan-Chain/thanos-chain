package com.thanos.chain.contract.ca.resolver;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import com.thanos.chain.contract.ca.filter.AbstractGlobalFilter;
import com.thanos.chain.ledger.model.event.ca.CaContractCode;
import com.thanos.chain.ledger.model.event.ca.JavaSourceCodeEntity;
import com.thanos.common.utils.ByteArrayWrapper;
import com.thanos.common.utils.ByteUtil;
import javassist.ClassPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.concurrent.NotThreadSafe;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * CaJavaCompiler.java description：
 *
 * @Author laiyiyu create on 2021-04-15 17:15:32
 */
@NotThreadSafe
public class CaJavaCompiler {

    private static final Logger logger = LoggerFactory.getLogger("ca");

    public static final String JAVA_EXTENSION = ".java";

    private static final JavaCompiler compiler;

    private static final List<String> options;

    private static final ClassLoader parentLoader;

    static {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            logger.error("please copy jdk/lib/tools.jar to the jre/lib/tools.jar");
            System.exit(0);
        }

        options = new ArrayList<String>();
        options.add("-source");
        options.add("1.8");
        options.add("-target");
        options.add("1.8");

        parentLoader = CaJavaCompiler.class.getClassLoader();
    }


    public final DiagnosticCollector<JavaFileObject> diagnosticCollector;

    public final GlobalFilterClassLoader classLoader;

    public final GlobalFilterJavaFileManager javaFileManager;

    public final ByteArrayWrapper isolateNamespace;

    boolean encryptByteCode;

    public CaJavaCompiler(byte[] isolateNamespace) {
        this.isolateNamespace = new ByteArrayWrapper(ByteUtil.copyFrom(isolateNamespace));
        diagnosticCollector = new DiagnosticCollector<>();

        StandardJavaFileManager manager = compiler.getStandardFileManager(diagnosticCollector, null, null);

        classLoader = AccessController.doPrivileged(new PrivilegedAction<GlobalFilterClassLoader>() {
            @Override
            public GlobalFilterClassLoader run() {
                return new GlobalFilterClassLoader(parentLoader);
            }
        });
        javaFileManager = new GlobalFilterJavaFileManager(manager, classLoader);
        this.encryptByteCode = Boolean.parseBoolean(System.getProperty("encryptByteCode"));
        logger.info("CaJavaCompiler encryptByteCode:{}", this.encryptByteCode);
    }

    public ProcessResult compile(CaContractCode caContractCode) {
        if (!Arrays.equals(caContractCode.getCodeAddress(), this.isolateNamespace.getData())) {
            return ProcessResult.ofError(String.format("isolate namespace error, resolver is[%s], code is[%s]", Hex.toHexString(this.isolateNamespace.getData()), Hex.toHexString(caContractCode.getCodeAddress())));
        }

        try {
            List<JavaSourceCodeEntity> javaSourceCodeEntities = caContractCode.getJavaCodeSourceEntity();
            List<GlobalFilterJavaFileObject> javaFileObjects = new ArrayList<>(javaSourceCodeEntities.size());

            for (JavaSourceCodeEntity codeEntity : javaSourceCodeEntities) {

                String name = codeEntity.getClazzName();
                String sourceCode = codeEntity.getSourceCode();
                int i = name.lastIndexOf('.');
                String packageName = i < 0 ? "" : name.substring(0, i);
                String className = i < 0 ? name : name.substring(i + 1);
                GlobalFilterJavaFileObject javaFileObject = new GlobalFilterJavaFileObject(className, sourceCode);
                javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
                        className + JAVA_EXTENSION, javaFileObject);
                javaFileObjects.add(javaFileObject);
            }

            Boolean success = compiler.getTask(null, javaFileManager, diagnosticCollector, options,
                    null, javaFileObjects).call();
            if (success == null || !success) {


                StringBuilder errSb = new StringBuilder();
                for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                    errSb.append("error class [").
                            append(diagnostic.getSource()).
                            append("] , detail:").append(diagnostic.getMessage(Locale.ENGLISH)).append(".");
                }

                return ProcessResult.ofError("Compilation failed, detail: " + errSb.toString());
            }


            return checkClass(caContractCode);
        } catch (Throwable t) {
            StringBuilder errSb = new StringBuilder();
            for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                errSb.append("error class [").
                        append(diagnostic.getSource()).
                        append("] , detail:").append(diagnostic.getMessage(Locale.ENGLISH)).append(".");
            }
            return ProcessResult.ofError("Compilation failed: " + errSb.toString());
        }
    }

    private ProcessResult checkClass(CaContractCode caContractCode) throws Exception {
        //trigger all class load and ClassPool.getDefault().makeClass(...)
        Class mainClazz = this.classLoader.loadClass(caContractCode.getFilterMainClassName());
        for (JavaSourceCodeEntity javaSourceCodeEntity : caContractCode.getJavaCodeSourceEntity()) {
            if (!javaSourceCodeEntity.getClazzName().equals(caContractCode.getFilterMainClassName())) {
                this.classLoader.loadClass(javaSourceCodeEntity.getClazzName());
            }
        }

        if (!AbstractGlobalFilter.class.isAssignableFrom(mainClazz)) {
            String msg = String.format("the main class is not sub class of AbstractGlobalFilter");
            return ProcessResult.ofError(msg);
        }

        //for (JavaSourceCodeEntity codeEntity: caContractCode.getJavaCodeSourceEntity()) {

//            ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
//            Thread.currentThread().setContextClassLoader(this.classLoader);
//            System.out.println("cac:" + classLoader);
//            doCheck(codeEntity.getClazzName());
//            Thread.currentThread().setContextClassLoader(currentLoader);

//            Class clazz2 = Class.forName(codeEntity.getClazzName());
//            System.out.println("clazz2:" + clazz2);

        for (Method method : mainClazz.getDeclaredMethods()) {
            if (method.getModifiers() != 1) {
                //ignore the not public method
                continue;
            }

            for (Class<?> type : method.getParameterTypes()) {
                if (!FilterInvokeParameterResolver.isLegitimate(type)) {
                    String msg = String.format("[%s->%s] has illegal param type[%s]", mainClazz.getName(), method.getName(), type.getSimpleName());
                    return ProcessResult.ofError(msg);
                }
            }

            if (!FilterInvokeResultResolver.isLegitimate(method.getReturnType())) {
                String msg = String.format("[%s->%s] has illegal return type[%s]", mainClazz.getName(), method.getName(), method.getReturnType().getSimpleName());
                return ProcessResult.ofError(msg);
            }
        }

        for (JavaSourceCodeEntity codeEntity : caContractCode.getJavaCodeSourceEntity()) {
            ProcessResult checkDepRes = DependencyChecker.doCheckDirectDependencies(codeEntity.getClazzName());
            if (!checkDepRes.isSuccess()) {
                return checkDepRes;
            }
        }

        return ProcessResult.SUCCESSFUL;
    }

//
//    public Class<?> doCompile(String name, String sourceCode) throws Throwable {
//        int i = name.lastIndexOf('.');
//        String packageName = i < 0 ? "" : name.substring(0, i);
//        String className = i < 0 ? name : name.substring(i + 1);
//        GlobalFilterJavaFileObject javaFileObject = new GlobalFilterJavaFileObject(className, sourceCode);
//        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
//                className + JAVA_EXTENSION, javaFileObject);
//        Boolean result = resolver.getTask(null, javaFileManager, diagnosticCollector, options,
//                null, Arrays.asList(javaFileObject)).call();
//        if (result == null || !result) {
//            throw new IllegalStateException("Compilation failed. class: " + name + ", diagnostics: " + diagnosticCollector);
//        }
//        return classLoader.loadClass(name);
//    }

    public static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class EncryptGlobalFilterJavaFileObject extends SimpleJavaFileObject {
        //文件字节码
        private final byte[] classFileByteCodes;
        //完整类名
        private final String fullClassName;

        EncryptGlobalFilterJavaFileObject(URI fileUri, String fullClassName, byte[] byteCodes) {
            super(fileUri, Kind.CLASS);

            this.fullClassName = fullClassName;
            this.classFileByteCodes = byteCodes;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(classFileByteCodes);
        }

        public String getFullClassName() {
            return fullClassName;
        }
    }

    private static final class GlobalFilterJavaFileObject extends SimpleJavaFileObject {

        private final CharSequence source;

        private ByteArrayOutputStream bytecode;

        public GlobalFilterJavaFileObject(final String baseName, final CharSequence source) {
            super(toURI(baseName + JAVA_EXTENSION), Kind.SOURCE);
            this.source = source;
        }

        GlobalFilterJavaFileObject(final String name, final Kind kind) {
            super(toURI(name), kind);
            source = null;
        }

        public GlobalFilterJavaFileObject(URI uri, Kind kind) {
            super(uri, kind);
            source = null;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
            if (source == null) {
                throw new UnsupportedOperationException("source == null");
            }
            return source;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() {
            return bytecode = new ByteArrayOutputStream();
        }

        public byte[] getByteCode() {
            return bytecode.toByteArray();
        }
    }

    private final class GlobalFilterJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private final GlobalFilterClassLoader classLoader;

        private final Map<URI, JavaFileObject> fileObjects = new ConcurrentHashMap<URI, JavaFileObject>();

        public GlobalFilterJavaFileManager(JavaFileManager fileManager, GlobalFilterClassLoader classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {

            Iterable<JavaFileObject> javaFileObjectIterable = super.list(location, packageName, kinds, recurse);

            //在加密环境下
            if (encryptByteCode) {
                if (packageName.startsWith("com.thanos")) {

                    LinkedList<JavaFileObject> javaFileObjectLinkedList = new LinkedList<>();

                    for (JavaFileObject javaFileObject : javaFileObjectIterable) {
                        //只处理.class文件
                        if (javaFileObject.getKind().equals(JavaFileObject.Kind.CLASS)) {

                            byte[] content = ClassByteCodeDecryptor.decrypt(javaFileObject);

                            //计算文件的URI和完整类名
                            String oldUriString = javaFileObject.toUri().toString(); //父类调用返回的uri字符串，它是以类名结尾的
                            String shortClassName = oldUriString.substring(oldUriString.lastIndexOf("/") + 1); //短类名，包括.class结尾
                            String fullClassName = packageName + "." + shortClassName.replace(JavaFileObject.Kind.CLASS.extension, "");

                            URI newUri = this.uri(location, packageName, shortClassName);

                            EncryptGlobalFilterJavaFileObject compilerReadJavaFileObject = new EncryptGlobalFilterJavaFileObject(newUri, fullClassName, content);
                            javaFileObjectLinkedList.add(compilerReadJavaFileObject);
                        } else {
                            //非class文件直接返回
                            javaFileObjectLinkedList.add(javaFileObject);
                        }
                    }

                    //替换父类的返回结果
                    javaFileObjectIterable = javaFileObjectLinkedList;
                }
            }
            return javaFileObjectIterable;
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            FileObject o = fileObjects.get(uri(location, packageName, relativeName));
            if (o != null) {
                return o;
            }
            return super.getFileForInput(location, packageName, relativeName);
        }

        public void putFileForInput(StandardLocation location, String packageName, String relativeName, JavaFileObject file) {
            fileObjects.put(uri(location, packageName, relativeName), file);
        }

        private URI uri(Location location, String packageName, String relativeName) {
            return toURI(location.getName() + '/' + packageName + '/' + relativeName);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, JavaFileObject.Kind kind, FileObject outputFile)
                throws IOException {
            JavaFileObject file = new GlobalFilterJavaFileObject(qualifiedName, kind);
            classLoader.add(qualifiedName, file);
            return file;
        }

        @Override
        public ClassLoader getClassLoader(JavaFileManager.Location location) {
            return classLoader;
        }

        @Override
        public String inferBinaryName(Location loc, JavaFileObject file) {
            if (file instanceof GlobalFilterJavaFileObject) {
                return file.getName();
            } else if (file instanceof EncryptGlobalFilterJavaFileObject) {
                return ((EncryptGlobalFilterJavaFileObject) file).getFullClassName();
            }
            return super.inferBinaryName(loc, file);
        }
    }

    private static final class GlobalFilterClassLoader extends ClassLoader {

        private final Map<String, JavaFileObject> classes = new HashMap<String, JavaFileObject>();

        GlobalFilterClassLoader(final ClassLoader parentClassLoader) {
            super(parentClassLoader);
        }

        @Override
        protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
            JavaFileObject file = classes.get(qualifiedClassName);
            if (file != null) {

                try {
                    ClassPool.getDefault().makeClass(((GlobalFilterJavaFileObject) file).openInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                byte[] bytes = ((GlobalFilterJavaFileObject) file).getByteCode();
                return defineClass(qualifiedClassName, bytes, 0, bytes.length);
            }
            try {
                return getClass().getClassLoader().loadClass(qualifiedClassName);
            } catch (ClassNotFoundException nf) {
                return super.findClass(qualifiedClassName);
            }
        }

        void add(final String qualifiedClassName, final JavaFileObject javaFile) {
            classes.put(qualifiedClassName, javaFile);
        }

        @Override
        protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
    }
}
