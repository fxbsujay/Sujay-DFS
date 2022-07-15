package com.susu.dfs.common.utils;

import java.util.Random;
import java.util.UUID;

/**
 * <p>Description: String processing class</p>
 * <p>字符串处理工具类</p>
 * @author sujay
 * @version 21:19 2022/1/20
 * @see String
 * @since JDK1.8
 */
public class StringUtils {

    public static final Random RANDOM = new Random();

    public static final String BASE_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * <p>将字符串text中由openToken和closeToken组成的占位符依次替换为args数组中的值</p>
     * @param openToken 开始字符
     * @param closeToken 结束字符
     * @param text 目标字符
     * @param args 替换字符
     */
    public static String parse(String openToken, String closeToken, String text, Object... args) {
        if (args == null || args.length <= 0) {
            return text;
        }
        int argsIndex =0;
        if (text == null || text.isEmpty()) {
            return "";
        }
        char[] src = text.toCharArray();
        int offset =0;
        int start = text.indexOf(openToken, offset);
        if (start == -1) {
            return text;
        }
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        while (start > -1) {
            if (start > 0 && src[start -1] =='\\') {
                builder.append(src, offset, start - offset -1).append(openToken);
                offset = start + openToken.length();
            }else {
                if (expression == null) {
                    expression = new StringBuilder();
                }else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if (end > offset && src[end -1] =='\\') {
                        expression.append(src, offset, end - offset -1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    }else {
                        expression.append(src, offset, end - offset);
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if (end == -1) {
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                }else {
                    String value = (argsIndex <= args.length -1) ?
                            (args[argsIndex] == null ? "" : args[argsIndex].toString()) : expression.toString();
                    builder.append(value);
                    offset = end + closeToken.length();
                    argsIndex++;
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }


    /**
     * <p>Description: Are they all numbers</p>
     * <p>是否全是数字</p>
     * <pre>
     * StringUtils.isAllDigital("123.45")    = false
     * StringUtils.isAllDigital("12345ABC")  = false
     * StringUtils.isAllDigital(" 12345")    = false
     * StringUtils.isAllDigital("00000")     = true
     * StringUtils.isAllDigital("123456")    = true
     * StringUtils.isAllDigital("")          = true
     * </pre>
     */
    public static boolean isAllDigital(String str) {
        char[] cs = str.toCharArray();
        boolean result = true;
        for (char c : cs) {
            if (!Character.isDigit(c)) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * <p>Description: The letter following the specified symbol is capitalized and the character is removed</p>
     * <p>指定符号的后面的字母转大写，并去掉该字符</p>
     * @param str 目标字符串
     * @param regex 指定的字符
     */
    public static String firstLetterBig(String str,char regex) {
        if (isEmpty(str)){
            return str;
        }
        char[] cs = dispelBlank(str).toCharArray();
        int len = cs.length;
        for (char c : cs) {
            if (c == regex) {
                --len;
            }
        }
        if (len == cs.length) {
            return str;
        }
        char[] buf = new char[len];
        int j = 0;
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] == regex) {
                if (cs[i + 1] >= 'a' && cs[i + 1] <= 'z') {
                    cs[i + 1] -= 32;
                }
            }else {
                buf[j] = cs[i];
                j++;
            }
        }
        return String.valueOf(buf);
    }

    /**
     * <p>Description: First capital letter</p>
     * <p>首字母大写,会去掉字符串前后空格</p>
     */
    public static String firstLetterBig(String str) {
        if (isBlank(str)){
            return str;
        }
        char[] cs = dispelBlank(str).toCharArray();
        if (cs[0] >= 'A' && cs[0] <= 'Z'){
            return str;
        }
        cs[0] -= 32;
        return String.valueOf(cs);
    }

    /**
     * <p>Description: First letter lowercase</p>
     * <p>首字母小写</p>
     */
    public static String firstLetterSmall(String str) {
        if (isBlank(str)){
            return str;
        }
        char[] cs = dispelBlank(str).toCharArray();
        if (cs[0] >= 'a' && cs[0] <= 'z'){
            return str;
        }
        cs[0] += 32;
        return String.valueOf(cs);
    }

    /**
     * <p>Description: get uuid</p>
     * <p>获取UUID</p>
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-","").toUpperCase();
    }

    /**
     * <p>Description: Clear the space around the string</p>
     * <p>清除字符串两边空白</p>
     */
    public static String dispelBlank(String str) {
        return isNotBlank(str) ? str.trim() : "";
    }


    /**
     * <p>Description: Clear all blanks in string</p>
     * <p>清除字符串中全部空白</p>
     */
    public static String dispelBlankAll(String str) {
        if (isEmpty(str)){
            return "";
        }
        StringBuilder sb = new StringBuilder(str);
        int index = 0;
        while (sb.length() > index) {
            if (Character.isWhitespace(sb.charAt(index))) {
                sb.deleteCharAt(index);
            } else {
                index++;
            }
        }
        return sb.toString();
    }

    /**
     *  <p>Description: Convert object to string</p>
     *  <p>转为String</p>
     */
    public static String toString(Object object) {
        if (object != null && object.toString().length() > 0) {
            return object.toString();
        }
        return "";
    }



    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     * <p>not empty and not null and not whitespace only</p>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     *
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    public static int remove(char[] cs, char val) {
        int len = cs.length;
        int i = 0, j = 0;

        while (i < len && j < len) {
            if (cs[j] != val) {
                cs[i] = cs[j];
                i++;
            }
            j++;
        }
        return i;
    }

    public static int removeElement(int[] nums, int val) {
        int len = nums.length;
        int i = 0;
        int j = 0;
        while (i < len && j < len) {
            if (nums[j] != val) {
                nums[i] = nums[j];
                i++;
            }
            j++;
        }
        return i;
    }

    public static int hash(String source, int maxSize) {
        int hash = toPositive(murmur2(source.getBytes()));
        return hash % maxSize;
    }

    /**
     * Generates 32 bit murmur2 hash from byte array
     *
     * @param data byte array to hash
     * @return 32 bit hash of the given array
     */
    public static int murmur2(final byte[] data) {
        int length = data.length;
        int seed = 0x9747b28c;
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed ^ length;
        int length4 = length / 4;

        for (int i = 0; i < length4; i++) {
            final int i4 = i * 4;
            int k = (data[i4] & 0xff) + ((data[i4 + 1] & 0xff) << 8) + ((data[i4 + 2] & 0xff) << 16) + ((data[i4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length % 4) {
            case 3:
                h ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2:
                h ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1:
                h ^= data[length & ~3] & 0xff;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;
        return h;
    }

    public static int toPositive(int number) {
        return number & 0x7fffffff;
    }

    /**
     * 获取随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        return getRandomString(length, BASE_KEY);
    }

    /**
     * 获取随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length, String key) {
        return getRandomString(length, key, false);
    }

    /**
     * 获取随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String getRandomString(int length, String key, boolean upperCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = RANDOM.nextInt(key.length());
            sb.append(key.charAt(number));
        }
        String s = sb.toString();
        return upperCase ? s.toUpperCase() : s;
    }

    public static String format(int i) {
        if (i >= 100) {
            return String.valueOf(i);
        }
        if (i >= 10) {
            return "0" + i;
        }
        if (i >= 0) {
            return "00" + i;
        }
        return "";
    }


    public static void main(String[] args) {
        String str = "  Hello Word Java ";
        Object obj = "  Hello Word Java ";
        System.out.println("删除前后空白" + dispelBlank(str));
        System.out.println("删除字符串中所有空白" + dispelBlankAll(str));
        System.out.println("字符串转换" + toString(obj));
        System.out.println("首字母大写" + firstLetterBig(" hello"));
        System.out.println("首字母小写" + firstLetterSmall(" Hello Word"));
        System.out.println("是否全是数字" + isAllDigital(" 0000"));
        System.out.println(firstLetterBig("as_A_1", '_'));
    }

}
