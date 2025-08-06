package com.supos.common;

public enum SrcJdbcType {
    None(0, "", "", 0),
    TdEngine(1, "tdengine-datasource", "td", Constants.TIME_SEQUENCE_TYPE),
    Postgresql(2, "postgresql", "pg", Constants.RELATION_TYPE),
    TimeScaleDB(3, "postgresql", "tmsc", Constants.TIME_SEQUENCE_TYPE),
    ;

    public final int id;

    public final String dataSrcType;
    public final String alias;
    public final int typeCode;//1--时序，2--关系

    SrcJdbcType(int id, String dataSrcType, String alias, int typeCode) {
        this.id = id;
        this.dataSrcType = dataSrcType;
        this.alias = alias;
        this.typeCode = typeCode;
    }

    public static SrcJdbcType getById(Integer id) {
        if (id == null) {
            return None;
        }
        int index = id;
        return index <= MAX_ID && index >= 0 ? types[index] : None;
    }

    private static final SrcJdbcType[] types;
    private static final int MAX_ID;

    static {
        int maxId = -1;
        for (SrcJdbcType v : SrcJdbcType.values()) {
            if (v.id > maxId) {
                maxId = v.id;
            }
        }
        MAX_ID = maxId;
        types = new SrcJdbcType[maxId + 1];
        for (SrcJdbcType v : SrcJdbcType.values()) {
            types[v.id] = v;
        }
    }

    public String toString() {
        return alias;
    }

}
