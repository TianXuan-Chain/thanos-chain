package com.thanos.chain.contract.ca.resolver;


import com.thanos.common.utils.rlp.RLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * FilterInvokeResultResolver.java description：
 *
 * @Author laiyiyu create on 2021-04-26 15:33:31
 */
public class FilterInvokeResultResolver {

    private static final Logger logger = LoggerFactory.getLogger("ca");
    
    static abstract class ParameterEncoder<T> {

        public abstract byte[] encode(T input);
    }

    //=======================

    static class ByteEncoder extends ParameterEncoder<Byte> {

        @Override
        public byte[] encode(Byte input) {

            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeByte(input);
        }
    }

    static class PrimitiveByteArrayEncoder extends ParameterEncoder<byte[]> {

        @Override
        public byte[] encode(byte[] input) {
            return RLP.encodeElement(input);
        }
    }

    static class ByteArrayEncoder extends ParameterEncoder<Byte[]> {

        @Override
        public byte[] encode(Byte[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }


            byte[] byteInput = new byte[input.length];
            for (int i = 0; i < input.length; i++) {
                byteInput[i] = input[i];
            }


            return RLP.encodeElement(byteInput);
        }
    }

    //=======================

    static class ShortEncoder extends ParameterEncoder<Short> {
        @Override
        public byte[] encode(Short input) {

            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeShort(input);
        }
    }

    static class ShortArrayEncoder extends ParameterEncoder<Short[]> {

