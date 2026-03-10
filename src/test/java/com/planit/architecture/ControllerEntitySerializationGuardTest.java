package com.planit.architecture;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

class ControllerEntitySerializationGuardTest {

    @Test
    void controllersMustNotReturnJpaEntityTypes() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();

        for (Class<?> controllerClass : findControllerClasses()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                collectEntityTypeViolations(
                        method.getGenericReturnType(),
                        controllerClass.getName() + "#" + method.getName(),
                        violations
                );
            }
        }

        if (!violations.isEmpty()) {
            Assertions.fail("Controller response must not expose JPA entities:\n" + String.join("\n", violations));
        }
    }

    private List<Class<?>> findControllerClasses() throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        provider.findCandidateComponents("com.planit").forEach(candidate -> {
            try {
                classes.add(Class.forName(candidate.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to load controller class: " + candidate.getBeanClassName(), e);
            }
        });
        return classes;
    }

    private void collectEntityTypeViolations(Type type, String location, List<String> violations) {
        if (type instanceof Class<?> clazz) {
            if (isJpaEntityType(clazz)) {
                violations.add(location + " returns entity type: " + clazz.getName());
            }
            if (clazz.isArray()) {
                collectEntityTypeViolations(clazz.getComponentType(), location, violations);
            }
            return;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            collectEntityTypeViolations(parameterizedType.getRawType(), location, violations);
            for (Type arg : parameterizedType.getActualTypeArguments()) {
                collectEntityTypeViolations(arg, location, violations);
            }
            return;
        }

        if (type instanceof WildcardType wildcardType) {
            for (Type upper : wildcardType.getUpperBounds()) {
                collectEntityTypeViolations(upper, location, violations);
            }
            for (Type lower : wildcardType.getLowerBounds()) {
                collectEntityTypeViolations(lower, location, violations);
            }
            return;
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            for (Type bound : typeVariable.getBounds()) {
                collectEntityTypeViolations(bound, location, violations);
            }
            return;
        }

        if (type instanceof GenericArrayType genericArrayType) {
            collectEntityTypeViolations(genericArrayType.getGenericComponentType(), location, violations);
        }
    }

    private boolean isJpaEntityType(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class)
                || clazz.isAnnotationPresent(MappedSuperclass.class)
                || clazz.isAnnotationPresent(Embeddable.class);
    }
}
