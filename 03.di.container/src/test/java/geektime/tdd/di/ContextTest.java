package geektime.tdd.di;

import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBindingTest {
        //instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        //component does not exist
        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("should_bind_type_to_an_injection_component")
        public void should_bind_type_to_an_injection_component(Class<? extends Component> implementation) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, implementation);

            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injection_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class))
            );
        }

        static class ConstructInjection implements Component {
            private Dependency dependency;

            @Inject
            public ConstructInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

    }


    @Nested
    public class DependencyCheckTest {
        // dependencies not exist
        @ParameterizedTest(name = "{0}")
        @MethodSource("should_throw_exception_if_dependency_not_found")
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, e.getDependency());
            assertEquals(Component.class, e.getComponent());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Missing Dependency Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Missing Dependency Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Missing Dependency Method", MissingDependencyMethod.class))
            );
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }


        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource("should_throw_exception_if_cyclic_dependencies_found")
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class,
                    () -> config.getContext()
            );
            HashSet<Class<?>> componentTypes = Sets.newHashSet(exception.getComponents());
            assertEquals(2, componentTypes.size());
            assertTrue(componentTypes.contains(Component.class));
            assertTrue(componentTypes.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {

            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource("should_throw_exception_if_transitive_cyclic_dependencies_found")
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends Component> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);
            CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class,
                    () -> config.getContext()
            );
            HashSet<Class<?>> componentTypes = Sets.newHashSet(exception.getComponents());
            assertEquals(3, componentTypes.size());
            assertTrue(componentTypes.contains(Component.class));
            assertTrue(componentTypes.contains(Dependency.class));
            assertTrue(componentTypes.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", IndirectCyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", IndirectCyclicComponentInjectField.class),
                    Named.of("Inject Method", IndirectCyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class))) {
                    for (Named anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class IndirectCyclicComponentInjectConstructor implements Component {
            @Inject
            public IndirectCyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class IndirectCyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        static class IndirectCyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {

            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {

            }
        }
    }
}
