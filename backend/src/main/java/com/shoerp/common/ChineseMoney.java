package com.shoerp.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额转人民币大写，用于采购合同「合计金额(大写)」。
 */
public final class ChineseMoney {

    private static final char[] DIGITS = "零壹贰叁肆伍陆柒捌玖".toCharArray();

    private static final String[] UNITS = {
            "", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟", "万"
    };

    private ChineseMoney() {
    }

    public static String toUpper(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        long cents = value.movePointRight(2).longValue();
        boolean negative = cents < 0;
        cents = Math.abs(cents);
        long integer = cents / 100;
        int jiao = (int) (cents / 10 % 10);
        int fen = (int) (cents % 10);

        StringBuilder result = new StringBuilder();
        if (negative) {
            result.append("负");
        }
        if (integer == 0) {
            result.append("零元");
        } else {
            result.append(integerToChinese(integer)).append("元");
        }
        if (jiao == 0 && fen == 0) {
            result.append("整");
        } else if (jiao == 0) {
            result.append('零').append(DIGITS[fen]).append("分");
        } else if (fen == 0) {
            result.append(DIGITS[jiao]).append("角整");
        } else {
            result.append(DIGITS[jiao]).append('角').append(DIGITS[fen]).append("分");
        }
        return result.toString();
    }

    private static String integerToChinese(long number) {
        String digits = String.valueOf(number);
        StringBuilder builder = new StringBuilder();
        int length = digits.length();
        for (int i = 0; i < length; i++) {
            int digit = digits.charAt(i) - '0';
            int position = length - i - 1;
            if (digit == 0) {
                if (i < length - 1 && digits.charAt(i + 1) != '0') {
                    builder.append('零');
                }
            } else {
                builder.append(DIGITS[digit]).append(UNITS[position]);
            }
        }
        return builder.toString();
    }
}
