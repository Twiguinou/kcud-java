package showoff;

import java.util.Optional;
import java.util.function.Consumer;

public interface Either<L, R>
{
    Optional<L> left();
    Optional<R> right();
    void ifLeft(Consumer<L> task);
    void ifRight(Consumer<R> task);
    void apply(Consumer<L> left_task, Consumer<R> right_task);

    static <L, R> Either<L, R> ofLeft(final L value)
    {
        return new Either<>()
        {
            @Override public Optional<L> left()
            {
                return Optional.of(value);
            }
            @Override public Optional<R> right()
            {
                return Optional.empty();
            }
            @Override public void ifLeft(Consumer<L> task)
            {
                task.accept(value);
            }
            @Override public void ifRight(Consumer<R> task) {}
            @Override public void apply(Consumer<L> left_task, Consumer<R> right_task)
            {
                left_task.accept(value);
            }
        };
    }

    static <L, R> Either<L, R> ofRight(final R value)
    {
        return new Either<>()
        {
            @Override public Optional<L> left()
            {
                return Optional.empty();
            }
            @Override public Optional<R> right()
            {
                return Optional.of(value);
            }
            @Override public void ifLeft(Consumer<L> task) {}
            @Override public void ifRight(Consumer<R> task)
            {
                task.accept(value);
            }
            @Override
            public void apply(Consumer<L> left_task, Consumer<R> right_task)
            {
                right_task.accept(value);
            }
        };
    }

    static <L, R> Either<L, R> of(final L left_value, final R right_value)
    {
        return new Either<L, R>()
        {
            @Override public Optional<L> left()
            {
                return Optional.of(left_value);
            }
            @Override public Optional<R> right()
            {
                return Optional.of(right_value);
            }
            @Override public void ifLeft(Consumer<L> task)
            {
                task.accept(left_value);
            }
            @Override public void ifRight(Consumer<R> task)
            {
                task.accept(right_value);
            }
            @Override public void apply(Consumer<L> left_task, Consumer<R> right_task)
            {
                left_task.accept(left_value);
                right_task.accept(right_value);
            }
        };
    }
}
