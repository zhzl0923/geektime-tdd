package geektime.tdd.di;

import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    default List<Class<?>> getDependencies() {
        return List.of();
    }
}
