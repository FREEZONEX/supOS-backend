package com.supos.uns.bo;

public interface UnsLabels {
    Long unsId();

    String[] labelNames();

    boolean isResetLabels();

    void setLabelId(String label, Long id);
}
