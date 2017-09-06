package eu.mihosoft.vmf.vmftext;

public class StringUtil {

    private StringUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static String firstToUpper (String name) {
        return name.substring(0,1).toUpperCase()+name.substring(1);
    }

    public static String firstToLower (String name) {
        return name.substring(0,1).toLowerCase()+name.substring(1);
    }

}
