package com.github.simonpercic.waterfallcache.model;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class GenericObject<T> {

    private T object;

    private String value;

    public T getObject() {
        return object;
    }

    public String getValue() {
        return value;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
