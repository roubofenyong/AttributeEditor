package com.esri.arcgis.android.samples.attributeeditor;

import android.view.View;

import com.esri.core.map.Field;

/**
 * Created by kobexuqiang on 2016/1/5.
 */
public class AttributeItem {

    private Field field;
    private Object value;
    private View view;

    public void setField(Field field) {
        this.field = field;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public Field getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
