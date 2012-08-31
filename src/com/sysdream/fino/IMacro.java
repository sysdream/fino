package com.sysdream.fino;

/**
 * Describe an inspection macro.
 *
 * Inspection macros are pieces of code that are compatible with
 * specific object types.
 * @author <a href="mailto:p.jaury@sysdream.com">Pierre Jaury</a>
 * @version 1.0
 */
interface IMacro
{
    /**
     * Describe the current macro.
     *
     * Because description is displayed in select boxes, string length should
     * not exceed 30 chars
     * @return the macro full text description
     */
    String getDescription
	();

    /**
     * Check wether the macro is compatible with the given target or not.
     *
     * @param target the potential macro execution target
     * @return <code>true</code> if the macro is compatible, <code>false</code>
     * otherwise.
     */
    boolean isCompatible
	(Object target);

    /**
     * List parameter types expected by the macro <code>run</code> method.
     *
     * @return a parameter type list
     */
    Class<?> getParameters
	();

    /**
     * Actually run the macro.
     *
     * No parameter type check is performed before <code>run</code> is invoked,
     * types should be verified when implementing the interface if necessary
     * @param parameters macro parameters as a list
     * @return the macro returned object
     */
    Object run
	(Object[] parameters);
}