        @Override
        public byte[] encode(Short[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    result[i] = RLP.encodeShort(input[i]);
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveShortArrayEncoder extends ParameterEncoder<short[]> {
        @Override
        public byte[] encode(short[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                result[i] = RLP.encodeShort(input[i]);
            }

            return RLP.encodeList(result);
        }
    }

    //=======================

    static class IntegerEncoder extends ParameterEncoder<Integer> {

        @Override
        public byte[] encode(Integer input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeInt(input);
        }
    }

    static class IntegerArrayEncoder extends ParameterEncoder<Integer[]> {

        @Override
        public byte[] encode(Integer[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    result[i] = RLP.encodeInt(input[i]);
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveIntegerArrayEncoder extends ParameterEncoder<int[]> {

        @Override
        public byte[] encode(int[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                result[i] = RLP.encodeInt(input[i]);
            }

            return RLP.encodeList(result);
        }
    }

    //=======================

    static class LongEncoder extends ParameterEncoder<Long> {

        @Override
        public byte[] encode(Long input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeBigInteger(BigInteger.valueOf(input));
        }
    }

    static class LongArrayEncoder extends ParameterEncoder<Long[]> {
        @Override
        public byte[] encode(Long[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    result[i] = RLP.encodeBigInteger(BigInteger.valueOf(input[i]));
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveLongArrayEncoder extends ParameterEncoder<long[]> {
        @Override
        public byte[] encode(long[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                result[i] = RLP.encodeBigInteger(BigInteger.valueOf(input[i]));
            }

            return RLP.encodeList(result);
        }
    }

    static class FloatEncoder extends ParameterEncoder<Float> {
        @Override
        public byte[] encode(Float input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeString(input + "");
        }
    }

    static class FloatArrayEncoder extends ParameterEncoder<Float[]> {

        @Override
        public byte[] encode(Float[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    result[i] = RLP.encodeString(input[i] + "");
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveFloatArrayEncoder extends ParameterEncoder<float[]> {

        @Override
        public byte[] encode(float[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                result[i] = RLP.encodeString(input[i] + "");
            }

            return RLP.encodeList(result);
        }
    }

    //=======================

    static class DoubleEncoder extends ParameterEncoder<Double> {

        @Override
        public byte[] encode(Double input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeString(input + "");
        }
    }

    static class DoubleArrayEncoder extends ParameterEncoder<Double[]> {

        @Override
        public byte[] encode(Double[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {
                    result[i] = RLP.encodeString(input[i] + "");
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveDoubleArrayEncoder extends ParameterEncoder<double[]> {

        @Override
        public byte[] encode(double[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                result[i] = RLP.encodeString(input[i] + "");
            }

            return RLP.encodeList(result);
        }
    }

    //=======================


    static class BooleanEncoder extends ParameterEncoder<Boolean> {

        @Override
        public byte[] encode(Boolean input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            if (input) {
                return RLP.encodeInt(1);
            } else {
                return RLP.encodeInt(0);
            }
        }
    }

    static class BooleanArrayEncoder extends ParameterEncoder<Boolean[]> {
        @Override
        public byte[] encode(Boolean[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i] != null) {


                    if (input[i]) {
                        result[i] = RLP.encodeInt(1);
                    } else {
                        result[i] = RLP.encodeInt(0);
                    }
                } else {
                    result[i] = RLP.encodeElement(null);
                }
            }

            return RLP.encodeList(result);
        }
    }

    static class PrimitiveBooleanArrayEncoder extends ParameterEncoder<boolean[]> {
        @Override
        public byte[] encode(boolean[] input) {
            if (input == null) {
                RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {
                if (input[i]) {
                    result[i] = RLP.encodeInt(1);
                } else {
                    result[i] = RLP.encodeInt(0);
                }
            }

            return RLP.encodeList(result);
        }
    }

    //=======================


    static class CharEncoder extends ParameterEncoder<Character> {
        @Override
        public byte[] encode(Character input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeString(input.toString());
        }
    }

    static class CharArrayEncoder extends ParameterEncoder<char[]> {
        @Override
        public byte[] encode(char[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeString(new String(input));
        }
    }

    static class CharacterArrayEncoder extends ParameterEncoder<Character[]> {
        
        @Override
        public byte[] encode(Character[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }
            
            char[] charArr = new char[input.length];
            for (int i = 0; i < input.length; i++) {
                if (input[i] == null) {
                    logger.warn("has illegal return, null Character, index=" + i);
                    charArr[i] = '*';
                } else {
                    charArr[i] = input[i];
                }
                
            }

            return RLP.encodeString(new String(charArr));
        }
    }


    //=======================


    static class StringEncoder extends ParameterEncoder<String> {

        @Override
        public byte[] encode(String input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            return RLP.encodeString(input);
        }
    }

    static class StringArrayEncoder extends ParameterEncoder<String[]> {

        @Override
        public byte[] encode(String[] input) {
            if (input == null) {
                return RLP.encodeElement(null);
            }

            byte[][] result = new byte[input.length][];
            for (int i = 0; i < input.length; i++) {

                if (input[i] != null) {
                    result[i] = RLP.encodeString(input[i]);
                } else {
                    result[i] = RLP.encodeElement(null);
                }

            }
            return RLP.encodeList(result);
        }
    }
    
    //===========================================

    static class VoidEncoder extends ParameterEncoder<Void> {
        @Override
        public byte[] encode(Void input) {
            
            return RLP.encodeElement(null);
        }
    }

    private static final Map<Class<?>, ParameterEncoder> BASE_TYPE_PARSER = new HashMap<>();

    static {
        BASE_TYPE_PARSER.put(byte.class, new ByteEncoder());
        BASE_TYPE_PARSER.put(Byte.class, new ByteEncoder());
        BASE_TYPE_PARSER.put(byte[].class, new PrimitiveByteArrayEncoder());
        BASE_TYPE_PARSER.put(Byte[].class, new ByteArrayEncoder());

        BASE_TYPE_PARSER.put(short.class, new ShortEncoder());
        BASE_TYPE_PARSER.put(Short.class, new ShortEncoder());
        BASE_TYPE_PARSER.put(short[].class, new PrimitiveShortArrayEncoder());
        BASE_TYPE_PARSER.put(Short[].class, new ShortArrayEncoder());

        BASE_TYPE_PARSER.put(int.class, new IntegerEncoder());
        BASE_TYPE_PARSER.put(Integer.class, new IntegerEncoder());
        BASE_TYPE_PARSER.put(int[].class, new PrimitiveIntegerArrayEncoder());
        BASE_TYPE_PARSER.put(Integer[].class, new IntegerArrayEncoder());

        BASE_TYPE_PARSER.put(long.class, new LongEncoder());
        BASE_TYPE_PARSER.put(Long.class, new LongEncoder());
        BASE_TYPE_PARSER.put(long[].class, new PrimitiveLongArrayEncoder());
        BASE_TYPE_PARSER.put(Long[].class, new LongArrayEncoder());

        BASE_TYPE_PARSER.put(float.class, new FloatEncoder());
        BASE_TYPE_PARSER.put(Float.class, new FloatEncoder());
        BASE_TYPE_PARSER.put(float[].class, new PrimitiveFloatArrayEncoder());
        BASE_TYPE_PARSER.put(Float[].class, new FloatArrayEncoder());

        BASE_TYPE_PARSER.put(double.class, new DoubleEncoder());
        BASE_TYPE_PARSER.put(Double.class, new DoubleEncoder());
        BASE_TYPE_PARSER.put(double[].class, new PrimitiveDoubleArrayEncoder());
        BASE_TYPE_PARSER.put(Double[].class, new DoubleArrayEncoder());

        BASE_TYPE_PARSER.put(boolean.class, new BooleanEncoder());
        BASE_TYPE_PARSER.put(Boolean.class, new BooleanEncoder());
        BASE_TYPE_PARSER.put(boolean[].class, new PrimitiveBooleanArrayEncoder());
        BASE_TYPE_PARSER.put(Boolean[].class, new BooleanArrayEncoder());

        BASE_TYPE_PARSER.put(char.class, new CharEncoder());
        BASE_TYPE_PARSER.put(Character.class, new CharEncoder());
        BASE_TYPE_PARSER.put(char[].class, new CharArrayEncoder());
        BASE_TYPE_PARSER.put(Character[].class, new CharacterArrayEncoder());

        BASE_TYPE_PARSER.put(String.class, new StringEncoder());
        BASE_TYPE_PARSER.put(String[].class, new StringArrayEncoder());

        BASE_TYPE_PARSER.put(void.class, new VoidEncoder());
        BASE_TYPE_PARSER.put(Void.class, new VoidEncoder());
    }

    public static boolean isLegitimate(Class<?> clazz) {
        return BASE_TYPE_PARSER.containsKey(clazz);
    }
    
    public static byte[] encodeResult(Class<?> returnType, Object output) {
        ParameterEncoder encoder = BASE_TYPE_PARSER.get(returnType);
        return encoder.encode(output);
    }
}
