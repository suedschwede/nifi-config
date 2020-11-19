package at.mic.nifi.config.model;

/**
 * Exception for this module
 *
 * Created by SFRJ2737 on 2017-05-28.
 */
public class ConfigException extends RuntimeException {
    public ConfigException(Throwable e) {
        super(e);
    }

    public ConfigException(String s, Throwable e) {
        super(s,e);
    }

    public ConfigException(String s) {
        super(s);
    }
}
