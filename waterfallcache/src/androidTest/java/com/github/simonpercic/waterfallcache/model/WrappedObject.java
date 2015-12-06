package com.github.simonpercic.waterfallcache.model;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
public class WrappedObject {

    private SimpleObject object;

    private String value;

    public SimpleObject getObject() {
        return object;
    }

    public String getValue() {
        return value;
    }

    public void setObject(SimpleObject object) {
        this.object = object;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
