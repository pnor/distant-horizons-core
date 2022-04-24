package com.seibel.lod.core.config.types;

import com.seibel.lod.core.config.ConfigEntryAppearance;

public class ConfigCategory<T> extends AbstractConfigType<T> {
    private final String destination;    // Where the category goes to

    public ConfigCategory(ConfigEntryAppearance appearance, T value, String destination) {
        super(appearance, value);
        if (destination == null) {
            this.destination = getNameWCategory();
        } else {
            this.destination = destination;
        }
    }

    public String getDestination() {
        return this.destination;
    }

    public static class Builder<T> extends AbstractConfigType.Builder<T> {
        private String tmpDestination = null;

        public Builder<T> setDestination(String newDestination) {
            this.tmpDestination = newDestination;
            return this;
        }

        public ConfigCategory<T> build() {
            return new ConfigCategory(tmpAppearance, tmpValue, tmpDestination);
        }
    }
}
