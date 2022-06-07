package geektime.tdd.di;

import java.util.List;

interface ComponentProvider<T> {
    T get(Context context);

    List<Class<?>> getDependencies();
}
