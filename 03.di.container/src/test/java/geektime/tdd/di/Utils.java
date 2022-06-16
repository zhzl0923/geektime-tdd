package geektime.tdd.di;

import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

interface TestComponent {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {
}

interface AnotherDependency {
}

record NamedLiteral(String value) implements jakarta.inject.Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}

@java.lang.annotation.Documented
@java.lang.annotation.Retention(RUNTIME)
@jakarta.inject.Qualifier
@interface Skywalker {
}

record SkywalkerLiteral() implements Skywalker {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skywalker;
    }
}

record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}

record SingletonLiteral() implements Singleton {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}

@Scope
@Documented
@Retention(RUNTIME)
@interface Pooled {
}

record PooledLiteral() implements Pooled {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

class PoolProvider<T> implements ComponentProvider<T> {
    static int MAX = 2;
    int current;
    private List<T> pool = new ArrayList<>();
    private ComponentProvider<T> provider;

    public PoolProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pool.size() < MAX) pool.add(provider.get(context));
        return pool.get(current++ % MAX);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return provider.getDependencies();
    }
}