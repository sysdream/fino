package com.sysdream.fino;

import java.lang.reflect.*;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import android.content.Context;
import android.util.Log;
import android.util.Base64;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Looper;

import dalvik.system.DexClassLoader;

/**
 * Main logics for the inspection service, actual implementation of the
 * <code>IInspectionService</code> interface.
 *
 * @author <a href="mailto:p.jaury@sysdream.com">Pierre Jaury</a>
 * @version 1.0
 */
public class InspectionStub
    extends IInspectionService.Stub
{
    /**
     * Constant used for field separation in <code>String</code> objects, as
     * specified in the <code>IInspectionService</code> documentation.
     */
    static final String SEPARATOR = ":";

    /**
     * Reference to the service entry point list
     */
    private ArrayList<Object> entryPoints;

    /**
     * Dex macros storage path
     */
    private File dexStorage;

    /**
     * Dex class loader
     */
    private Context context;

    /**
     * UI Handler
     */
    private Handler handler;

    /**
     * Constructor
     *
     * @param entryPoints reference to the entry point list
     */
    public InspectionStub
	(ArrayList<Object> entryPoints,
	 Context context)
    {
	this.entryPoints = entryPoints;
	this.context = context;
	this.handler = new Handler();
    }

    /**
     * Get the value of a field.
     *
     * @param f the field to get
     * @param o the object to read
     * @return the field value if no error
     */
    private Object get
	(Field f, Object o)
    {
	try {
	    f.setAccessible(true);
	    return f.get(o);
	}
	catch(final Exception e) {
	    return null;
	}
    }

    /**
     * Set the value of a field.

     * @param f the field to set
     * @param o the object to modify
     * @param value the new field value
     */
    private void set
	(Field f, Object o, Object value)
    {
	try {
	    f.setAccessible(true);
	    f.set(o, value);
	}
	catch(final Exception e) {
	}
    }

    /**
     * Invoke method of an endpoint
     *
     * @param object the object where the method is declared
     * @param method method name
     * @param params parameters array
     * @return entryPoint index or less than 0 if an error occured
     */
    public int invoke
	(final Object object,
	 final Method method,
	 final Object[] params)
	throws IllegalArgumentException
    {
	/*
	 * Call the method
	 */
	Object result = null;
	try {
	    /* Try a classic method invocation ... */
	    result = method.invoke(object, params);
	}
	catch (IllegalArgumentException e) {
	    throw e;
	}
	catch (Exception e) {
	    /* ... if it fails, then try to invoke it on the UI thread
	     * NOTE: the method will return null since it is an asynchronous
	     * invocation.
	     *
	     * TODO: Restrict this part to CalledFromWrongThreadException
	     */
	    try {
		/* Launch a Runnable inside the UI thread */
		handler.post(new Runnable(){
			public void run() {
			    try {
				/* Invoke the target method */
				method.invoke(object, params);
			    } catch (Exception e) {
				e.printStackTrace();
			    }
			}
		    });
	    } catch (Exception e2) {
		e2.printStackTrace();
	    }
	}
	/*
	 * If the result is null, return -1, otherwise store to the entry points
	 * stack and return the identifier
	 */
	if(result != null) {
	    entryPoints.add(result);
	    return entryPoints.indexOf(result);
	}
	else {
	    return -1;
	}
    }


    /**
     * List every field for an object type.
     *
     * @param o the object
     * @return a list of <code>Field</code> objects
     */
    private Vector<Field> listFields
	(Object o)
    {
	final Vector<Field> result = new Vector<Field>();
	/* Handle case object is a class */
	Class<?> c = null;
	if (!o.getClass().isAssignableFrom(Class.class))
	    c = o.getClass();
	else
	    c = (Class<?>)o;
	/* List declared fields */
	for(; c != null; c = c.getSuperclass()) {
	    result.addAll(Arrays.asList(c.getDeclaredFields()));
	}
	return result;
    }

    /**
     * List every field for an object type.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return a list of <code>Field</code> objects
     */
    private Vector<Field> listFields
	(final int entryPoint,
	 final int[] path)
    {
	return listFields(resolvePath(entryPoint, path));
    }

    /**
     * List every method for an object type.
     *
     * @param o the object
     * @retunrn a list of <code>Method</code> objects
     */
    private Vector<Method> listMethods
	(Object o)
    {
	final Vector<Method> result = new Vector<Method>();
	Class<?> c = null;
	if (!o.getClass().isAssignableFrom(Class.class))
	    c = o.getClass();
	else
	    c = (Class<?>)o;
	/* Get methods */
	for(; c != null; c = c.getSuperclass()) {
	    result.addAll(Arrays.asList(c.getDeclaredMethods()));
	}
	return result;
    }

    /**
     * List every method for an object type.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return a list of <code>Method</code> objects
     */
    private Vector<Method> listMethods
	(final int entryPoint,
	 final int[] path)
    {
	return listMethods(resolvePath(entryPoint, path));
    }

    /**
     * List constructors for a class
     *
     * @param clazz the class object
     * @return a list of <code>Constructor</code> objects
     */
    private Vector<Constructor> listConstructors
	(Class clazz)
    {
	final Vector<Constructor> result = new Vector<Constructor>();
	for(; clazz != null; clazz = clazz.getSuperclass()) {
	    result.addAll(Arrays.asList(clazz.getConstructors()));
	}
	return result;
    }


    /**
     * List constructors for a class name
     *
     * @param className the class name
     * @return a list of <code>Constructor</code> objects
     */
    private Vector<Constructor> listConstructors
    (String className)
    {
        try {
            Class<?> c = Class.forName(className);
            return listConstructors(c);
        } catch(ClassNotFoundException e) {
            return null;
        }
    }


    /**
     * List classes for a class
     *
     * @param o the object
     * @return the list of classes declared by the object
     */
    private Vector<Class> listClasses
    (Object o)
    {
	final Vector<Class> result = new Vector<Class>();
	Class<?> c = o.getClass();
	for(; c != null; c = c.getSuperclass()) {
	    result.addAll(Arrays.asList(c.getDeclaredClasses()));
	}
	return result;
    }

    /**
     * List classes for a class
     *
     * @param entryPoint the reference entry point
     * @param path the path from the entry point
     * @return a list of <code>Class</code> objects
     */
    private Vector<Class> listClasses
	(final int entryPoint,
	 final int[] path)
    {
	return listClasses(resolvePath(entryPoint, path));
    }

    /**
     * Browse a path from an entry point
     *
     * @param entryPoint the entry point
     * @param path the path
     * @return list of browsed fields
     */
    private Vector<Field> browsePath
	(final int entryPoint,
	 final int[] path)
    {
	final Vector<Field> fields = new Vector<Field>();
	Object o = entryPoints.get(entryPoint);
	for(final int i: path) {
	    final Field f = listFields(o).get(i);
	    fields.add(f);
	    o = get(f, o);
	}
	return fields;
    }

    /**
     * Resolve a path from an entry point.
     *
     * @param entryPoint the entry point
     * @param path the path
     * @return the referenced object
     */
    private Object resolvePath
	(final int entryPoint,
	 final int[] path)
    {
	Object o = entryPoints.get(entryPoint);
	for(final int i: path) {
	    /*
	     * Would be nice to return a well-typed exception once it is
	     * handled by android services
	     */
	    if(o == null)
		break;
	    final Field f = listFields(o).get(i);
	    o = get(f, o);
	}
	return o;
    }

    /**
     * Push an object to the entry point register.
     *
     * @param o the object to push
     * @return index in the entry point list
     */
    private int pushObject
	(final Object o)
    {
	if(o == null) {
	    return -1;
	}
	if(!entryPoints.contains(o)) {
	    entryPoints.add(o);
	}
	return entryPoints.indexOf(o);
    }
    
    /**
     * @see IInspectionService.getEntryPoints
     */
    public String[] getEntryPoints
	()
	throws RemoteException
    {
	Vector<String> result = new Vector<String>();
	for(final Object entryPoint: entryPoints) {
	    result.add(entryPoint.toString()
		       + SEPARATOR
		       + entryPoint.getClass().getName());
	}
	return result.toArray(new String[result.size()]);
    }

    /**
     * @see IInspectionService.filterEntryPoints
     */
    public int[] filterEntryPoints
	(String type)
    {
	final Vector<Object> filtered = new Vector<Object>();
	/*
	 * For every entry point, check if it is instance of the given class,
	 * then potentially add to the result
	 */
	try {
	    final Class<?> filter = Class.forName(type);
	    for(Object o: entryPoints)
		if(filter.isInstance(o))
		    filtered.add(o);
	} catch (final ClassNotFoundException e) {
	    e.printStackTrace();
	}
	/*
	 * Convert the list to an identifiers array then return
	 */
	final int[] result = new int[filtered.size()];
	for(int i = 0; i < filtered.size(); i++)
	    result[i] = entryPoints.indexOf(filtered.get(i));
	return result;
    }

    /**
     * @see IInspectionService.getFields
     */
    public String[] getFields
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Vector<String> result = new Vector<String>();
	for(final Field f: listFields(entryPoint, path)) {
	    result.add(f.getName()
		       + SEPARATOR
		       + Modifier.toString(f.getModifiers())
		       + " "
		       +f.getType().getName());
	}
	return result.toArray(new String[result.size()]);
    }

    /**
     * @see IInspectionService.getClasses
     */
    public String[] getClasses
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Vector<String> result = new Vector<String>();
	for(final Class c: listClasses(entryPoint, path)) {
	    result.add(c.getName()
		       + SEPARATOR
		       + c.toString());
	}
	return result.toArray(new String[]{});
    }

    /**
     * @see IInspectionService.getMethods
     */
    public String[] getMethods
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Vector<String> result = new Vector<String>();
	for(final Method m: listMethods(entryPoint, path)) {
	    result.add(m.getName()
		       + SEPARATOR
		       + m.toString());
	}
	return result.toArray(new String[]{});
    }

    /**
     * @see IInspectionService.newInstance
     */
    public int newInstance
	(final int entryPoint,
     final int[] path,
	 final int[] paramsId)
    {
	Object o = null;
	final Object[] params = new Object[paramsId.length];
	for(int i = 0; i < params.length; i++) {
	    params[i] = entryPoints.get(paramsId[i]);
	}
	/* List constructors */
	for(final Constructor c: listConstructors((Class<?>)resolvePath(entryPoint, path))) {
	    try {
		if (c.getParameterTypes().length == params.length) {
		    o = c.newInstance(params);
		    /* Push as an entrypoint */
		    if (o != null)
			return pushObject(o);
		}
	    } catch(InstantiationException e) {
		return -1;
	    } catch(IllegalArgumentException e) {
	    } catch(InvocationTargetException e) {
		return -2;
	    } catch(Exception e) {
		return -3;
	    }
	}
	/* Error */
	return -1;
    }

    /**
     * @see IInspectionService.getPath
     */
    public String getPath
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	String result = resolvePath(entryPoint, path).toString();
	for(final Field f: browsePath(entryPoint, path))
	    result += SEPARATOR + f.getName();
	return result;
    }

    /**
     * @see IInspectionService.getType
     */
    public String getType
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Object o = resolvePath(entryPoint, path);
	return (o == null) ? "null" : o.getClass().getName();
    }

    /**
     * @see IInspectionService.getTypes
     */
    public String[] getTypes
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Vector<String> result = new Vector<String>();
	final Object o = resolvePath(entryPoint, path);
	if(o == null) {
	    result.add("null");
	}
	else {
	    Class<?> c = o.getClass();
	    result.add(c.getName());
	    for(; c != null; c = c.getSuperclass()) {
		result.add(c.getName());
		/* Add every implemented interfaces */
		for (Class i: c.getInterfaces())
		    result.add(i.getName());
	    }
	}
	return result.toArray(new String[result.size()]);
    }


    /**
     * @see IInspectionService.getClass
     */
    public int getClass
    (final String className)
    {
        try {
            Class<?> c = Class.forName(className);
            return pushObject(c);
        }
        catch(ClassNotFoundException e) {
            return -1;
        }
    }


    /**
     * @see IInspectionService.getValue
     */
    public String getValue
	(final int entryPoint,
	 final int[] path)
    {
	final Object o = resolvePath(entryPoint, path);
	return (o == null) ? "null" : o.toString();
    }

    /**
     * @see IInspectionService.setValue
     */
    public void setValue
	(final int entryPoint,
	 final int[] path,
	 final int value)
	throws RemoteException
    {
	if(path.length > 0) {
	    final int[] parent = new int[path.length - 1];
	    System.arraycopy(path, 0, parent, 0, parent.length);
        if (value >= 0)
	        set(browsePath(entryPoint, path).get(parent.length),
		    resolvePath(entryPoint, parent),
	    	entryPoints.get(value));
        else
            set(browsePath(entryPoint, path).get(parent.length),
            resolvePath(entryPoint, parent),
            null);
	}
    }

    /**
     * @see IInspectionService.getMethodName
     */
    public String getMethodName
	(final int entryPoint,
	 final int[] path,
	 final int method)
	throws RemoteException
    {
	return listMethods(resolvePath(entryPoint, path))
	    .get(method)
	    .toString();
    }

    /**
     * @see IInspectionService.getMethodParams
     */
    public String[] getMethodParams
	(final int entryPoint,
	 final int[] path,
	 final int method,
	 final int[] parameters)
	throws RemoteException
    {
	final Class<?>[] params = listMethods(resolvePath(entryPoint, path))
	    .get(method)
	    .getParameterTypes();
	String[] result = new String[params.length];
	for(int i = 0; i < params.length; i++) {
	    result[i] = "";
	    if(parameters.length == params.length) {
		result[i] += (parameters[i] >= 0) ?
		    entryPoints.get(parameters[i]).toString() : "-";
		result[i] += SEPARATOR;
	    }
	    result[i] += params[i].getName();
	}
	return result;
    }


    /**
     * @see IInspectionService.invokeMethod
     */
    public int invokeMethod
	(final int entryPoint,
	 final int[] path,
	 final int method,
	 final int[] paramsId)
	throws RemoteException
    {
	/*
	 * Fetch the method and prepare the parameters
	 */
	final Method m = listMethods(resolvePath(entryPoint, path))
	    .get(method);
	m.setAccessible(true);
	final Object[] params = new Object[paramsId.length];
	for(int i = 0; i < params.length; i++) {
        if (paramsId[i]<0)
            params[i] = null;
        else
	        params[i] = entryPoints.get(paramsId[i]);
	}
	/*
	 * Call the method
	 */
	try {
	    return invoke(resolvePath(entryPoint, path), m, params);
	} catch (IllegalArgumentException e) {
	    return -1;
	}
    }

    /**
     * @see IInspectionService.invokeMethodByName
     */
    public int invokeMethodByName
	(final int entryPoint,
	 final int[] path,
	 final String method,
	 final int[] paramsId)
	throws RemoteException
    {
	/* Build the parameters objects */
	final Object[] params = new Object[paramsId.length];
	for(int i = 0; i < params.length; i++) {
        if (paramsId[i]<0)
            params[i] = null;
        else
	        params[i] = entryPoints.get(paramsId[i]);
	}
    
	Object o = resolvePath(entryPoint, path);
	/* Loop on methods with the same name and try all of them */
	for (Method m : listMethods(o)) {
	    if (m.getName().equals(method)) {
		m.setAccessible(true);
		try {
		    return invoke(o, m, params);
		} catch (IllegalArgumentException e) {
		}
	    }
	}
	return -2;
    }


    /**
     * @see IInspectionService.isIterable
     */
    public boolean isIterable
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Object o = resolvePath(entryPoint, path);
	return o.getClass().isArray() || (o instanceof Iterable);
    }

    /**
     * @see IInspectionService.listIterable
     */
    public String[] getIterable
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	final Object o = resolvePath(entryPoint, path);
	final Vector<String> result = new Vector<String>();
	/*
	 * For arrays, use array reflection API that allows browsing native
	 * arrays
	 */
	if(o.getClass().isArray()) {
	    for(int i = 0; i < Array.getLength(o); i++) {
		final Object item = Array.get(o, i);
		result.add(item.toString()
			   + SEPARATOR
			   + item.getClass().getName());
	    }
	}
	/*
	 * For any other iterable implementing the Java Interface, simply cast
	 * and use Iterable methods
	 */
	else if(o instanceof Iterable<?>) {
	    for(final Object i: (Iterable<?>) o) {
		result.add(i.toString()
			   + SEPARATOR
			   + i.getClass().getName());
	    }
	}
	return result.toArray(new String[result.size()]);
    }

    /**
     * @see IInspectionService.getIterableItem
     */
    public int getIterableItem
	(final int entryPoint,
	 final int[] path,
	 final int item)
	throws RemoteException
    {
	final Object o = resolvePath(entryPoint, path);
	int result = 0;
	if(o.getClass().isArray()) {
	    result = pushObject(Array.get(o, item));
	}
	else if(o instanceof Iterable<?>) {
	    final Iterator<?> it = ((Iterable<?>) o).iterator();
	    Object r = null;
	    for(int i = 0; i <= item; i++) {
		r = it.next();
	    }
	    result = pushObject(r);
	}
	return result;
    }

    /**
     * @see IInspectionService.pushString
     */
    public int pushString
	(final String s)
	throws RemoteException
    {
	return pushObject(s);
    }

    /**
     * @see IInspectionService.pushInt
     */
    public int pushInt
	(final int i)
	throws RemoteException
    {
	return pushObject(i);
    }

    /**
     * @see IInspectionService.pushBoolean
     */
    public int pushBoolean
	(final boolean b)
	throws RemoteException
    {
	return pushObject(b);
    }

    /**
     * @see IInspectionService.push
     */
    public int push
	(final int entryPoint,
	 final int[] path)
	throws RemoteException
    {
	return pushObject(resolvePath(entryPoint, path));
    }

    /**
     * @see IInspectionService.loadMacro
     */
    public int loadMacro
	(final String name,
	 final String dex)
	throws RemoteException
    {
	/*
	 * Then try and load the class
	 */
	try {
        String tmpname = UUID.randomUUID().toString()+".jar";
        final File internal = new File
	        (this.context.getDir("dex", Context.MODE_PRIVATE), tmpname);
	    final File optimized = this.context.getDir("outdex", Context.MODE_PRIVATE);
	    final FileOutputStream fos = new FileOutputStream(internal);
	    fos.write(Base64.decode(dex, Base64.DEFAULT));
	    fos.close();
	    DexClassLoader loader = new DexClassLoader
	        (internal.getAbsolutePath(),
	        optimized.getAbsolutePath(),
	        null,
	        this.context.getClassLoader());
	    Class clazz = loader.loadClass(name);
	    return pushObject(clazz/*(IMacro)clazz.newInstance()*/);
	}
	catch(Exception e) {
	    e.printStackTrace(); //TODO debug
        return -1;
	}
    }
}
