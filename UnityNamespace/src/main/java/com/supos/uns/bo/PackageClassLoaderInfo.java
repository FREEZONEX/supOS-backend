package com.supos.uns.bo;

import lombok.Getter;

@Getter
public class PackageClassLoaderInfo {
    final String basePackage;
    final ClassLoader classLoader;

    public PackageClassLoaderInfo(String basePackage, ClassLoader classLoader) {
        this.basePackage = basePackage;
        this.classLoader = classLoader;
    }

    @Override
    public String toString() {
        return basePackage;
    }
}
