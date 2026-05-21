package cn.rbac.server.framework.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    String title() default "";

    BusinessType businessType() default BusinessType.OTHER;

    boolean isSaveRequestData() default true;

    boolean isSaveResponseData() default true;

    enum BusinessType {
        OTHER(0),
        INSERT(1),
        UPDATE(2),
        DELETE(3),
        QUERY(4),
        EXPORT(5),
        IMPORT(6);

        private final int value;

        BusinessType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
