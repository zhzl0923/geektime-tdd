package geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.*;

public class ContextConfig {
    private final Map<Component, ComponentProvider<?>> components;


    public ContextConfig() {
        components = new HashMap<>();
    }

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), (ComponentProvider<Type>) context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
            throw new IllegalComponentException();
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectProvider<>(implementation));
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class)))
            throw new IllegalComponentException();
        for (Annotation qualifier : qualifiers)
            components.put(new Component(type, qualifier), new InjectProvider<>(implementation));
    }

    @SuppressWarnings("unchecked")
    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            public <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return (Optional<ComponentType>) Optional.ofNullable(components.get(ref.component()))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(components.get(ref.component())).map(provider -> (ComponentType) provider.get(this));
            }
        };
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component()))
                throw new DependencyNotFoundException(component, dependency.component());
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component()))
                    throw new CyclicDependenciesFoundException(visiting);
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return List.of();
        }
    }
}

