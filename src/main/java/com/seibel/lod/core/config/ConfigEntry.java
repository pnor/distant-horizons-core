package com.seibel.lod.core.config;

import com.seibel.lod.core.config.file.ConfigFileHandling;

/**
 * Use for making the config variables
 *
 * @author coolGi2007
 */
public class ConfigEntry<T> {
    public String category = "";    // This should only be set once in the init
    public String name;            // This should only be set once in the init

    private T value;
    private String comment;
    private T min;
    private T max;
    private boolean show; // Show the option

    /** Creates the entry */
    private ConfigEntry(T value, String comment, T min, T max, boolean show) {
        this.value = value;
        this.comment = comment;
        this.min = min;
        this.max = max;
        this.show = show;
    }

    /** Gets the value */
    public T get() {
        return this.value;
    }
    /** Sets the value */
    public void set(T new_value) {
        this.value = new_value;
        save();
    }
    /** Sets the value without saving */
    public void setWTSave(T new_value) {
        this.value = new_value;
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
    /** Checks weather it should be shown */
    public boolean getShow() {
        return this.show;
    }
    /** Says to show the option */
    public void setShow(boolean newShow) {
        this.show = newShow;
    }

    /** Gets the comment */
    public String getComment() {
        return this.comment;
    }
    /** Sets the comment */
    public void setComment(String newComment) {
        this.comment = newComment;
    }


    /** Should not be needed for anything other than the gui/file handling */
    public String getCategory() {
        return this.category;
    }
    /** Should not be needed for anything other than the gui/file handling */
    public String getName() {
        return this.name;
    }
    /** Should not be needed for anything other than the gui/file handling */
    public String getNameWCategory() {
        return (this.category.isEmpty() ? "" : this.category + ".") + this.name;
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

    // Use this so it dost do file handling stuff
    public static class Builder<T> {
        private T tmpValue;
        private String tmpComment;
        private T tmpMin;
        private T tmpMax;
        private boolean tmpShow = true;

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

        public Builder<T> showOption(boolean newShow) {
            this.tmpShow = newShow;
            return this;
        }


        public ConfigEntry<T> build() {
            return new ConfigEntry<T>(tmpValue, tmpComment, tmpMin, tmpMax, tmpShow);
        }
    }
}
