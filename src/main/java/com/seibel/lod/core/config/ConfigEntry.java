package com.seibel.lod.core.config;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 * @version 02-03-2022
 */
public class ConfigEntry {
    public Object value;
    public String comment;
    // Should the min and max be long or int or double or something else
    public double min = Double.MIN_VALUE;
    public double max = Double.MAX_VALUE;


    /** Gets the value */
    public Object get() {
        return this.value;
    }
    /** Sets the value */
    public void set(Object new_value) {
        this.value = new_value;
        // Something to save the value
    }

    /** Gets the min value */
    public double getMin() {
        return this.min;
    }
    /** Sets the min value */
    public void setMin(double new_min) {
        this.min = new_min;
    }
    /** Gets the max value */
    public double getMax() {
        return this.max;
    }
    /** Sets the max value */
    public void setMax(double new_max) {
        this.max = new_max;
    }

    /** Gets the comment */
    public String getComment() {
        return this.comment;
    }
    /** Sets the comment */
    public void setComment(String new_comment) {
        this.comment = new_comment;
    }

    /** Is the value of this equal to another */
    public boolean equals(ConfigEntry obj) {
        if (this.value.equals(obj.value))
            return true;
        return false;
    }
}
