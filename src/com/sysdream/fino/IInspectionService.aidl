package com.sysdream.fino;


/**
 * Aim at providing a full featured remote inspection interface once the
 * implementation service has been injected into an existing package.
 *
 * As part of the project Fino for minimal footprint dynamic inspection, it is
 * expected but not required that injection of the implementation service and
 * inspection are silent unless the user specifically requires a field to be
 * modified or a remote method to be invoked.
 *
 * The inspection mechanism is driven by the entry point concept. An entry
 * is an arbitrary reference to any object in the remote application memory.
 * Memoery is explored in a semantic way by browsing from entry points to
 * object fields. A single reference is described by:
 *  - the entry point that lead to it;
 *  - the path, from field to field, from the entry point to the specific
 *    reference.
 * Both entry points and path items are passed as integer index respectively
 * from the entry point list and the field list.
 *
 * Because encoding arbitrary object to <code>Parcelable</code> strings sounds
 * tough, only <code>String</code> objects are returned. In case of typed
 * returned objects, the <code>String</code> looks like:
 *
 *   "<type>:<value>"
 *
 * @author <a href="mailto:p.jaury@sysdream.com">Pierre Jaury</a>
 * @version 1.0
 */
interface IInspectionService
{
    /**
     * List registered entry points.
     *
     * @return an array listing existing entry points
     */
    String[] getEntryPoints
	();

    /**
     * Filter registered entry points given a compatible type description.
     *
     * An registered entry point is compatible with the type described if :
     *  - the given <code>String</code> is a resolvable type name in the
     *    service context;
     *  - the entry point canonical type may be cast to the described type.
     *
     * @param type the type description
     * @return an <code>int[]</code> array of compatible entry points
     */
    int[] filterEntryPoints
	(in String type);

    /**
     * List fields available for the referenced object.
     *
     * The field list is returned in the "<type>:<name>" format.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return the list of accessible fields
     */
    String[] getFields
	(in int entryPoint,
	 in int[] path);

    /**
     * List methods available for the referenced object.
     *
     * The field list is returned in the "<full name>:<name>" format.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return the list of accessible methods
     */
    String[] getMethods
	(in int entryPoint,
	 in int[] path);

    /**
     * Generate a descriptive value of the path from the entry point.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return a dot-separated list of fields as a <code>String</code>
     */
    String getPath
	(in int entryPoint,
	 in int[] path);

    /**
     * Describe the type of the referenced object.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return the described type as a <code>String</code>
     */
    String getType
	(in int entryPoint,
	 in int[] path);


    /**
     * Get the <code>String</code> value of the referenced object.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return the object description as return by <code>toString()</code>
     */
    String getValue
	(in int entryPoint,
	 in int[] path);

    /**
     * Set an object value.
     *
     * Because the object value cannot be modified directly, only its reference
     * according to the given path is set. Thus, not every reference to the
     * object is modified and the behavior may differ depending on the entry
     * point and path used for reference.
     * For the same reason, entry point value cannot be set.
     *
     * @param entryPoint an <code>int</code> value
     * @param path an <code>int</code> value
     * @param value an <code>int</code> value
     */
    void setValue
	(in int entryPoint,
	 in int[] path,
	 in int value);

    /**
     * Get a method name.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @param method the method index from method list
     * @return the method name
     */
    String getMethodName
	(in int entryPoint,
	 in int[] path,
	 in int method);

    /**
     * Get a list of method parameter types.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @param method the method index from method list
     * @return a list of <code>String</code> description of parameter types
     */
    String[] getMethodParams
	(in int entryPoint,
	 in int[] path,
	 in int method);

    /**
     * Invoke a method given the provided parameters.
     *
     * Parameters must be provided as a list of index from the entry point
     * list. Thus, only entry points may be used as method parameters. The
     * result is returned as an entry point as well. See the <code>push</code>
     * method for creating entry point.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @param method the method index from method list
     * @param parameters the parameter list
     * @return index of the method result in the entry point list
     */
    int invokeMethod
	(in int entryPoint,
	 in int[] path,
	 in int method,
	 in int[] parameters);

    /**
     * Check if the referenced object is iterable.
     *
     * Both standard Java arrays and objects implementing the
     * <code>Iterable</code> interface are considered iterable.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return <code>true</code> if the object is iterable, <code>false</code>
     *         otherwise
     */
    boolean isIterable
	(in int entryPoint,
	 in int[] path);

    /**
     * List the contents of an iterable.
     *
     * Iterable items are returned in the same format as the
     * <code>getFields</code> method.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return the iterable contents
     */
    String[] getIterable
	(in int entryPoint,
	 in int[] path);

    /**
     * Get a specific iterable item as an entry point.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @param item the item index from the iterable list
     * @return index to the object in the entry point list
     */
    int getIterableItem
	(in int entryPoint,
	 in int[] path,
	 in int item);

    /**
     * Add a <code>String</code> to the entry point list.
     *
     * @param s the string
     * @return index of the <code>String</code> in the entry point list
     */
    int pushString
	(in String s);

    /**
     * Add an integer to the entry point list.
     *
     * @param i the integer
     * @return index of the integer in the entry point list
     */
    int pushInt
	(in int i);

    /**
     * Add a boolean to the entry point list.
     *
     * @param b the boolean
     * @return index of the integer in the entry point list
     */
    int pushBoolean
	(in boolean b);

    /**
     * Add an arbitrary object to the entry point list.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return index of the object in the entry point list
     */
    int push
	(in int entryPoint,
	 in int[] path);

    /**
     * List available macros.
     *
     * @return list of macro names
     */
    String[] listMacros
	();

    /**
     * List macros compatible with the referenced object.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @return list of compatible macro indexes
     */
    int[] filterMacros
	(in int entryPoint,
	 in int[] path);

    /**
     * List macro parameters.
     *
     * @param macro the macro index from the macro list
     * @return list of macro parameter names and types
     */
    String[] getMacroParams
	(in int macro);

    /**
     * Run the specified macro.
     *
     * @param entryPoint the reference entry point
     * @param path the path from entry point
     * @param macro the macro index from macro list
     * @param params a list of parameter index from the entry point list
     * @return index of the result as an index from the entry point list
     */
    int runMacro
	(in int entryPoint,
	 in int[] path,
	 in int macro,
	 in int[] params);

    /**
     * Load a new macro;
     *
     * Load a macro from a remote application. The macro must be sent
     * as a loadable dex class implementing the <code>IMacro</code>
     * interface. The string must be base64 encoded.
     * @param macro the loaded macro
     */
    void loadMacro
	(in String name,
	 in byte[] dex);
}
