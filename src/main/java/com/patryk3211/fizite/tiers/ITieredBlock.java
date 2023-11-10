package com.patryk3211.fizite.tiers;

public interface ITieredBlock {
//    <T> boolean hasTierType();
    <T extends ITier> T getTier(Class<T> clazz);
}
