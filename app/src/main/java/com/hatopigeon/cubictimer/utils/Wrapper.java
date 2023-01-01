package com.hatopigeon.cubictimer.utils;

/**
 * A simple wrapper for other objects. This is useful when using a {@code Loader} with an activity
 * or fragment if the same loaded object can be modified and returned as "new" data when a change
 * to the data is notified. In this case, the {@code LoaderManager} will not call
 * {@code onLoadFinished} unless the loaded object is different from the one previously delivered.
 * To work around this, the same data can be returned, but in a different {@code Wrapper} each time,
 * so {@code onLoadFinished} will be fired as expected.
 *
 * @param <T> The type of the content object being wrapped.
 *
 * @author damo
 */
public class Wrapper<T> {
    /**
     * The content being wrapped.
     */
    private final T mContent;

    /**
     * Creates a new wrapper object around the given content object.
     *
     * @param content The content for the wrapper. May be {@code null}.
     */
    private Wrapper(T content) {
        mContent = content;
    }

    /**
     * Creates a new wrapper around the given content object.
     *
     * @param content
     *     The content object for the wrapper. May be {@code null}.
     * @param <T>
     *     The type of the content object.
     *
     * @return
     *     The new wrapper containing {@code content}.
     */
    public static <T> Wrapper<T> wrap(T content) {
        return new Wrapper<>(content);
    }

    /**
     * Gets the content object from the wrapper.
     *
     * @return The content object. May be {@code null}.
     */
    public T content() {
        return mContent;
    }

    /**
     * Indicates if the content object is {@code null} or not.
     *
     * @return {@code true} if the content object is {@code null}; {@code false} otherwise.
     */
    public boolean isEmpty() {
        return mContent == null;
    }

    /**
     * Creates a new wrapper around the content object taken from this wrapper. The content object
     * may be {@code null}.
     *
     * @return
     *     The new wrapper containing the same content object as this (old) wrapper.
     */
    public Wrapper<T> rewrap() {
        return new Wrapper<>(content());
    }
}
