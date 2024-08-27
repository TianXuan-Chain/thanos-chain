package com.thanos.chain.contract.ca.resolver;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thanos.chain.consensus.hotstuffbft.model.ProcessResult;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

/**
 * DependencyChecker.java description：
 *
 * @Author laiyiyu create on 2021-04-21 17:22:31
 */
public class DependencyChecker {

    public static final String reg = "(L.+?;)";

    public static final Set<String> blackClassMap = new HashSet<>();
    public static final Set<String> blackPackageMap = new HashSet<>();

    static {

        blackPackageMap.add("java.lang.reflect");
        blackPackageMap.add("java.io");
        blackPackageMap.add("java.nio");
        blackPackageMap.add("java.net");
        blackPackageMap.add("java.sql");

        blackClassMap.add("java.lang.System");
        blackClassMap.add("java.lang.Class");
        blackClassMap.add("java.util.Random");
        blackClassMap.add("java.util.Random");
    }

    //private final String clazz;

    public DependencyChecker() {
    }



//    public static ProcessResult doCheck(String clazz) {
//        ScanResult scanResult = new ClassGraph()
//                .acceptPackages("com.thanos.chain.contract.ca.resolver")
//                .enableInterClassDependencies()
//                .enableSystemJarsAndModules()
//                .scan();
//
//        //System.out.println("cac1:" + Thread.currentThread().getContextClassLoader());
//
//
//        ClassInfo rootClass = scanResult.getClassInfo(clazz);
//
//
//        ClassInfoList classInfoList = rootClass.getClassDependencies();
//        for (ClassInfo temp: classInfoList) {
//            System.out.println(temp.getName());
//
//            if (blackClassMap.contains(temp.getName())) {
//                return ProcessResult.buildFail(clazz + " dependencies the is black name class " + temp.getName());
//            }
//
//
//        }
//
//
//        return ProcessResult.buildSuccess();
//
//
//    }


//    /**
//     * 查看某个类引用的所有类
//     * @param clazz
//     * @param set
//     * @throws NotFoundException
//     */
//    public static void bfs(String clazz, Set<String> set) throws NotFoundException {
//        //ClassPool.getDefault().importPackage("com.thanos.chain.contract.ca.filter.impl");
//        //ClassFile classfile =
//        //ClassPool.getDefault().makeClass(clazz);
//        //System.out.println(clazz);
//        CtClass ctClass = ClassPool.getDefault().get(clazz);
//        // 当前类直接依赖的类
//        Set<String> levelClasses = new TreeSet<>();
//        ClassFile classFile = ctClass.getClassFile();
//
//        /**
//         * 遍历代码内使用的类，包含方法实现里使用的类，不包含方法签名里的类
//         */
//        for (Object objName : classFile.getConstPool().getClassNames()) {
//            String className = (String) objName;
//            if (className.startsWith("[L")) {
//                className = className.substring(2, className.length() - 1);
//            } else if (className.startsWith("[")) {
//                continue;
//            }
//            className = getClassName(className);
//            addClassName(set, levelClasses, className);
//        }
//
//        /**
//         * 获取父类
//         */
//        String superClass = classFile.getSuperclass();
//        if (!"".equals(superClass) && superClass != null && !set.contains(superClass)) {
//            levelClasses.add(superClass);
//            set.add(superClass);
//        }
//
//        /**
//         * 获取所有接口
//         */
//        String[] interfaces = classFile.getInterfaces();
//        if (interfaces != null) {
//            for (String face : interfaces) {
//                String className = getClassName(face);
//                addClassName(set, levelClasses, className);
//            }
//        }
//
//        /**
//         * 获取字段的类型
//         */
//        List<FieldInfo> fieldInfoList = classFile.getFields();
//        if (fieldInfoList != null) {
//            for (FieldInfo fieldInfo : fieldInfoList) {
//                String descriptor = fieldInfo.getDescriptor();
//                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
//                    String className = descriptor.substring(1, descriptor.length() - 1);
//                    className = getClassName(className);
//                    addClassName(set, levelClasses, className);
//                }
//
//                if (descriptor.startsWith("[L") && descriptor.endsWith(";")) {
//                    String className = descriptor.substring(2, descriptor.length() - 1);
//                    className = getClassName(className);
//                    addClassName(set, levelClasses, className);
//                }
//            }
//        }
//
//        /**
//         * 获取方法声明的参数和返回值包含的所有类
//         */
//        List<MethodInfo> methodInfoList = classFile.getMethods();
//        if (methodInfoList != null) {
//            for (MethodInfo methodInfo : methodInfoList) {
//                String descriptor = methodInfo.getDescriptor();
//                extractClassNames(descriptor, set, levelClasses);
//            }
//        }
//
//        /**
//         * 对当前类直接依赖的类，继续查寻它们依赖的其他类
//         */
//        if (!levelClasses.isEmpty()) {
//            for (String className : levelClasses) {
//                bfs(className, set);
//            }
//        }
//    }

//    private static void addClassName(Set<String> set, Set<String> levelClasses, String className) {
//        // 如果当前节点已经被访问过，不再将它添加到当前类的直接依赖中
//        if (!set.contains(className)) {
//            levelClasses.add(className);
//            set.add(className);
//        }
//    }

