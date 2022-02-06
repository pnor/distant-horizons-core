package com.seibel.lod.core.config;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 * @version 02-06-2022
 */
public class ConfigEntry {
    private Object value;
    private String comment;
    private Class<?> type;
    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;

    /** Defult entry */
    public ConfigEntry() {
    }

    /** Sets everything */
    public ConfigEntry(Object value, Class<?> type, String comment, double min, double max) {
        this.value = value;
        this.type = type;
        this.comment = comment;
        this.min = min;
        this.max = max;
    }

    /** Gets the value */
    public <type> type get() {
        return (type) this.value;
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

    // Use this so it dosnt do file handling stuff
    public static class Builder {
        private Object tmpValue;
        private String tmpComment;
        private Class<?> tmpType;
        private double tmpMin = Double.MIN_VALUE;
        private double tmpMax = Double.MAX_VALUE;

        public Builder set(Object newValue) {
            this.tmpValue = newValue;
            this.tmpType = newValue.getClass();
            return this;
        }

        public Builder comment(String newComment) {
            this.tmpComment = newComment;
            return this;
        }

        public Builder setMinMax(double newMin, double newMax) {
            this.tmpMin = newMin;
            this.tmpMax = newMax;
            return this;
        }


        public ConfigEntry build() {
            return new ConfigEntry(tmpValue, tmpType, tmpComment, tmpMin, tmpMax);
        }
    }
}
