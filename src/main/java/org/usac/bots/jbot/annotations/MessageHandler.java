package org.usac.bots.jbot.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageHandler {
    String id();
    float activationScore() default 0.5f;
    float residualScore() default 0;
}
