package geektime.tdd.di;

import java.util.Optional;

interface Context {
    <ComponentType> Optional<ComponentType> get(ComponentRef<ComponentType> ref);

}
