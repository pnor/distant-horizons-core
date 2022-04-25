package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.ConfigEntryAppearance;
import com.seibel.lod.core.config.file.ConfigFileHandling;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 */
public class ConfigEntry<T> extends AbstractConfigType<T, ConfigEntry> {

    private T defaultValue;
    private String comment;
    private T min;
    private T max;

    /** Creates the entry */
    private ConfigEntry(ConfigEntryAppearance appearance, T value, String comment, T min, T max) {
        super(appearance, value);
        this.defaultValue = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
    }


    /** Gets the default value of the option */
    public T getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void set(T newValue) {
        this.value = newValue;
        save();
    }

    /** Sets the value without saving */
    public void setWTSave(T newValue) {
        this.value = newValue;
    }

    /** Gets the min value */
    public T getMin() {
        return this.min;
    }
    /** Sets the min value */
    public void setMin(T newMin) {
        this.min = newMin;
    }
    /** Gets the max value */
    public T getMax() {
        return this.max;
    }
    /** Sets the max value */
    public void setMax(T newMax) {
        this.max = newMax;
    }

    /** Gets the comment */
    public String getComment() {
        return this.comment;
    }
    /** Sets the comment */
    public void setComment(String newComment) {
        this.comment = newComment;
    }


    /**
     * Checks if the option is valid
     *
     * 0 == valid
     * 1 == number too high
     * -1 == number too low
     */
    public byte isValid() {
        if (Number.class.isAssignableFrom(this.value.getClass())) { // Only check min max if it is a number
            if (this.max != null && (Double) this.value > (Double) this.max)
                return 1;
            if (this.min != null && (Double) this.value < (Double) this.min)
                return -1;

            return 0;
        }
        return 0;
    }

    /** This should normally not be called since set() automatically calls this */
    public void save() {
        ConfigFileHandling.saveEntry(this);
    }
    /** This should normally not be called except for special circumstances */
    public void load() {
        ConfigFileHandling.loadEntry(this);
    }

    /** Is the value of this equal to another */
    public boolean equals(ConfigEntry<?> obj) {
        // Can all of this just be "return this.value.equals(obj.value)"?

        if (this.value.getClass() != obj.value.getClass())
            return false;
        if (Number.class.isAssignableFrom(this.value.getClass())) {
            if (this.value == obj.value)
                return true;
            else return false;
        } else {
            if (this.value.equals(obj.value))
                return true;
            else return false;
        }
    }

    public static class Builder<T> extends AbstractConfigType.Builder<T, Builder> {
        private String tmpComment;
        private T tmpMin;
        private T tmpMax;

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
            return new ConfigEntry<T>(tmpAppearance, tmpValue, tmpComment, tmpMin, tmpMax);
        }
    }
}
