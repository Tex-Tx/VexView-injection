package cn.pig.compat.common.config;

   
                                  
   
public class Property {

    private final String value;
    private final String defaultValue;

    public Property(String value, String defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public boolean getBoolean() {
        return Boolean.parseBoolean(trim(this.value));
    }

    public String getString() {
        return this.value == null ? "" : this.value;
    }

    public double getDouble() {
        try {
            return Double.parseDouble(trim(this.value));
        } catch (Throwable t) {
            try {
                return Double.parseDouble(trim(this.defaultValue));
            } catch (Throwable ignored) {
                return 0.0D;
            }
        }
    }

    public int getInt() {
        try {
            return Integer.parseInt(trim(this.value));
        } catch (Throwable t) {
            try {
                return Integer.parseInt(trim(this.defaultValue));
            } catch (Throwable ignored) {
                return 0;
            }
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
