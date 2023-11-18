package com.patryk3211.fizite.tiers;

public interface ITieredBlock {
    <T extends ITier> T getTier(Class<T> clazz);
}
