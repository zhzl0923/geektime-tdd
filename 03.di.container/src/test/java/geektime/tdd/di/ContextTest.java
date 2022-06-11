package geektime.tdd.di;

import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

@Nested
public class ContextTest {
    ContextConfig config;

    TestComponent instance = new TestComponent() {
    };

    Dependency dependency = new Dependency() {
    };

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBindingTest {
        //instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("should_bind_type_to_an_injection_component")
        public void should_bind_type_to_an_injection_component(Class<? extends TestComponent> implementation) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, implementation);

            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injection_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class))
            );
        }

        static class ConstructInjection implements TestComponent {
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

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
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

        //component does not exist
        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        // could get Provider<T> from context
        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);
            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifierTest {
            @Test
            public void should_bind_instance_with_multi_qualifier() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("ChooseOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                TestComponent chooseOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChooseOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();
                assertSame(instance, chooseOne);
                assertSame(instance, skywalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                config.bind(TestComponent.class, ConstructInjection.class, new NamedLiteral("ChooseOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                TestComponent chooseOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChooseOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();
                assertSame(dependency, chooseOne.dependency());
                assertSame(dependency, skywalker.dependency());
            }

            @Test
            public void should_retrieve_bind_type_as_provider() {
                config.bind(TestComponent.class, instance, new NamedLiteral("ChoseOne"), new SkywalkerLiteral());
                Optional<Provider<TestComponent>> provider = config.getContext().get(new ComponentRef<>(new SkywalkerLiteral()) {
                });

                assertTrue(provider.isPresent());
            }


            @Test
            public void should_retrieve_empty_if_no_matched_qualifiers() {
                config.bind(TestComponent.class, instance);
                Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));

                assertTrue(component.isEmpty());
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent component = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class,
                        () -> config.bind(TestComponent.class, component, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);

                assertThrows(IllegalComponentException.class,
                        () -> config.bind(TestComponent.class, ConstructInjection.class, new TestLiteral()));
            }
        }


    }

    @Nested
    public class DependencyCheckTest {
        // dependencies not exist
        @ParameterizedTest(name = "{0}")
        @MethodSource("should_throw_exception_if_dependency_not_found")
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFoundException e = assertThrows(DependencyNotFoundException.class, () -> config.getContext());
            assertEquals(Dependency.class, e.getDependency().type());
            assertEquals(TestComponent.class, e.getComponent().type());
        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Missing Dependency Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Missing Dependency Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Missing Dependency Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Missing Dependency Provider Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Missing Dependency Provider Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Missing Dependency Provider Method", MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }


        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource("should_throw_exception_if_cyclic_dependencies_found")
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class,
                    () -> config.getContext()
            );
            HashSet<Class<?>> componentTypes = Sets.newHashSet(exception.getComponents());
            assertEquals(2, componentTypes.size());
            assertTrue(componentTypes.contains(TestComponent.class));
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

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {

            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {

            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource("should_throw_exception_if_transitive_cyclic_dependencies_found")
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);
            CyclicDependenciesFoundException exception = assertThrows(
                    CyclicDependenciesFoundException.class,
                    () -> config.getContext()
            );
            HashSet<Class<?>> componentTypes = Sets.newHashSet(exception.getComponents());
            assertEquals(3, componentTypes.size());
            assertTrue(componentTypes.contains(TestComponent.class));
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

        static class IndirectCyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public IndirectCyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class IndirectCyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class IndirectCyclicComponentInjectMethod implements TestComponent {
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
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent component) {

            }
        }

        static class CyclicDependencyProviderInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderInjectConstructor(Provider<TestComponent> component) {
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderInjectConstructor.class);
            assertTrue(config.getContext().get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifierTest {
            // dependency missing if qualifier not match
            @ParameterizedTest
            @MethodSource("should_throw_exception_if_dependency_with_qualifier_not_found")
            public void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.bind(Dependency.class, dependency);
                config.bind(TestComponent.class, component, new NamedLiteral("Owner"));

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(new Component(TestComponent.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            public static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(
                        Named.of("Inject Constructor with Qualifier", InjectConstructor.class),
                        Named.of("Inject Field with Qualifier", InjectField.class),
                        Named.of("Inject Method with Qualifier", InjectMethod.class),
                        Named.of("Provider in Inject Constructor with Qualifier", InjectConstructorProvider.class),
                        Named.of("Provider in Inject Field with Qualifier", InjectFieldProvider.class),
                        Named.of("Provider in Inject Method with Qualifier", InjectMethodProvider.class)
                ).map(Arguments::of);
            }

            static class InjectConstructor implements TestComponent {
                @Inject
                public InjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class InjectField implements TestComponent {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class InjectMethod implements TestComponent {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class InjectConstructorProvider implements TestComponent {
                @Inject
                public InjectConstructorProvider(@Skywalker Provider<Dependency> dependency) {
                }
            }

            static class InjectFieldProvider implements TestComponent {
                @Inject
                @Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider implements TestComponent {
                @Inject
                void install(@Skywalker Provider<Dependency> dependency) {
                }
            }


            // check cyclic dependencies with qualifier
            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChoseOne")
                Dependency dependency;
            }

            static class NotCyclicInjectField implements Dependency {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChoseOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            @ParameterizedTest(name = "{1} -> @Skywalker({0}) -> @Named(\"ChoseOne\") not cyclic dependencies")
            @MethodSource("should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifiers")
            public void should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifiers(Class<? extends Dependency> skywalker,
                                                                                                                      Class<? extends Dependency> notCyclic) {
                config.bind(Dependency.class, dependency, new NamedLiteral("ChoseOne"));
                config.bind(Dependency.class, skywalker, new SkywalkerLiteral());
                config.bind(Dependency.class, notCyclic);

                assertDoesNotThrow(() -> config.getContext());
            }

            public static Stream<Arguments> should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifiers() {
                ArrayList<Arguments> arguments = new ArrayList<>();
                for (Named skywalker : List.of(Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class)))
                    for (Named notCyclic : List.of(Named.of("Inject Constructor", NotCyclicInjectConstructor.class),
                            Named.of("Inject Field", NotCyclicInjectField.class),
                            Named.of("Inject Method", NotCyclicInjectMethod.class)))
                        arguments.add(Arguments.of(skywalker, notCyclic));
                return arguments.stream();
            }
        }
    }

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