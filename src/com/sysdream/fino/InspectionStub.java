package com.sysdream.fino;

import java.lang.reflect.*;
import java.util.*;

import android.os.RemoteException;

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
     * Constructor
     *
     * @param entryPoints reference to the entry point list
     */
    public InspectionStub
	(ArrayList<Object> entryPoints)
    {
	this.entryPoints = entryPoints;
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
     * List every field for an object type.
     *
     * @param o the object
     * @return a list of <code>Field</code> objects
     */
    private Vector<Field> listFields
	(Object o)
    {
	final Vector<Field> result = new Vector<Field>();
	Class<?> c = o.getClass();
	while(c != Object.class) {
	    result.addAll(Arrays.asList(c.getDeclaredFields()));
	    c = c.getSuperclass();
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
	Class<?> c = o.getClass();
	while(c != Object.class) {
	    result.addAll(Arrays.asList(c.getDeclaredMethods()));
	    c = c.getSuperclass();
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
	    set(browsePath(entryPoint, path).get(path.length),
		resolvePath(entryPoint, parent),
		entryPoints.get(value));
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
	 final int method)
	throws RemoteException
    {
	final Class<?>[] params = listMethods(resolvePath(entryPoint, path))
	    .get(method)
	    .getParameterTypes();
	String[] result = new String[params.length];
	for(int i = 0; i < params.length; i++) {
	    result[i] = params[i].getName();
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
	    params[i] = entryPoints.get(paramsId[i]);
	}
	/*
	 * Call the method
	 */
	Object result = null;
	try {
	    result = m.invoke(resolvePath(entryPoint, path), params);
	} catch (Exception e) {
	    e.printStackTrace();
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
     * @see IInspectionService.listMacros
     */
    public String[] listMacros
	()
	throws RemoteException
    {
	return null; //TODO
    }

    /**
     * @see IInspectionService.filterMacros
     */
    public int[] filterMacros
	(int entryPoint,
	 int[] path)
	throws RemoteException
    {
	return null; //TODO
    }

    /**
     * @see IInspectionService.getMacrosParams
     */
    public String[] getMacroParams
	(int macro)
	throws RemoteException
    {
	return null; //TODO
    }

    /**
     * @see IInspectionService.runMacro
     */
    public int runMacro
	(int entryPoint,
	 int[] path,
	 int macro,
	 int[] params)
	throws RemoteException
    {
	return -1; //TODO
    }
}
