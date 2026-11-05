package com.marinbom2.retailfnbplatform.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * enum 칼럼을 varchar 로, CHECK 제약 없이 만든다.
 * 기본은 네이티브 enum 이라 ddl-auto=update 에서 enum 값을 추가하면 저장이 막힌다. 검증은 EnumColumnSchemaTest.
 */
public class NoColumnCheckMySQLDialect extends MySQLDialect {

    public NoColumnCheckMySQLDialect() {
        super();
    }

    public NoColumnCheckMySQLDialect(DialectResolutionInfo info) {
        super(info);
    }

    /** enum 타입 코드를 varchar 로 바인딩·DDL 하게 바꾼다. 네이티브 enum 타입을 쓰지 않는다. */
    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.getTypeConfiguration().getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.ENUM, VarcharJdbcType.INSTANCE);
    }

    /** 칼럼 CHECK 제약(enum 값 목록 포함)을 만들지 않는다. */
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }
}
