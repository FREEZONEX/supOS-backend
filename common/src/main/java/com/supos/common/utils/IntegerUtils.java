package com.supos.common.utils;

public class IntegerUtils {

    /**
     * 不抛出异常的数字转换
     *
     * @param s
     * @return
     */
    public static Integer parseInt(String s) {
        return parseInt(s, 10, null);
    }

    public static int getInt(Integer num) {
        return num != null ? num : 0;
    }

    public static int getInt(Integer num, int def) {
        return num != null ? num : def;
    }

    /**
     * 不抛出异常的数字转换
     *
     * @param s     -- 字符串
     * @param radix -- 进制
     * @param err   -- err[0] 用来存储异常信息
     * @return 转换失败则返回 null
     */
    public static Integer parseInt(String s, int radix, String[] err) {
        if (s == null) {
            return null;
        }
        if (err == null || err.length < 1) {
            err = new String[1];
        }
        if (radix < Character.MIN_RADIX) {
            err[0] = "radix " + radix +
                    " less than Character.MIN_RADIX";
            return null;
        }

        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix +
                    " greater than Character.MAX_RADIX");
        }

        int result = 0;
        boolean negative = false;
        int i = 0, len = s.length();
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+') {
                    err[0] = "forInput:" + s;
                    return null;
                }

                if (len == 1) {
                    // Cannot have lone "+" or "-"
                    err[0] = "forInput:" + s;
                    return null;
                }
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0) {
                    err[0] = "forInput:" + s;
                    return null;
                }
                if (result < multmin) {
                    err[0] = "forInput:" + s;
                    return null;
                }
                result *= radix;
                if (result < limit + digit) {
                    err[0] = "forInput:" + s;
                    return null;
                }
                result -= digit;
            }
        } else {
            err[0] = "forInput:" + s;
            return null;
        }
        return negative ? result : -result;
    }
}
