package com.susu.dfs.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * java的位运算
 * <ul>
 *     <li>
 *          <p>& 与：全 1 为 1，有 0 则 0</p>
 *          <p>特殊用法：清零（与 0 进行与运算）、取一个数中的指定位（与 1 进行与运算）</p>
 *     </li>
 *     <li>
 *         <p>| 或：有 1 则 1，全 0 为 0</p>
 *         <p>特殊用法：使特定数位为 1</p>
 *     </li>
 *     <li>
 *         <p>^ 异或：不同为1，相同为0</p>
 *         <p>特殊用法：使特定数位翻转（与1异或）、保留原值（与0异或）、交换两个变量的值</p>
 *     </li>
 *     <li>
 *         <p>非（NOT，~）：取反</p>
 *     </li>
 *     <li>
 *         <p>左移运算：value << num</p>
 *         数值value向左移动num位，左边二进制位丢弃，右边补0。（注意byte和short类型移位运算时会变成int型，结果要强制转换）
 *         若1被移位到最左侧，则变成负数
 *         左移时舍弃位不包含1，则左移一次，相当于乘2
 *     </li>
 *     <li>
 *         <p>右移运算：value >> num</p>
 *         数值value向右移动num位，正数左补0，负数左补1，右边舍弃。（即保留符号位）
 *         右移一次，相当于除以2，并舍弃余数
 *         无符号右移>>>：左边位用0补充，右边丢弃
 *     </li>
 *     <li>
 *         <p>>>> 表示无符号右移，也叫逻辑右移，即若该数为正，则高位补0，而若该数为负数，则右移后高位同样补0</p>
 *
 *     </li>
 * </ul>
 * <p>负数以其正值的补码形式表示</p>
 * <pre>
 * 原码：一个整数按照绝对值大小转换成的二进制数称为原码
 * 反码：将二进制按位取反，所得的新二进制数称为原二进制数的反码
 * 补码：反码加1称为补码
 * </pre>
 * <p>先 & 后 ^ 后 |</p>
 * <p>int n=7; n<<=3; n=n&n+1|n+2^n+3; n>>=2;System.out.println(n);</p>
 * <p>n=14</p>
 *
 * <p>Description: Binary conversion</p>
 * <p>进制转换</p>
 * @author sujay
 * @version 14:25 2022/4/28
 * @since JDK1.8 <br/>
 */
public class HexConvertUtils {

