package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {
    private Dependency dependency = new Dependency() {
    };
    private Context context = mock(Context.class);

    @BeforeEach
    public void setup() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjectionTest {
        @Nested
        class InjectTest {
            //No args constructor
            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                Component instance = new InjectProvider<>(ComponentWithDefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            static class ComponentWithDefaultConstructor implements Component {
                public ComponentWithDefaultConstructor() {
                }
            }

            // with dependencies
            @Test
            public void should_inject_dependency_via_inject_constructor() {
                ComponentWithInjectConstructor instance = new InjectProvider<>(ComponentWithInjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectProvider<ComponentWithInjectConstructor> provider
                        = new InjectProvider<>(ComponentWithInjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            static class ComponentWithInjectConstructor implements Component {

                private Dependency dependency;

                @Inject
                public ComponentWithInjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }

                public Dependency getDependency() {
                    return dependency;
                }
            }
        }

        @Nested
        class IllegalInjectTest {
            // abstract class
            abstract class AbstractComponent implements Component {
                @Inject
                public AbstractComponent() {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectProvider<>(AbstractComponent.class));
            }

            // interface
            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectProvider<>(Component.class));
            }

            // multi inject constructors
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectProvider<>(ComponentWithMultiInjectConstructor.class)
                );
            }

            static class ComponentWithMultiInjectConstructor implements Component {
                private String name;
                private String value;

                @Inject
                public ComponentWithMultiInjectConstructor(String name, String value) {
                    this.name = name;
                    this.value = value;
                }

                @Inject
                public ComponentWithMultiInjectConstructor(String name) {
                    this.name = name;
                }
            }

            // no default constructor and inject constructor
            @Test
            public void should_throw_exception_if_no_inject_constructor_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class,
                        () -> new InjectProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }

            static class ComponentWithNoInjectConstructorNorDefaultConstructor implements Component {
                public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
                }
            }
        }
    }

    @Nested
    public class FieldInjectionTest {

        @Nested
        class InjectTest {
            static class ComponentWithFiledInjection {
                @Inject
                Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFiledInjection {
            }

            // inject field
            @Test
            public void should_inject_dependency_via_field() {

                ComponentWithFiledInjection component = new InjectProvider<>(ComponentWithFiledInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {

                SubClassWithFieldInjection component = new InjectProvider<>(SubClassWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            // should provide dependency information for field injection
            @Test
            public void should_include_field_dependency_in_dependencies() {
                InjectProvider<ComponentWithFiledInjection> provider = new InjectProvider<>(ComponentWithFiledInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

        }

        @Nested
        class IllegalInjectTest {
            // throw exception if field is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectProvider<>(FinalInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjectionTest {

        @Nested
        class InjectTest {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            //  inject method with no dependencies will be called
            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency component = new InjectProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
            }

            //  inject method with dependencies will be injected
            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                InjectMethodWithDependency component = new InjectProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);
            }

            // override inject method from superclass
            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component = new InjectProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_inject_method_with_method() {

                SubclassOverrideSuperClassWithInject component =
                        new InjectProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);

                assertEquals(1, component.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {
                SubClassOverrideSuperClassWithNoInject component = new InjectProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);
                assertEquals(0, component.superCalled);
            }

            // include dependencies from inject methods
            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectProvider<InjectMethodWithDependency> provider = new InjectProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectTest {
            // throw exception if type parameter defined
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {

                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectProvider<>(InjectMethodWithTypeParameter.class));
            }
        }
    }
}


