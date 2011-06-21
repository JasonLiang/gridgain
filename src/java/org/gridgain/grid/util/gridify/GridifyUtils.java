// Copyright (C) GridGain Systems, Inc. Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.util.gridify;

import org.gridgain.grid.gridify.*;
import org.gridgain.grid.typedef.internal.*;
import org.jetbrains.annotations.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility class with common methods used in gridify annotations.
 *
 * @author 2005-2011 Copyright (C) GridGain Systems, Inc.
 * @version 3.1.1c.21062011
 */
public final class GridifyUtils {
    /** */
    public static final int UNKNOWN_SIZE = -1;

    /** Allowed method types for return type in method. */
    private static final Class<?>[] ALLOWED_MTD_RETURN_TYPES = new Class<?>[] {
        Iterable.class,
        Iterator.class,
        Enumeration.class,
        Collection.class,
        Set.class,
        List.class,
        Queue.class,
        CharSequence.class
    };

    /** Allowed method types for input param in method for split. */
    private static final Class<?>[] ALLOWED_MTD_PARAM_TYPES = new Class<?>[] {
        Iterable.class,
        Iterator.class,
        Enumeration.class,
        Collection.class,
        Set.class,
        List.class,
        Queue.class,
        CharSequence.class
    };

    /**
     * Enforces singleton.
     */
    private GridifyUtils() {
        // No-op.
    }

    /**
     * Gets length of elements in container object with unknown type.
     * There is no ability to get size of elements for some object types
     * and method returns {@link #UNKNOWN_SIZE} value.
     *
     * @param obj Container object with elements.
     * @return Elements size of {@code UNKNOWN_SIZE}.
     */
    public static int getLength(Object obj) {
        if (obj == null) {
            return 0;
        }
        else if (obj instanceof Collection) {
            return ((Collection<?>)obj).size();
        }
        else if (obj instanceof CharSequence) {
            return ((CharSequence)obj).length();
        }
        else if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        }