    /**
     * <p>Description: bytes to hexadecimal string</p>
     * <p>bytes转16进制字符串</p>
     * @param bytes 字节数组
     * @return 16进制字符串
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            String sTemp = Integer.toHexString(0xFF & b);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }


    /**
     * <p>Description: Hexadecimal string to bytes</p>
     * <p>16进制字符串转bytes</p>
     * @param str 16进制字符串
     * @return 字节数组
     */
    public static byte[] hexStringToByte(String str) {
        int len = 0;
        int num = 0;
        if (str.length() >= 2) {
            len = (str.length() / 2);
            num = (str.length() % 2);
            if (num == 1) {
                str = "0" + str;
                len = len + 1;
            }
        } else {
            str = "0" + str;
            len = 1;
        }
        byte[] result = new byte[len];
        char[] archer = str.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(archer[pos]) << 4 | toByte(archer[pos + 1]));
        }
        return result;
    }

    /**
     * <p>Description: Hexadecimal string to ASCII</p>
     * <p>16进制字符串转ASCII</p>
     * @param hex 16进制字符串
     * @return 字节数组
     */
    public static String hexToASCII(String hex) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            sb.append((char) Integer.parseInt(str, 16));
        }
        return sb.toString();
    }

    /**
     * <p>Description: Hexadecimal string to ASCII</p>
     * <p>十进制字符串转ASCII</p>
     * @param dec 10进制int
     * @return 字节数组
     */
    public static String decToASCII(int dec) {
        return hexToASCII(Integer.toHexString(dec));
    }

    private static int toByte(char c) {
        if (c >= 'a') {
            return (c - 'a' + 10) & 0x0f;
        }
        if (c >= 'A') {
            return (c - 'A' + 10) & 0x0f;
        }
        return (c - '0') & 0x0f;
    }

    /**
     * <p>Description: decimal to binary</p>
     * <p>十进制转二进制</p>
     * @param number 数值
     * @return 二进制数
     */
    public static String decToBin(int number) {
        return decToBin(number,7);
    }

    /**
     * <p>Description: decimal to binary</p>
     * <p>十进制转二进制</p>
     * @param number 数值
     * @param bit 位数 从 0 开始数 ， 如果位数为 8 位，则传入 7
     * @return 二进制数
     */
    public static String decToBin(int number,int bit) {
        StringBuilder sb = new StringBuilder(bit + 1);
        for(int i = bit; i >= 0; i--) {
            int num = number >>> i & 1;
            sb.append(num);
        }
        return sb.toString();
    }

    /**
     * <p>Description: decimal to binary</p>
     * <p>二进制转十进制</p>
     * @param number 数值
     * @return 二进制数
     */
    public static int binToDec(int number) {
        int decimal = 0,p = 0;
        while(number!=0) {
            decimal += ( (number % 10) * Math.pow(2, p) );
            number = number / 10;
            p ++;
        }
        return decimal;
    }

    /**
     * <p>Description: long to byte</p>
     * @return long
     */
    public static long bytesToLong(byte[] byteNum, int index) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[ix + index] & 0xff);
        }
        return num;
    }

    /**
     * <p>Description: byte to long</p>
     * @return byte[]
     */
    public static byte[] longToBytes(long num) {
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }

    /**
     * <p>Description: int to long</p>
     * @return byte[]
     */
    public static byte[] intToBytes(int bodyLength, int index, int value) {
        byte[] bytes = new byte[bodyLength];
        bytes[index] = (byte) (value >>> 24);
        bytes[index + 1] = (byte) (value >>> 16);
        bytes[index + 2] = (byte) (value >>> 8);
        bytes[index + 3] = (byte) value;
        return bytes;
    }


    public static void setInt(byte[] bytes, int index, int value) {
        bytes[index] = (byte) (value >>> 24);
        bytes[index + 1] = (byte) (value >>> 16);
        bytes[index + 2] = (byte) (value >>> 8);
        bytes[index + 3] = (byte) value;
    }

    public static byte[] longToBytes(int bodyLength, int index, long value) {
        byte[] bytes = new byte[bodyLength];
        bytes[index] = (byte) (value >>> 56);
        bytes[index + 1] = (byte) (value >>> 48);
        bytes[index + 2] = (byte) (value >>> 40);
        bytes[index + 3] = (byte) (value >>> 32);
        bytes[index + 4] = (byte) (value >>> 24);
        bytes[index + 5] = (byte) (value >>> 16);
        bytes[index + 6] = (byte) (value >>> 8);
        bytes[index + 7] = (byte) value;
        return bytes;
    }

    public static void setLong( byte[] bytes, int index, long value) {
        bytes[index] = (byte) (value >>> 56);
        bytes[index + 1] = (byte) (value >>> 48);
        bytes[index + 2] = (byte) (value >>> 40);
        bytes[index + 3] = (byte) (value >>> 32);
        bytes[index + 4] = (byte) (value >>> 24);
        bytes[index + 5] = (byte) (value >>> 16);
        bytes[index + 6] = (byte) (value >>> 8);
        bytes[index + 7] = (byte) value;
    }

    public static int bytesToInt(byte[] memory, int index) {
        return  (memory[index]     & 0xff) << 24 |
                (memory[index + 1] & 0xff) << 16 |
                (memory[index + 2] & 0xff) <<  8 |
                memory[index + 3] & 0xff;
    }

    static long byteToLong(byte[] memory, int index) {
        return  ((long) memory[index]     & 0xff) << 56 |
                ((long) memory[index + 1] & 0xff) << 48 |
                ((long) memory[index + 2] & 0xff) << 40 |
                ((long) memory[index + 3] & 0xff) << 32 |
                ((long) memory[index + 4] & 0xff) << 24 |
                ((long) memory[index + 5] & 0xff) << 16 |
                ((long) memory[index + 6] & 0xff) <<  8 |
                (long) memory[index + 7] & 0xff;
    }


    public static void main(String[] args) {
        System.out.println(Arrays.toString(String.valueOf(1398100821).getBytes(StandardCharsets.UTF_8)));
    }
}
