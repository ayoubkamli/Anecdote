package io.gresse.hugo.anecdote;

import android.app.Application;

import io.gresse.hugo.anecdote.anecdote.model.MyObjectBox;
import toothpick.Scope;
import toothpick.Toothpick;
import toothpick.registries.FactoryRegistryLocator;
import toothpick.registries.MemberInjectorRegistryLocator;
import toothpick.smoothie.module.SmoothieApplicationModule;

/**
 * Main application
 *
 * Created by Hugo Gresse on 15/04/2017.
 */

public class AnecdoteApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Remove reflection from Toothpick
        Toothpick.setConfiguration(toothpick.configuration.Configuration.forProduction().disableReflection());
        MemberInjectorRegistryLocator.setRootRegistry(new io.gresse.hugo.anecdote.MemberInjectorRegistry());
        FactoryRegistryLocator.setRootRegistry(new io.gresse.hugo.anecdote.FactoryRegistry());

        // Open and attach smoothie module on Toothpick DI library
        Scope appScope = Toothpick.openScope(this);
        appScope.installModules(new SmoothieApplicationModule(this));
        // Create BoxStore and supply to to Toothpick to DI
        appScope.installModules(new ObectBoxModule(
                MyObjectBox.builder().androidContext(this).build()
        ));
    }
}
