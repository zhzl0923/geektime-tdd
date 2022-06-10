package geektime.tdd.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesFoundException extends RuntimeException {
    private final Set<Component> components = new HashSet<>();

    public CyclicDependenciesFoundException(Stack<Component> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class<?>[]::new);
    }
}
