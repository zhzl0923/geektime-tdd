package geektime.tdd.di;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ContextConfig {
    private final Map<Class<?>, ComponentProvider<?>> providers;

    public ContextConfig() {
        providers = new HashMap<>();
    }

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T, R extends T>
    void bind(Class<T> type, Class<R> implementation) {
        providers.put(type, new InjectProvider<>(implementation));
    }

    @SuppressWarnings("unchecked")
    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public <T> Optional<T> get(Class<T> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (T) provider.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency)) throw new DependencyNotFoundException(component, dependency);
            if (visiting.contains(dependency)) throw new CyclicDependenciesFoundException(visiting);
            visiting.push(dependency);
            checkDependencies(dependency, visiting);
            visiting.pop();
        }
    }

}