        return UNKNOWN_SIZE;
    }

    /**
     * Gets iterator or create new for container object with elements with unknown type.
     *
     * @param obj Container object with elements.
     * @return Iterator.
     */
    public static Iterator<?> getIterator(final Object obj) {
        assert obj != null;

        if (obj instanceof Iterable) {
            return ((Iterable<?>)obj).iterator();
        }
        else if (obj instanceof Enumeration) {
            final Enumeration<?> enumeration = (Enumeration<?>)obj;

            return new Iterator<Object>() {
                @Override public boolean hasNext() {
                    return enumeration.hasMoreElements();
                }

                @Override public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    return enumeration.nextElement();
                }

                @Override public void remove() {
                    throw new UnsupportedOperationException("Not implemented.");
                }
            };
        }
        else if (obj instanceof Iterator) {
            return (Iterator<?>)obj;
        }
        else if (obj instanceof CharSequence) {
            final CharSequence charSeq = (CharSequence)obj;

            return new Iterator<Object>() {
                private int idx;

                @Override public boolean hasNext() {
                    return idx < charSeq.length();
                }

                @Override public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    idx++;

                    return charSeq.charAt(idx - 1);
                }

                @Override public void remove() {
                    throw new UnsupportedOperationException("Not implemented.");
                }
            };
        }
        else if (obj.getClass().isArray()) {
            return new Iterator<Object>() {
                private int idx;

                @Override public boolean hasNext() {
                    return idx < Array.getLength(obj);
                }

                @Override public Object next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    idx++;

                    return Array.get(obj, idx - 1);
                }

                @Override public void remove() {
                    throw new UnsupportedOperationException("Not implemented.");
                }
            };
        }

        throw new IllegalArgumentException("Unknown parameter type: " + obj.getClass().getName());
    }

    /**
     * Check if method return type can be used for {@link GridifySetToSet}
     * or {@link GridifySetToValue} annotations.
     *
     * @param cls Method return type class to check.
     * @return {@code true} if method return type is valid.
     */
    public static boolean isMethodReturnTypeValid(Class<?> cls) {
        for (Class<?> mtdReturnType : ALLOWED_MTD_RETURN_TYPES) {
            if (mtdReturnType.equals(cls)) {
                return true;
            }
        }

        return cls.isArray();
    }

    /**
     * Check if method parameter type type can be used for {@link GridifySetToSet}
     * or {@link GridifySetToValue} annotations.
     *
     * @param cls Method parameter to check.
     * @return {@code true} if method parameter type is valid.
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    public static boolean isMethodParameterTypeAllowed(Class<?> cls) {
        for (Class<?> mtdReturnType : ALLOWED_MTD_PARAM_TYPES) {
            if (mtdReturnType.equals(cls)) {
                return true;
            }
        }

        if (cls.isArray()) {
            return true;
        }

        int mod = cls.getModifiers();

        if (!Modifier.isInterface(mod) && !Modifier.isAbstract(mod) && Collection.class.isAssignableFrom(cls)) {
            Constructor[] ctors = cls.getConstructors();

            for (Constructor ctor : ctors) {
                try {
                    if (ctor.getParameterTypes().length == 0 && ctor.newInstance() != null) {
                        return true;
                    }
                }
                catch (InstantiationException e) {
                    // No-op.
                }
                catch (IllegalAccessException e) {
                    // No-op.
                }
                catch (InvocationTargetException e) {
                    // No-op.
                }
            }
        }


        return false;
    }

    /**
     * Check is method parameter annotated with {@link GridifyInput}.
     *
     * @param anns Annotations for method parameters.
     * @return {@code true} if annotation found.
     */
    public static boolean isMethodParameterTypeAnnotated(Annotation[] anns) {
        if (anns != null && anns.length > 0) {
            for (Annotation ann : anns) {
                if (ann.annotationType().equals(GridifyInput.class)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Allowed method return types.
     *
     * @return List of class names or text comments.
     */
    public static List<String> getAllowedMethodReturnTypes() {
        List<String> types = new ArrayList<String>(ALLOWED_MTD_RETURN_TYPES.length + 1);

        for (Class<?> type : ALLOWED_MTD_RETURN_TYPES) {
            types.add(type.getName());
        }

        types.add("Java Array");

        return types;
    }

    /**
     * Allowed method return types.
     *
     * @return List of class names or text comments.
     */
    public static Collection<String> getAllowedMethodParameterTypes() {
        Collection<String> types = new ArrayList<String>(ALLOWED_MTD_PARAM_TYPES.length + 1);

        for (Class<?> type : ALLOWED_MTD_PARAM_TYPES) {
            types.add(type.getName());
        }

        types.add("Java Array");

        return Collections.unmodifiableCollection(types);
    }

    /**
     * Converts parameter object to {@link Collection}.
     *
     * @param arg Method parameter object.
     * @return Collection of parameters or {@code null} for unknown object.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable
    public static Collection parameterToCollection(Object arg) {
        if (arg instanceof Collection) {
            return (Collection)arg;
        }
        else if (arg instanceof Iterator) {
            Collection res = new ArrayList();

            for (Iterator iter = (Iterator)arg; iter.hasNext();) {
                res.add(iter.next());
            }

            return res;
        }
        else if (arg instanceof Iterable) {
            Collection res = new ArrayList();

            for (Object o : ((Iterable)arg)) {
                res.add(o);
            }

            return res;
        }
        else if (arg instanceof Enumeration) {
            Collection res = new ArrayList();

            Enumeration elements = (Enumeration)arg;

            while (elements.hasMoreElements()) {
                res.add(elements.nextElement());
            }

            return res;
        }
        else if (arg != null && arg.getClass().isArray()) {
            Collection res = new ArrayList();

            for (int i = 0; i < Array.getLength(arg); i++) {
                res.add(Array.get(arg, i));
            }

            return res;
        }
        else if (arg instanceof CharSequence) {
            CharSequence elements = (CharSequence)arg;

            Collection<Character> res = new ArrayList<Character>(elements.length());

            for (int i = 0; i < elements.length(); i++) {
                res.add(elements.charAt(i));
            }

            return res;
        }

        return null;
    }

    /**
     * Converts {@link Collection} back to object applied for method.
     *
     * @param paramCls Method parameter type.
     * @param data Collection of data elements.
     * @return Object applied for method.
     */
    @SuppressWarnings({"unchecked"})
    @Nullable
    public static Object collectionToParameter(Class<?> paramCls, Collection data) {
        if (Collection.class.equals(paramCls)) {
            return data;
        }
        else if (Iterable.class.equals(paramCls)) {
            return data;
        }
        else if (Iterator.class.equals(paramCls)) {
            return new IteratorAdapter(data);
        }
        else if (Enumeration.class.equals(paramCls)) {
            return new EnumerationAdapter(data);
        }
        else if (Set.class.equals(paramCls)) {
            return new HashSet(data);
        }
        else if (List.class.equals(paramCls)) {
            return new LinkedList(data);
        }
        else if (Queue.class.equals(paramCls)) {
            return new LinkedList(data);
        }
        else if (CharSequence.class.equals(paramCls)) {
            SB sb = new SB();

            for (Object obj : data) {
                assert obj instanceof Character;

                sb.a(obj);
            }

            return sb;
        }
        else if (paramCls.isArray()) {
            Class<?> componentType = paramCls.getComponentType();

            Object arr = Array.newInstance(componentType, data.size());

            int i = 0;

            for (Object element : data) {
                Array.set(arr, i, element);

                i++;
            }

            return arr;
        }
        // Note, that parameter class must contain default non-param constructor.
        else if (Collection.class.isAssignableFrom(paramCls)) {
            try {
                Collection col = (Collection)paramCls.newInstance();

                for (Object dataObj : data) {
                    col.add(dataObj);
                }

                return col;
            }
            catch (InstantiationException e) {
                // No-op.
            }
            catch (IllegalAccessException e) {
                // No-op.
            }
        }

        return null;
    }

    /**
     * Serializable {@link Enumeration} implementation based on {@link Collection}.
     *
     * @author 2005-2011 Copyright (C) GridGain Systems, Inc.
     */
    private static class EnumerationAdapter<T> implements Enumeration<T>, Serializable {
        /** */
        private Collection<T> col;

        /** */
        private transient Iterator<T> iter;

        /**
         * Creates enumeration.
         *
         * @param col Collection.
         */
        private EnumerationAdapter(Collection<T> col) {
            this.col = col;

            iter = col.iterator();
        }

        /** {@inheritDoc} */
        @Override public boolean hasMoreElements() {
            return iter.hasNext();
        }

        /** {@inheritDoc} */
        @Override public T nextElement() {
            return iter.next();
        }

        /**
         * Recreate inner state for object after deserialization.
         *
         * @param in Input stream.
         * @throws ClassNotFoundException Thrown in case of error.
         * @throws IOException Thrown in case of error.
         */
        private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
            // Always perform the default de-serialization first.
            in.defaultReadObject();

            iter = col.iterator();
        }

        /**
         * @param out Output stream
         * @throws IOException Thrown in case of error.
         */
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Perform the default serialization for all non-transient, non-static fields.
            out.defaultWriteObject();
        }
    }

    /**
     * Serializable {@link Iterator} implementation based on {@link Collection}.
     *
     * @author 2005-2011 Copyright (C) GridGain Systems, Inc.
     */
    private static class IteratorAdapter<T> implements Iterator<T>, Serializable {
        /** */
        private Collection<T> col;

        /** */
        private transient Iterator<T> iter;

        /**
         * @param col Collection.
         */
        IteratorAdapter(Collection<T> col) {
            this.col = col;

            iter = col.iterator();
        }

        /** {@inheritDoc} */
        @Override public boolean hasNext() {
            return iter.hasNext();
        }

        /** {@inheritDoc} */
        @Override public T next() {
            return iter.next();
        }

        /** {@inheritDoc} */
        @Override public void remove() {
            iter.remove();
        }

        /**
         * Recreate inner state for object after deserialization.
         *
         * @param in Input stream.
         * @throws ClassNotFoundException Thrown in case of error.
         * @throws IOException Thrown in case of error.
         */
        private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
            // Always perform the default de-serialization first.
            in.defaultReadObject();

            iter = col.iterator();
        }

        /**
         * @param out Output stream
         * @throws IOException Thrown in case of error.
         */
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Perform the default serialization for all non-transient, non-static fields.
            out.defaultWriteObject();
        }
    }
}
