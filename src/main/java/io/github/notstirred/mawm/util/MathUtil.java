package io.github.notstirred.mawm.util;

public class MathUtil {
    public static int parseInt(String val) throws NumberFormatException {
        if (val.equalsIgnoreCase("infinity"))
            return Integer.MAX_VALUE / 2;
        else if (val.equalsIgnoreCase("-infinity"))
            return Integer.MIN_VALUE / 2;
        else
            return Integer.parseInt(val);
    }
}