    private static String getClassName(String className) {
        return className.replaceAll("/", ".");
    }

//    private static void extractClassNames(String descriptor, Set<String> set, Set<String> levelClasses) {
//        String reg = "(L.+?;)";
//        Pattern pattern = Pattern.compile(reg);
//        Matcher matcher = pattern.matcher(descriptor);
//        while (matcher.find()) {
//            String className = matcher.group();
//            className = className.substring(1, className.length() - 1);
//            className = getClassName(className);
//            addClassName(set, levelClasses, className);
//        }
//    }



    public static ProcessResult doCheckDirectDependencies(String clazz) throws NotFoundException {

        CtClass ctClass = ClassPool.getDefault().get(clazz);
        // 当前类直接依赖的类
        Set<String> levelClasses = new TreeSet<>();
        ClassFile classFile = ctClass.getClassFile();

        /**
         * 遍历代码内使用的类，包含方法实现里使用的类，不包含方法签名里的类
         */
        for (Object objName : classFile.getConstPool().getClassNames()) {
            String className = (String) objName;
            if (className.startsWith("[L")) {
                className = className.substring(2, className.length() - 1);
            } else if (className.startsWith("[")) {
                continue;
            }
            className = getClassName(className);
            levelClasses.add(className);
        }

        /**
         * 获取父类
         */
        String superClass = classFile.getSuperclass();
        if (!"".equals(superClass) && superClass != null) {
            levelClasses.add(superClass);
        }

        /**
         * 获取所有接口
         */
        String[] interfaces = classFile.getInterfaces();
        if (interfaces != null) {
            for (String face : interfaces) {
                String className = getClassName(face);
                levelClasses.add(className);
            }
        }

        /**
         * 获取字段的类型
         */
        List<FieldInfo> fieldInfoList = classFile.getFields();
        if (fieldInfoList != null) {
            for (FieldInfo fieldInfo : fieldInfoList) {
                String descriptor = fieldInfo.getDescriptor();
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    String className = descriptor.substring(1, descriptor.length() - 1);
                    className = getClassName(className);
                    levelClasses.add(className);
                }

                if (descriptor.startsWith("[L") && descriptor.endsWith(";")) {
                    String className = descriptor.substring(2, descriptor.length() - 1);
                    className = getClassName(className);
                    levelClasses.add(className);
                }
            }
        }

        /**
         * 获取方法声明的参数和返回值包含的所有类
         */
        List<MethodInfo> methodInfoList = classFile.getMethods();
        if (methodInfoList != null) {
            for (MethodInfo methodInfo : methodInfoList) {
                String descriptor = methodInfo.getDescriptor();

                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(descriptor);
                while (matcher.find()) {
                    String className = matcher.group();
                    className = className.substring(1, className.length() - 1);
                    className = getClassName(className);
                    levelClasses.add(className);
                }
            }
        }

        //check back list class or package
        for (String dependencyClass: levelClasses) {
            if (blackClassMap.contains(dependencyClass)) {
                return ProcessResult.ofError(String.format("current class[%s] dependencies the black class[%s]", clazz, dependencyClass));
            }

            for (String blackPackage: blackPackageMap) {
                if (dependencyClass.startsWith(blackPackage)) {
                    return ProcessResult.ofError(String.format("current class[%s] dependencies the black package[%s] class[%s]", clazz, blackPackage, dependencyClass));
                }
            }
        }
        return ProcessResult.SUCCESSFUL;
    }

    public static void main(String[] args) {

    }
}
