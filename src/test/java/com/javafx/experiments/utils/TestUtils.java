package com.javafx.experiments.utils;

/**
 * @author Eclion
 */
public class TestUtils {
    public static String ALL_TEST_PASSED = "";

    public static void assertEquals(Object expected, Object actual) throws Exception {
        if (expected != actual)
        {
            throw new Exception("different");
        }
    }
}
