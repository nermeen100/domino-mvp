package org.dominokit.domino.apt.client;

import com.google.auto.service.AutoService;
import javax.annotation.Generated;
import org.dominokit.domino.api.client.ModuleConfiguration;
import org.dominokit.domino.api.client.ModuleConfigurationProvider;
import org.dominokit.domino.api.client.annotations.GwtIncompatible;

/**
 * This is generated class, please don't modify
 */
@Generated("org.dominokit.domino.apt.client.processors.module.client.ConfigurationProviderAnnotationProcessor")
@GwtIncompatible("Unused in GWT compilation")
@AutoService(ModuleConfigurationProvider.class)
public class TestModuleConfiguration_Provider implements ModuleConfigurationProvider {

    @Override
    public ModuleConfiguration get(){
        return new TestModuleConfiguration();
    }
}