package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.ConfigBase;
import com.seibel.lod.core.config.ConfigEntryAppearance;
import com.seibel.lod.core.config.file.ConfigFileHandling;
import com.seibel.lod.core.objects.MinDefaultMax;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 */
public class ConfigEntry<T> extends AbstractConfigType<T, ConfigEntry<T>> {
    private T defaultValue;
    private String comment;
    private T min;
    private T max;
    // Stuff for server overwrites
    public final boolean useApiOverwrite;
    private T apiValue;

    /** Creates the entry */
    private ConfigEntry(ConfigEntryAppearance appearance, T value, String comment, T min, T max, boolean useApiOverwrite) {
        super(appearance, value);
        this.defaultValue = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
        this.useApiOverwrite = useApiOverwrite;
    }


    /** Gets the default value of the option */
    public T getDefaultValue() {
        return this.defaultValue;
    }

    public void setApiValue(T newApiValue) {
        this.apiValue = newApiValue;
    }
    @Override
    public void set(T newValue) {
        this.value = newValue;
        save();
    }
    @Override
    public T get() {
        if (useApiOverwrite && apiValue != null)
            return apiValue;
        return value;
    }
    public T getTrueValue() {
        return value;
    }

    public MinDefaultMax<T> getMinValueMax() {
        return new MinDefaultMax<T>(min, value, max);
    }
    public MinDefaultMax<T> getMinDefaultMax() {
        return new MinDefaultMax<T>(min, defaultValue, max);
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
    /** Sets the min and max in 1 setter */
    public void setMinMax(T newMin, T newMax) {
        this.max = newMin;
        this.min = newMax;
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
        return isValid(value);
    }
    /** Checks if a value is valid */
    public byte isValid(T value) {
        if (ConfigBase.disableMinMax)
            return 0;
        if (Number.class.isAssignableFrom(value.getClass())) { // Only check min max if it is a number
            if (this.max != null && Double.valueOf(value.toString()) > Double.valueOf(max.toString()))
                return 1;
            if (this.min != null && Double.valueOf(value.toString()) < Double.valueOf(min.toString()))
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

    public static class Builder<T> extends AbstractConfigType.Builder<T, Builder<T>> {
        private String tmpComment;
        private T tmpMin;
        private T tmpMax;
        private boolean tmpUseApiOverwrite;

        public Builder<T> comment(String newComment) {
            this.tmpComment = newComment;
            return this;
        }

        public Builder<T> setMinDefaultMax(MinDefaultMax<T> newMinDefaultMax) {
            this.set(newMinDefaultMax.defaultValue);
            this.setMinMax(newMinDefaultMax.minValue, newMinDefaultMax.maxValue);
            return this;
        }

        public Builder<T> setMinMax(T newMin, T newMax) {
            this.tmpMin = newMin;
            this.tmpMax = newMax;
            return this;
        }

        public Builder<T> setMin(T newMin) {
            this.tmpMin = newMin;
            return this;
        }
        public Builder<T> setMax(T newMax) {
            this.tmpMax = newMax;
            return this;
        }

        public Builder<T> setUseApiOverwrite(boolean newUseApiOverwrite) {
            this.tmpUseApiOverwrite = newUseApiOverwrite;
            return this;
        }


        public ConfigEntry<T> build() {
            return new ConfigEntry<T>(tmpAppearance, tmpValue, tmpComment, tmpMin, tmpMax, tmpUseApiOverwrite);
        }
    }
}
