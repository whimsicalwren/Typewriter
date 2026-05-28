package dev.wren.typewriter.internal.waytoomanyinterfaces;

import dev.wren.typewriter.builder.Builder;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface ITransformable<SELF extends ITransformable<SELF>> {

    @SuppressWarnings("unchecked")
    default SELF self() {
        return (SELF) this;
    }

    /**
     * Apply a transformation function to this {@link Builder}, returning the result.
     *
     * <p>Use this when you need to derive a value or different type from the builder — for example,
     * building into a different representation:
     *
     * <pre>{@code
     * SomeObject result = builder.transform(SomeObject::fromBuilder);
     * }</pre>
     *
     * <p>If your transformation returns the same builder type, prefer {@link #transform(UnaryOperator)}
     * instead:
     *
     * <pre>{@code
     * // Prefer this:
     * builder.transform(b -> b.foo("x").bar("y"));
     *
     * // Over these:
     * builder.transform(b -> { b.foo("x").bar("y"); return b; });
     * builder.transform(b -> b.foo("x").bar("y"));
     * }</pre>
     *
     * @param transformFunction a function that accepts this builder and returns a value
     * @return the result of applying {@code transformFunction} to this builder
     * @param <S2> the return type of the transformation
     */
    default <S2> S2 transform(Function<SELF, S2> transformFunction) {
        return transformFunction.apply(self());
    }



    /**
     * Apply a transformation unary operator to this {@link Builder}, returning the result.
     *
     * <p>Prefer this over {@link #transform(Function)} when the transformation returns the same type as the input.
     *
     * <pre>{@code
     * builder.transform(b -> b.foo("x").bar("y"));
     * }</pre>
     *
     * @param transformUnaryOp a consumer that accepts and mutates this builder
     * @return this builder, for chaining
     */
    default SELF transform(UnaryOperator<SELF> transformUnaryOp) {
        return transformUnaryOp.apply(self());
    }

    /**
     * Conditionally apply a transformation unary operator to this {@link Builder}, returning the result or the unchanged builder if the condition is false.
     **
     * <pre>{@code
     * builder.transformDependent(DistHelper.isClient(), () -> b -> b.foo("x").bar("y"))
     * }</pre>
     *
     * @param transformUnaryOp a consumer that accepts and mutates this builder
     * @return this builder, for chaining
     */
    default SELF transformDependent(boolean condition, Supplier<UnaryOperator<SELF>> transformUnaryOp) {
        if (condition) {
            return transformUnaryOp.get().apply(self());
        } else return self();
    }
}
