package aquarius.iemodule.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StringUtils {

    private static final Pattern NOT_BLANK_PATTER = Pattern.compile("^\\s*$");

    /**
     * Checks if a String object is null and blank or not
     *
     * @param value the object to be checked
     * @return true if "value" is not null and is not blank, otherwise return false
     */
    public static boolean hasContent(String value) {
        return value != null && !NOT_BLANK_PATTER.matcher(value).matches();
    }

    /**
     * Checks if a String object is blank or not
     *
     * @param value the object to be checked
     * @return true if "value" is blank, otherwise return false
     */
    public static boolean isBlank(String value) {
        return value == null || NOT_BLANK_PATTER.matcher(value).matches();
    }

    public static String[] split(String value, String delimiter) {
        List<String> parts = new ArrayList<>();

        int jumpLength = delimiter.length();
        int startIndex = 0;
        int stopIndex = value.indexOf(delimiter, startIndex);
        if (stopIndex == -1) {
            return new String[]{value};
        }

        while (true) {
            String part = value.substring(startIndex, stopIndex);
            if (hasContent(part)) {
                parts.add(part);
            }

            startIndex = stopIndex + jumpLength;
            stopIndex = value.indexOf(delimiter, startIndex);

            if (stopIndex == -1) {
                stopIndex = value.length();

                part = value.substring(startIndex, stopIndex);
                if (hasContent(part)) {
                    parts.add(part);
                }
                break;
            }
        }

        return parts.toArray(new String[0]);
    }

    public static String camelCaseToSnakeCase(String camelCaseString) {
        char[] characters = camelCaseString.toCharArray();
        StringBuilder builder = new StringBuilder(String.valueOf(characters[0]));

        for (int i = 1; i < characters.length; i++) {
            if (Character.isUpperCase(characters[i]) && builder.charAt(builder.length() - 1) != '_') {
                builder.append("_");
            }
            builder.append(characters[i]);
        }
        return builder.toString().toLowerCase();
    }
}
