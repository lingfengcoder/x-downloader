package com.lingfeng.rpc.util;


/**
 * @Author: wz
 * @Date: 2022/5/11 12:56
 * @Description:
 */
public class StringUtils {
  
        /**
         * Check if the String is null or has only whitespaces.
         *
         * Modified from {@link org.apache.commons.lang.StringUtils#isBlank(String)}.
         *
         * @param string String to check
         * @return {@code true} if the String is null or has only whitespaces
         */
        public static boolean isBlank( String string) {
            if (isEmpty(string)) {
                return true;
            }
            for (int i = 0; i < string.length(); i++) {
                if (!Character.isWhitespace(string.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Check if the String has any non-whitespace character.
         *
         * @param string String to check
         * @return {@code true} if the String has any non-whitespace character
         */
        public static boolean isNotBlank( String string) {
            return !isBlank(string);
        }

        /**
         * Check if the String is null or empty.
         *
         * @param string String to check
         * @return {@code true} if the String is null or empty
         */
        public static boolean isEmpty( String string) {
            return string == null || string.isEmpty();
        }

        /**
         * Check if the String has any character.
         *
         * @param string String to check
         * @return {@code true} if the String has any character
         * @since 1.1.0
         */
        public static boolean isNotEmpty( String string) {
            return !isEmpty(string);
        }

        /**
         * Truncate the String to the max length.
         *
         * @param string String to truncate
         * @param maxLength max length
         * @return truncated String
         */
        public static String truncate(String string, int maxLength) {
            if (string.length() > maxLength) {
                return string.substring(0, maxLength);
            }
            return string;
        }

        private StringUtils() {
        }


}
