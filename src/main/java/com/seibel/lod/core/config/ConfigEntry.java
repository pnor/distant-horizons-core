package com.seibel.lod.core.config;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 * @version 02-06-2022
 */
public class ConfigEntry<T> {
    private T value;
    private String comment;
    private T min;
    private T max;

    /** Creates the entry */
    private ConfigEntry(T value, String comment, T min, T max) {
        this.value = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
    }

    /** Gets the value */
    public T get() {
        return this.value;
    }
    /** Sets the value */
    public void set(T new_value) {
        this.value = new_value;
        // Something to save the value
    }

    /** Gets the min value */
    public T getMin() {
        return this.min;
    }
    /** Sets the min value */
    public void setMin(T new_min) {
        this.min = new_min;
    }
    /** Gets the max value */
    public T getMax() {
        return this.max;
    }
    /** Sets the max value */
    public void setMax(T new_max) {
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

    public boolean isValid() {  // TODO Make this better
        if (Number.class.isAssignableFrom(this.value.getClass())) { // Only check min max if it is a number
            if (this.min != null && this.max != null) return true;  // If there is no min max then return

            if ((Double) this.value < (Double) this.min || (Double) this.value > (Double) this.max)
                return false;
            else return true;
        }
        else return true;
    }

    /** Is the value of this equal to another */
    public boolean equals(ConfigEntry<?> obj) {
        if (this.value == obj.value)
            return true;
        return false;
    }

    // Use this so it dosnt do file handling stuff
    public static class Builder<T> {
        private T tmpValue;
        private String tmpComment;
        private T tmpMin;
        private T tmpMax;

        public Builder<T> set(T newValue) {
            this.tmpValue = newValue;
            return this;
        }

        public Builder<T> comment(String newComment) {
            this.tmpComment = newComment;
            return this;
        }

        public Builder<T> setMinMax(T newMin, T newMax) {
            this.tmpMin = newMin;
            this.tmpMax = newMax;
            return this;
        }


        public ConfigEntry<T> build() {
            return new ConfigEntry<T>(tmpValue, tmpComment, tmpMin, tmpMax);
        }
    }
}
