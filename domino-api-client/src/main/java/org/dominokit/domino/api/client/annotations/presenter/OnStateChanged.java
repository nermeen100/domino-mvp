package org.dominokit.domino.api.client.annotations.presenter;

import org.dominokit.domino.api.shared.extension.ActivationEvent;

import javax.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OnStateChanged {

    @NotNull
    Class<? extends ActivationEvent> value();
}
