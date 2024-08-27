package com.thanos.chain.contract.ca.resolver;

import com.thanos.common.utils.ByteUtil;
import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

/**
 * FilterInvokeParameterResolver.java description：
 *
 * @Author laiyiyu create on 2021-04-23 17:34:10
 */
public class FilterInvokeParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger("ca");

    public static final byte[] EMPTY_ARR = new byte[]{-64};

    static abstract class ParameterDecoder<T> {

        public abstract T decode(byte[] input);
    }

    //=======================

    static class ByteDecoder extends ParameterDecoder<Byte> {

        @Override
        public Byte decode(byte[] input) {
            return Byte.valueOf((byte) ByteUtil.byteArrayToInt(input));
        }
    }

    static class PrimitiveByteArrayDecoder extends ParameterDecoder<byte[]> {

        @Override
        public byte[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }
            return input;
        }
    }

    static class ByteArrayDecoder extends ParameterDecoder<Byte[]> {

        @Override
        public Byte[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            Byte[] result = new Byte[input.length];
            for (int i = 0; i < input.length; i++) {
                result[i] = Byte.valueOf(input[i]);
            }
            return result;
        }
    }

    //=======================

    static class ShortDecoder extends ParameterDecoder<Short> {
        @Override
        public Short decode(byte[] input) {
            return Short.valueOf(ByteUtil.byteArrayToShort(input));
        }
    }

    static class ShortArrayDecoder extends ParameterDecoder<Short[]> {
        @Override
        public Short[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            Short[] result = new Short[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Short.valueOf(ByteUtil.byteArrayToShort(inputList.get(i).getRLPData()));
            }

            return result;
        }
    }

    static class PrimitiveShortArrayDecoder extends ParameterDecoder<short[]> {
        @Override
        public short[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            short[] result = new short[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = ByteUtil.byteArrayToShort(inputList.get(i).getRLPData());
            }

            return result;
        }
    }

    //=======================

    static class IntegerDecoder extends ParameterDecoder<Integer> {
        @Override
        public Integer decode(byte[] input) {
            return Integer.valueOf(ByteUtil.byteArrayToInt(input));
        }
    }

    static class IntegerArrayDecoder extends ParameterDecoder<Integer[]> {
        @Override
        public Integer[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);
            Integer[] result = new Integer[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Integer.valueOf(ByteUtil.byteArrayToInt(inputList.get(i).getRLPData()));
            }

            return result;
        }
    }

    static class PrimitiveIntegerArrayDecoder extends ParameterDecoder<int[]> {
        @Override
        public int[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            int[] result = new int[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = ByteUtil.byteArrayToInt(inputList.get(i).getRLPData());
            }
            return result;
        }
    }

    //=======================

    static class LongDecoder extends ParameterDecoder<Long> {
        @Override
        public Long decode(byte[] input) {
            return Long.valueOf(ByteUtil.byteArrayToLong(input));
        }
    }

    static class LongArrayDecoder extends ParameterDecoder<Long[]> {
        @Override
        public Long[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            Long[] result = new Long[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Long.valueOf(ByteUtil.byteArrayToLong(inputList.get(i).getRLPData()));
            }

            return result;
        }
    }

    static class PrimitiveLongArrayDecoder extends ParameterDecoder<long[]> {
        @Override
        public long[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);
            long[] result = new long[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = ByteUtil.byteArrayToLong(inputList.get(i).getRLPData());
            }

            return result;
        }
    }

    static class FloatDecoder extends ParameterDecoder<Float> {
        @Override
        public Float decode(byte[] input) {
            return Float.parseFloat(new String(input));
        }
    }

    static class FloatArrayDecoder extends ParameterDecoder<Float[]> {
        @Override
        public Float[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            Float[] result = new Float[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Float.valueOf(Float.parseFloat(new String(inputList.get(i).getRLPData())));
            }

            return result;
        }
    }

    static class PrimitiveFloatArrayDecoder extends ParameterDecoder<float[]> {
        @Override
        public float[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            float[] result = new float[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Float.parseFloat(new String(inputList.get(i).getRLPData()));
            }
            return result;
        }
    }

    //=======================

    static class DoubleDecoder extends ParameterDecoder<Double> {
        @Override
        public Double decode(byte[] input) {
            return Double.parseDouble(new String(input));
        }
    }

    static class DoubleArrayDecoder extends ParameterDecoder<Double[]> {
        @Override
        public Double[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            Double[] result = new Double[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Double.valueOf(Double.parseDouble(new String(inputList.get(i).getRLPData())));
            }

            return result;
        }
    }

    static class PrimitiveDoubleArrayDecoder extends ParameterDecoder<double[]> {
        @Override
        public double[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);

            double[] result = new double[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Double.parseDouble(new String(inputList.get(i).getRLPData()));
            }

            return result;
        }
    }

    //=======================


    static class BooleanDecoder extends ParameterDecoder<Boolean> {
        @Override
        public Boolean decode(byte[] input) {
            Boolean flag = ByteUtil.byteArrayToInt(input) == 1? true: false;
            return flag;
        }
    }

    static class BooleanArrayDecoder extends ParameterDecoder<Boolean[]> {
        @Override
        public Boolean[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);
            Boolean[] result = new Boolean[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = ByteUtil.byteArrayToInt(inputList.get(i).getRLPData()) == 1? true: false;
            }

            return result;
        }
    }

    static class PrimitiveBooleanArrayDecoder extends ParameterDecoder<boolean[]> {
        @Override
        public boolean[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);
            boolean[] result = new boolean[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                result[i] = Boolean.valueOf(ByteUtil.byteArrayToInt(inputList.get(i).getRLPData()) == 1? true: false);
            }

            return result;
        }
    }

    //=======================


    static class CharDecoder extends ParameterDecoder<Character> {
        @Override
        public Character decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            try {
                String charStr = new String(input, "UTF-8");
                if (charStr.length() != 1) {
                    throw new RuntimeException("illegal str length!");
                }

                return charStr.charAt(0);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class CharArrayDecoder extends ParameterDecoder<char[]> {
        @Override
        public char[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            try {
                String charStrArr = new String(input, "UTF-8");
                //System.out.println("CharacterArrayDecoder value:" + charStrArr);
                return charStrArr.toCharArray();

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class CharacterArrayDecoder extends ParameterDecoder<Character[]> {
        @Override
        public Character[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            try {
                String charStrArr = new String(input, "UTF-8");
                //System.out.println("CharacterArrayDecoder value:" + charStrArr);

                char[] tempArr = charStrArr.toCharArray();
                Character[] result = new Character[tempArr.length];
                for (int i = 0; i < tempArr.length; i++) {
                    result[i] = Character.valueOf(tempArr[i]);
                }

                return result;
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }


    //=======================


    static class StringDecoder extends ParameterDecoder<String> {
        @Override
        public String decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            try {
                return new String(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class StringArrayDecoder extends ParameterDecoder<String[]> {
        @Override
        public String[] decode(byte[] input) {
            if (input == null) {
                throw new RuntimeException("input is null!");
            }

            if (Arrays.equals(input, EMPTY_ARR)) {
                throw new RuntimeException("input is null!");
            }

            RLPList inputList = (RLPList)RLP.decode2(input).get(0);
            String[] result = new String[inputList.size()];
            for (int i = 0; i < inputList.size(); i++) {
                try {
                    result[i] = new String(inputList.get(i).getRLPData(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            return result;
        }
    }

    private static final Map<Class<?>, ParameterDecoder> BASE_TYPE_PARSER = new HashMap<>();
    //private static final Map<Integer, Class<?>> CODE_2_TYPE_TABLE = new HashMap<>();
    private static final Map<Class<?>, Integer> TYPE_2_CODE_TABLE = new HashMap<>();

    static {
        BASE_TYPE_PARSER.put(byte.class, new ByteDecoder());
        BASE_TYPE_PARSER.put(Byte.class, new ByteDecoder());
        BASE_TYPE_PARSER.put(byte[].class, new PrimitiveByteArrayDecoder());
        BASE_TYPE_PARSER.put(Byte[].class, new ByteArrayDecoder());

        BASE_TYPE_PARSER.put(short.class, new ShortDecoder());
        BASE_TYPE_PARSER.put(Short.class, new ShortDecoder());
        BASE_TYPE_PARSER.put(short[].class, new PrimitiveShortArrayDecoder());
        BASE_TYPE_PARSER.put(Short[].class, new ShortArrayDecoder());

        BASE_TYPE_PARSER.put(int.class, new IntegerDecoder());
        BASE_TYPE_PARSER.put(Integer.class, new IntegerDecoder());
        BASE_TYPE_PARSER.put(int[].class, new PrimitiveIntegerArrayDecoder());
        BASE_TYPE_PARSER.put(Integer[].class, new IntegerArrayDecoder());

        BASE_TYPE_PARSER.put(long.class, new LongDecoder());
        BASE_TYPE_PARSER.put(Long.class, new LongDecoder());
        BASE_TYPE_PARSER.put(long[].class, new PrimitiveLongArrayDecoder());
        BASE_TYPE_PARSER.put(Long[].class, new LongArrayDecoder());

        BASE_TYPE_PARSER.put(float.class, new FloatDecoder());
        BASE_TYPE_PARSER.put(Float.class, new FloatDecoder());
        BASE_TYPE_PARSER.put(float[].class, new PrimitiveFloatArrayDecoder());
        BASE_TYPE_PARSER.put(Float[].class, new FloatArrayDecoder());

        BASE_TYPE_PARSER.put(double.class, new DoubleDecoder());
        BASE_TYPE_PARSER.put(Double.class, new DoubleDecoder());
        BASE_TYPE_PARSER.put(double[].class, new PrimitiveDoubleArrayDecoder());
        BASE_TYPE_PARSER.put(Double[].class, new DoubleArrayDecoder());

        BASE_TYPE_PARSER.put(boolean.class, new BooleanDecoder());
        BASE_TYPE_PARSER.put(Boolean.class, new BooleanDecoder());
        BASE_TYPE_PARSER.put(boolean[].class, new PrimitiveBooleanArrayDecoder());
        BASE_TYPE_PARSER.put(Boolean[].class, new BooleanArrayDecoder());

        BASE_TYPE_PARSER.put(char.class, new CharDecoder());
        BASE_TYPE_PARSER.put(Character.class, new CharDecoder());
        BASE_TYPE_PARSER.put(char[].class, new CharArrayDecoder());
        BASE_TYPE_PARSER.put(Character[].class, new CharacterArrayDecoder());


        BASE_TYPE_PARSER.put(String.class, new StringDecoder());
        BASE_TYPE_PARSER.put(String[].class, new StringArrayDecoder());

        //========================================

//        CODE_2_TYPE_TABLE.put(1, byte.class);
//        CODE_2_TYPE_TABLE.put(2, Byte.class);
//        CODE_2_TYPE_TABLE.put(3, byte[].class);
//        CODE_2_TYPE_TABLE.put(4, Byte[].class);
//
//        CODE_2_TYPE_TABLE.put(5, short.class);
//        CODE_2_TYPE_TABLE.put(6, Short.class);
//        CODE_2_TYPE_TABLE.put(7, short[].class);
//        CODE_2_TYPE_TABLE.put(8, Short[].class);
//
//        CODE_2_TYPE_TABLE.put(9, int.class);
//        CODE_2_TYPE_TABLE.put(10, Integer.class);
//        CODE_2_TYPE_TABLE.put(11, int[].class);
//        CODE_2_TYPE_TABLE.put(12, Integer[].class);
//
//        CODE_2_TYPE_TABLE.put(13, long.class);
//        CODE_2_TYPE_TABLE.put(14, Long.class);
//        CODE_2_TYPE_TABLE.put(15, long[].class);
//        CODE_2_TYPE_TABLE.put(16, Long[].class);
//
//        CODE_2_TYPE_TABLE.put(17, float.class);
//        CODE_2_TYPE_TABLE.put(18, Float.class);
//        CODE_2_TYPE_TABLE.put(19, float[].class);
//        CODE_2_TYPE_TABLE.put(20, Float[].class);
//
//        CODE_2_TYPE_TABLE.put(21, double.class);
//        CODE_2_TYPE_TABLE.put(22, Double.class);
//        CODE_2_TYPE_TABLE.put(23, double[].class);
//        CODE_2_TYPE_TABLE.put(24, Double[].class);
//
//        CODE_2_TYPE_TABLE.put(25, boolean.class);
//        CODE_2_TYPE_TABLE.put(26, Boolean.class);
//        CODE_2_TYPE_TABLE.put(27, boolean[].class);
//        CODE_2_TYPE_TABLE.put(28, Boolean[].class);
//
//        CODE_2_TYPE_TABLE.put(29, char.class);
//        CODE_2_TYPE_TABLE.put(30, Character.class);
//        CODE_2_TYPE_TABLE.put(31, char[].class);
//        CODE_2_TYPE_TABLE.put(32, Character[].class);
//
//        CODE_2_TYPE_TABLE.put(33, String.class);
//        CODE_2_TYPE_TABLE.put(34, String[].class);


        //========================================

        TYPE_2_CODE_TABLE.put(byte.class, 1);
        TYPE_2_CODE_TABLE.put(Byte.class, 2);
        TYPE_2_CODE_TABLE.put(byte[].class, 3);
        TYPE_2_CODE_TABLE.put(Byte[].class, 4);

        TYPE_2_CODE_TABLE.put(short.class, 5);
        TYPE_2_CODE_TABLE.put(Short.class, 6);
        TYPE_2_CODE_TABLE.put(short[].class, 7);
        TYPE_2_CODE_TABLE.put(Short[].class, 8);

        TYPE_2_CODE_TABLE.put(int.class, 9);
        TYPE_2_CODE_TABLE.put(Integer.class, 10);
        TYPE_2_CODE_TABLE.put(int[].class, 11);
        TYPE_2_CODE_TABLE.put(Integer[].class, 12);

        TYPE_2_CODE_TABLE.put(long.class, 13);
        TYPE_2_CODE_TABLE.put(Long.class, 14);
        TYPE_2_CODE_TABLE.put(long[].class, 15);
        TYPE_2_CODE_TABLE.put(Long[].class, 16);

        TYPE_2_CODE_TABLE.put(float.class, 17);
        TYPE_2_CODE_TABLE.put(Float.class, 18);
        TYPE_2_CODE_TABLE.put(float[].class, 19);
        TYPE_2_CODE_TABLE.put(Float[].class, 20);

        TYPE_2_CODE_TABLE.put(double.class, 21);
        TYPE_2_CODE_TABLE.put(Double.class, 22);
        TYPE_2_CODE_TABLE.put(double[].class, 23);
        TYPE_2_CODE_TABLE.put(Double[].class, 24);

        TYPE_2_CODE_TABLE.put(boolean.class, 25);
        TYPE_2_CODE_TABLE.put(Boolean.class, 26);
        TYPE_2_CODE_TABLE.put(boolean[].class, 27);
        TYPE_2_CODE_TABLE.put(Boolean[].class, 28);

        TYPE_2_CODE_TABLE.put(char.class, 29);
        TYPE_2_CODE_TABLE.put(Character.class, 30);
        TYPE_2_CODE_TABLE.put(char[].class, 31);
        TYPE_2_CODE_TABLE.put(Character[].class, 32);

        TYPE_2_CODE_TABLE.put(String.class, 33);
        TYPE_2_CODE_TABLE.put(String[].class, 34);
    }

    public static Object[] ParseMethodParams(Method method, byte[] input) {
        Class[] paramTypes = method.getParameterTypes();
        Object[] result = new Object[paramTypes.length];
        RLPList inputList = (RLPList)RLP.decode2(input).get(0);
        if (paramTypes.length != inputList.size()) {
            throw new RuntimeException(String.format("illegal input parameter length, method need [%d], actual [%d]", paramTypes.length, inputList.size()));
        }

        for (int i = 0; i < paramTypes.length; i++) {
            //System.out.println(i + ":" + paramTypes[i].getName());
            result[i] = BASE_TYPE_PARSER.get(paramTypes[i]).decode(inputList.get(i).getRLPData());
        }
        return result;
    }

    public static boolean isLegitimate(Class<?> clazz) {
        return BASE_TYPE_PARSER.containsKey(clazz);
    }

    public static Integer getCode(Class<?> clazz) {
        return TYPE_2_CODE_TABLE.get(clazz);
    }
}
