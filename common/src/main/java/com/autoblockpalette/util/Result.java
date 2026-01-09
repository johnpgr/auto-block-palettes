package com.autoblockpalette.util;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * A functional result type representing either a success value or an error message.
 *
 * @param <T> The type of the success value
 */
public sealed interface Result<T> {

    /**
     * Returns true if this result is a success.
     */
    boolean isSuccess();

    /**
     * Returns true if this result is an error.
     */
    default boolean isError() {
        return !isSuccess();
    }

    /**
     * Maps the success value using the provided function.
     *
     * @param mapper The function to apply to the success value
     * @param <U>    The type of the mapped value
     * @return A new Result with the mapped value, or the original error
     */
    <U> Result<U> map(Function<? super T, ? extends U> mapper);

    /**
     * FlatMaps the success value using the provided function.
     *
     * @param mapper The function to apply to the success value
     * @param <U>    The type of the mapped value
     * @return The Result from the mapper, or the original error
     */
    <U> Result<U> flatMap(Function<? super T, ? extends Result<U>> mapper);

    /**
     * Executes the given action if this is a success.
     *
     * @param action The action to execute with the success value
     * @return This result for chaining
     */
    Result<T> ifSuccess(Consumer<? super T> action);

    /**
     * Executes the given action if this is an error.
     *
     * @param action The action to execute with the error message
     * @return This result for chaining
     */
    Result<T> ifError(Consumer<String> action);

    /**
     * Returns the success value or throws an IllegalStateException with the error message.
     *
     * @return The success value
     * @throws IllegalStateException if this is an error
     */
    T getOrThrow();

    /**
     * Returns the success value or the provided default value.
     *
     * @param defaultValue The value to return if this is an error
     * @return The success value or the default value
     */
    T getOrElse(T defaultValue);

    /**
     * Creates a success result.
     *
     * @param value The success value
     * @param <T>   The type of the value
     * @return A success result containing the value
     */
    static <T> Result<T> success(@NotNull T value) {
        return new Success<>(value);
    }

    /**
     * Creates an error result.
     *
     * @param message The error message
     * @param <T>     The type of the expected value
     * @return An error result containing the message
     */
    static <T> Result<T> error(@NotNull String message) {
        return new Error<>(message);
    }

    record Success<T>(@NotNull T value) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <U> Result<U> flatMap(Function<? super T, ? extends Result<U>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Result<T> ifSuccess(Consumer<? super T> action) {
            action.accept(value);
            return this;
        }

        @Override
        public Result<T> ifError(Consumer<String> action) {
            return this;
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value;
        }
    }

    record Error<T>(@NotNull String message) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
            return (Result<U>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> flatMap(Function<? super T, ? extends Result<U>> mapper) {
            return (Result<U>) this;
        }

        @Override
        public Result<T> ifSuccess(Consumer<? super T> action) {
            return this;
        }

        @Override
        public Result<T> ifError(Consumer<String> action) {
            action.accept(message);
            return this;
        }

        @Override
        public T getOrThrow() {
            throw new IllegalStateException(message);
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }
    }
}

