package com.sysdream.fino;

import java.util.ArrayList;
import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.os.Bundle;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Activity;

import dalvik.system.DexClassLoader;

/**
 * Provide a full featured remote inspection interface as specified in the
 * <code>IInspectionService</code> interface.
 *
 * @author <a href="mailto:p.jaury@sysdream.com">Pierre Jaury</a>
 * @version 1.0
 */
public class InspectionService
    extends Service
{
    /**
     * Entry points list
     */
    private ArrayList<Object> entryPoints = new ArrayList<Object>();


    /**
     * Prepare the inspection by registering first entry points.
     *
     */
    public void onCreate
	()
    {
    File dex_dir = null;
    File outdex_dir = null;

	super.onCreate();
    /*
     * Flush macro files and optimized dex files
     */
    dex_dir = new File(getDir("dex", Context.MODE_PRIVATE).getAbsolutePath());
    for (File f : dex_dir.listFiles()) {
        if (f.isFile())
            f.delete();
    }
    outdex_dir = new File(this.getDir("outdex", Context.MODE_PRIVATE).getAbsolutePath());
    for (File f : outdex_dir.listFiles()) {
        if (f.isFile())
            f.delete();
    }

    /*
     * Add the default entry points to the list
     */
    if(!entryPoints.contains(true)) {
        /*
        entryPoints.add(false);
        entryPoints.add(true);
        entryPoints.add("Hello, world!");
        */
        entryPoints.add(this.getApplication());
    }
	/*
	 * Register the ActivityLifecycleCallback for entry points automatic
	 * discovery.
	 * Note this requires API >= 15
	 */
	getApplication().registerActivityLifecycleCallbacks
	    (new ActivityLifecycleCallbacks() {
		    /**
		     * Whenever an activity is resumed, check if it is
		     * already registered. If not, add it to the register.
		     */
		    public void onActivityResumed
			(final Activity activity)
		    {
			if(!InspectionService.this.entryPoints.contains
			   (activity))
			    InspectionService.this.entryPoints.add(activity);
		    }

		    /**
		     * Whenever an activity is destroyed, try to remove it from
		     * the register in order to avoid null pointer exceptions.
		     */
		    public void onActivityDestroyed
			(final Activity activity)
		    {
			if(InspectionService.this.entryPoints.contains
			   (activity))
			    InspectionService.this.entryPoints.remove(activity);
		    }

		    /**
		     * Nil
		     */
		    public void onActivityCreated
			(final Activity activity,
			 final Bundle savedInstanceState)
		    {}

		    /**
		     * Nil
		     */
		    public void onActivityPaused
			(final Activity activity)
		    {}

		    /**
		     * Nil
		     */
		    public void onActivitySaveInstanceState
			(final Activity activity,
			 Bundle outState)
		    {}

		    /**
		     * Nil
		     */
		    public void onActivityStarted
			(final Activity activity)
		    {}

		    /**
		     * Nil
		     */
		    public void onActivityStopped
			(final Activity activity)
		    {}
		});
    }

    /**
     * Bind to the service.
     *
     * @param e the binding intent
     * @return a binder instance
     */
    public IBinder onBind
	(final Intent e)
    {
	/*
	 * Class loader for dynamic macro loading
	 */
    /*
	final File internal = new File
	    (getDir("dex", Context.MODE_PRIVATE), "macros.jar");
	final File optimized = getDir("outdex", Context.MODE_PRIVATE);
	DexClassLoader loader = new DexClassLoader
	    (internal.getAbsolutePath(),
	     optimized.getAbsolutePath(),
	     null,
	     getClassLoader());
    */
	/*
	 * Initializing the actual service
	 */
	return new InspectionStub
	    (entryPoints,
	     getApplicationContext());
    }
}
