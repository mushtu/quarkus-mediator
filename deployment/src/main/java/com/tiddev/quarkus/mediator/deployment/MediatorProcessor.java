package com.tiddev.quarkus.mediator.deployment;

import com.tiddev.quarkus.mediator.Mediator;
import com.tiddev.quarkus.mediator.MediatorContext;
import com.tiddev.quarkus.mediator.MediatorDefinition;
import com.tiddev.quarkus.mediator.MediatorException;
import com.tiddev.quarkus.mediator.MediatorNotificationHandler;
import com.tiddev.quarkus.mediator.MediatorRequestHandler;
import com.tiddev.quarkus.mediator.MediatorQualifier;
import com.tiddev.quarkus.mediator.MediatorRequestChain;
import com.tiddev.quarkus.mediator.MediatorRequestMiddleware;
import com.tiddev.quarkus.mediator.NotificationHandler;
import com.tiddev.quarkus.mediator.RequestMiddleware;
import com.tiddev.quarkus.mediator.RequestHandler;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.IfThenElse;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.IndexView;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Quarkus deployment processor for the mediator extension.
 */
public class MediatorProcessor {

    private static final DotName MEDIATOR_DEFINITION = DotName.createSimple(MediatorDefinition.class.getName());
    private static final DotName REQUEST_HANDLER = DotName.createSimple(MediatorRequestHandler.class.getName());
    private static final DotName NOTIFICATION_HANDLER = DotName.createSimple(MediatorNotificationHandler.class.getName());
    private static final DotName REQUEST_MIDDLEWARE = DotName.createSimple(MediatorRequestMiddleware.class.getName());
    private static final DotName REQUEST_HANDLER_TYPE = DotName.createSimple(RequestHandler.class.getName());
    private static final DotName NOTIFICATION_HANDLER_TYPE = DotName.createSimple(NotificationHandler.class.getName());
    private static final DotName REQUEST_MIDDLEWARE_TYPE = DotName.createSimple(RequestMiddleware.class.getName());

    private static final MethodDescriptor REQUIRE_NON_NULL = MethodDescriptor.ofMethod(Objects.class, "requireNonNull", Object.class, Object.class, String.class);
    private static final MethodDescriptor OBJECT_GET_CLASS = MethodDescriptor.ofMethod(Object.class, "getClass", Class.class);
    private static final MethodDescriptor REQUEST_HANDLE = MethodDescriptor.ofMethod(RequestHandler.class, "handle", Object.class, Object.class);
    private static final MethodDescriptor NOTIFICATION_HANDLE = MethodDescriptor.ofMethod(NotificationHandler.class, "handle", void.class, Object.class);
    private static final MethodDescriptor REQUEST_MIDDLEWARE_HANDLE = MethodDescriptor.ofMethod(RequestMiddleware.class, "handle", Object.class, MediatorContext.class, MediatorRequestChain.class);
    private static final MethodDescriptor MEDIATOR_CONTEXT_CTOR = MethodDescriptor.ofConstructor(MediatorContext.class, Set.class, Object.class, Class.class);
    private static final MethodDescriptor SET_ADD = MethodDescriptor.ofMethod(Set.class, "add", boolean.class, Object.class);

    @BuildStep
    BeanDefiningAnnotationBuildItem mediatorDefinitionBean() {
        return new BeanDefiningAnnotationBuildItem(MEDIATOR_DEFINITION);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem requestHandlerBean() {
        return new BeanDefiningAnnotationBuildItem(REQUEST_HANDLER);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem notificationHandlerBean() {
        return new BeanDefiningAnnotationBuildItem(NOTIFICATION_HANDLER);
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem requestMiddlewareBean() {
        return new BeanDefiningAnnotationBuildItem(REQUEST_MIDDLEWARE);
    }

    @BuildStep
    void generateMediators(CombinedIndexBuildItem combinedIndex,
                           BuildProducer<GeneratedBeanBuildItem> generatedBeans,
                           BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        IndexView index = combinedIndex.getIndex();
        List<MediatorScope> scopes = resolveScopes(index, validationErrors);
        if (scopes.isEmpty()) {
            return;
        }

        ClassOutput output = new GeneratedBeanGizmoAdaptor(generatedBeans);
        for (MediatorScope scope : scopes) {
            generateMediator(output, scope);
        }
    }

    private List<MediatorScope> resolveScopes(IndexView index, BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        List<AnnotationInstance> definitions = new ArrayList<>(index.getAnnotations(MEDIATOR_DEFINITION));
        List<MediatorScope> scopes;
        if (definitions.isEmpty()) {
            scopes = List.of(new MediatorScope("com.tiddev.quarkus.mediator.generated.GeneratedMediator", null, null, Set.of(), true));
        } else {
            scopes = new ArrayList<>(definitions.size());
            int ordinal = 0;
            for (AnnotationInstance definition : definitions) {
                ClassInfo classInfo = definition.target().asClass();
                scopes.add(new MediatorScope(
                        generatedClassName(classInfo, ordinal++),
                        classInfo.name().toString(),
                        resolveMediatorName(definition),
                        resolvePackages(classInfo, definition),
                        false));
            }
        }
        collectHandlers(index, scopes, validationErrors);
        return scopes;
    }

    private void collectHandlers(IndexView index, List<MediatorScope> scopes, BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        for (AnnotationInstance annotation : index.getAnnotations(REQUEST_HANDLER)) {
            ClassInfo classInfo = annotation.target().asClass();
            validateHandlerClass(classInfo, REQUEST_HANDLER, REQUEST_HANDLER_TYPE);
            Class<?> beanClass = loadClass(classInfo.name().toString());
            Class<?> requestType = loadClass(resolveHandlerTypeArgument(classInfo, REQUEST_HANDLER_TYPE, 0));
            for (MediatorScope scope : scopes) {
                if (scope.matches(packageName(classInfo))) {
                    registerRequestHandler(scope, requestType.getName(), new RequestHandlerModel(beanClass, requestType), validationErrors);
                }
            }
        }

        for (AnnotationInstance annotation : index.getAnnotations(NOTIFICATION_HANDLER)) {
            ClassInfo classInfo = annotation.target().asClass();
            validateHandlerClass(classInfo, NOTIFICATION_HANDLER, NOTIFICATION_HANDLER_TYPE);
            Class<?> beanClass = loadClass(classInfo.name().toString());
            Class<?> notificationType = loadClass(resolveHandlerTypeArgument(classInfo, NOTIFICATION_HANDLER_TYPE, 0));
            for (MediatorScope scope : scopes) {
                if (scope.matches(packageName(classInfo))) {
                    scope.notificationHandlers
                            .computeIfAbsent(notificationType.getName(), ignored -> new ArrayList<>())
                            .add(new NotificationHandlerModel(beanClass, notificationType));
                }
            }
        }

        for (AnnotationInstance annotation : index.getAnnotations(REQUEST_MIDDLEWARE)) {
            ClassInfo classInfo = annotation.target().asClass();
            validateHandlerClass(classInfo, REQUEST_MIDDLEWARE, REQUEST_MIDDLEWARE_TYPE);
            Class<?> beanClass = loadClass(classInfo.name().toString());
            String requestTypeName = resolveHandlerTypeArgument(classInfo, REQUEST_MIDDLEWARE_TYPE, 0);
            int order = annotation.value("order") == null ? 0 : annotation.value("order").asInt();
            RequestMiddlewareModel middleware = new RequestMiddlewareModel(beanClass, loadClass(requestTypeName), order);
            boolean registered = false;
            for (MediatorScope scope : scopes) {
                if (scope.matches(packageName(classInfo)) && scope.requestHandlers.containsKey(requestTypeName)) {
                    scope.requestMiddlewares.computeIfAbsent(requestTypeName, ignored -> new ArrayList<>()).add(middleware);
                    registered = true;
                }
            }
            if (!registered) {
                validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(new IllegalStateException(
                        "Request middleware " + beanClass.getName()
                                + " targets request type " + requestTypeName
                                + " but no matching request handler was found in any mediator scope that includes the bean package.")));
            }
        }

        for (MediatorScope scope : scopes) {
            for (List<RequestMiddlewareModel> middlewares : scope.requestMiddlewares.values()) {
                middlewares.sort(Comparator.comparingInt(RequestMiddlewareModel::order).thenComparing(model -> model.beanClass().getName()));
            }
        }
    }

    private void registerRequestHandler(MediatorScope scope,
                                        String requestTypeName,
                                        RequestHandlerModel handler,
                                        BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
        RequestHandlerModel existing = scope.requestHandlers.putIfAbsent(requestTypeName, handler);
        if (existing != null) {
            validationErrors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(new IllegalStateException(
                    "Duplicate mediator request handler for request type " + requestTypeName
                            + " in scope " + scopeLabel(scope)
                            + ". Found handlers: " + existing.beanClass().getName()
                            + " and " + handler.beanClass().getName()
                            + ". Keep only one @MediatorRequestHandler for each request type in a scope.")));
        }
    }

    private void validateHandlerClass(ClassInfo classInfo, DotName annotationName, DotName requiredInterface) {
        if (Modifier.isAbstract(classInfo.flags())) {
            throw new IllegalStateException("Abstract class " + classInfo.name() + " cannot be annotated with " + annotationName);
        }
        if (classInfo.interfaceNames().stream().noneMatch(requiredInterface::equals)) {
            throw new IllegalStateException("Class " + classInfo.name() + " is annotated with " + annotationName + " but does not implement " + requiredInterface);
        }
    }

    private String resolveHandlerTypeArgument(ClassInfo classInfo, DotName requiredInterface, int argumentIndex) {
        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (!interfaceType.name().equals(requiredInterface)) {
                continue;
            }
            if (!(interfaceType instanceof ParameterizedType parameterizedType)) {
                throw new IllegalStateException("Class " + classInfo.name() + " must parameterize " + requiredInterface);
            }
            List<Type> arguments = parameterizedType.arguments();
            if (argumentIndex >= arguments.size()) {
                throw new IllegalStateException("Class " + classInfo.name() + " does not declare type argument " + argumentIndex + " for " + requiredInterface);
            }
            Type argument = arguments.get(argumentIndex);
            if (argument.kind() != Type.Kind.CLASS) {
                throw new IllegalStateException("Class " + classInfo.name() + " must use a concrete class type for " + requiredInterface + " type argument " + argumentIndex);
            }
            return argument.name().toString();
        }
        throw new IllegalStateException("Class " + classInfo.name() + " does not directly implement " + requiredInterface);
    }

    private void generateMediator(ClassOutput output, MediatorScope scope) {
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(output)
                .className(scope.generatedClassName)
                .interfaces(Mediator.class)
                .build();
        try {
            classCreator.addAnnotation(ApplicationScoped.class);
            if (scope.definitionClassName != null) {
                classCreator.addAnnotation(MediatorQualifier.class).addValue("value", loadClass(scope.definitionClassName));
            }
            if (scope.mediatorName != null) {
                classCreator.addAnnotation(Named.class).addValue("value", scope.mediatorName);
            }

            for (RequestHandlerModel handler : scope.requestHandlers.values()) {
                handler.setField(createInjectedField(classCreator, handler.beanClass(), fieldName(handler.beanClass(), "requestHandler")));
            }
            for (List<NotificationHandlerModel> handlers : scope.notificationHandlers.values()) {
                for (NotificationHandlerModel handler : handlers) {
                    handler.setField(createInjectedField(classCreator, handler.beanClass(), fieldName(handler.beanClass(), "notificationHandler")));
                }
            }
            for (List<RequestMiddlewareModel> middlewares : scope.requestMiddlewares.values()) {
                for (RequestMiddlewareModel middleware : middlewares) {
                    middleware.setField(createInjectedField(classCreator, middleware.beanClass(), fieldName(middleware.beanClass(), "requestMiddleware")));
                }
            }

            createSendMethod(classCreator, scope);
            createPublishMethod(classCreator, scope);
        } finally {
            classCreator.close();
        }
    }

    private FieldDescriptor createInjectedField(ClassCreator classCreator, Class<?> beanClass, String fieldName) {
        FieldCreator field = classCreator.getFieldCreator(fieldName, beanClass);
        field.setModifiers(Modifier.PUBLIC);
        field.addAnnotation(Inject.class);
        return field.getFieldDescriptor();
    }

    private void createSendMethod(ClassCreator classCreator, MediatorScope scope) {
        MethodCreator method = classCreator.getMethodCreator("send", Object.class, Object.class);
        method.setParameterNames(new String[] {"request"});
        try {
            ResultHandle request = method.getMethodParam(0);
            ResultHandle validatedRequest = method.invokeStaticMethod(REQUIRE_NON_NULL, request, method.load("request"));
            emitRequestDispatch(method, scope, validatedRequest, method.getThis(), 0);
        } finally {
            method.close();
        }
    }

    private void createPublishMethod(ClassCreator classCreator, MediatorScope scope) {
        MethodCreator method = classCreator.getMethodCreator("publish", Void.TYPE, Object.class);
        method.setParameterNames(new String[] {"notification"});
        try {
            ResultHandle notification = method.getMethodParam(0);
            ResultHandle validatedNotification = method.invokeStaticMethod(REQUIRE_NON_NULL, notification, method.load("notification"));
            emitNotificationDispatch(method, scope, validatedNotification, method.getThis());
            method.returnVoid();
        } finally {
            method.close();
        }
    }

    private ResultHandle createPackageSet(BytecodeCreator bytecodeCreator, Set<String> packages) {
        ResultHandle set = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(LinkedHashSet.class));
        for (String packageName : packages) {
            bytecodeCreator.invokeInterfaceMethod(SET_ADD, set, bytecodeCreator.load(packageName));
        }
        return set;
    }

    private void emitRequestDispatch(BytecodeCreator bytecodeCreator, MediatorScope scope, ResultHandle request, ResultHandle self, int index) {
        List<RequestHandlerModel> handlers = new ArrayList<>(scope.requestHandlers.values());
        if (handlers.isEmpty()) {
            bytecodeCreator.throwException(MediatorException.class, "No request handler registered for this mediator scope");
            return;
        }
        if (index >= handlers.size()) {
            bytecodeCreator.throwException(MediatorException.class, "No request handler registered for this request type");
            return;
        }

        RequestHandlerModel handler = handlers.get(index);
        IfThenElse branch = bytecodeCreator.ifThenElse(bytecodeCreator.instanceOf(request, handler.requestType()));
        BytecodeCreator then = branch.then();
        ResultHandle typedRequest = then.checkCast(request, handler.requestType());
        ResultHandle packages = createPackageSet(then, scope.packages);
        ResultHandle context = then.newInstance(MEDIATOR_CONTEXT_CTOR, packages, typedRequest, then.loadClass(handler.requestType()));
        emitRequestMiddlewareChain(then, scope, handler.requestType().getName(), 0, typedRequest, context, self, handler);
        emitRequestDispatch(branch.elseThen(), scope, request, self, index + 1);
    }

    private void emitRequestMiddlewareChain(BytecodeCreator bytecodeCreator,
                                            MediatorScope scope,
                                            String requestTypeName,
                                            int index,
                                            ResultHandle request,
                                            ResultHandle context,
                                            ResultHandle self,
                                            RequestHandlerModel handler) {
        List<RequestMiddlewareModel> middlewares = scope.requestMiddlewares.get(requestTypeName);
        if (middlewares == null || index >= middlewares.size()) {
            ResultHandle handlerBean = bytecodeCreator.readInstanceField(handler.field(), self);
            ResultHandle response = bytecodeCreator.invokeInterfaceMethod(REQUEST_HANDLE, handlerBean, request);
            bytecodeCreator.returnValue(response);
            return;
        }

        RequestMiddlewareModel middleware = middlewares.get(index);
        ResultHandle middlewareBean = bytecodeCreator.readInstanceField(middleware.field(), self);
        FunctionCreator chain = bytecodeCreator.createFunction(MediatorRequestChain.class);
        BytecodeCreator chainBytecode = chain.getBytecode();
        ResultHandle nextRequest = chainBytecode.getMethodParam(0);
        emitRequestMiddlewareChain(chainBytecode, scope, requestTypeName, index + 1, nextRequest, context, self, handler);
        bytecodeCreator.returnValue(bytecodeCreator.invokeInterfaceMethod(REQUEST_MIDDLEWARE_HANDLE, middlewareBean, context, chain.getInstance()));
    }

    private void emitNotificationDispatch(BytecodeCreator bytecodeCreator, MediatorScope scope, ResultHandle notification, ResultHandle self) {
        for (List<NotificationHandlerModel> handlers : scope.notificationHandlers.values()) {
            for (NotificationHandlerModel handler : handlers) {
                BytecodeCreator then = bytecodeCreator.ifTrue(bytecodeCreator.instanceOf(notification, handler.notificationType())).trueBranch();
                ResultHandle typedNotification = then.checkCast(notification, handler.notificationType());
                ResultHandle handlerBean = then.readInstanceField(handler.field(), self);
                then.invokeInterfaceMethod(NOTIFICATION_HANDLE, handlerBean, typedNotification);
            }
        }
    }

    private Set<String> resolvePackages(ClassInfo classInfo, AnnotationInstance definition) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        AnnotationValue packagesValue = definition.value("packages");
        if (packagesValue == null) {
            packages.add(packageName(classInfo));
        } else {
            for (AnnotationValue value : packagesValue.asArrayList()) {
                String candidate = value.asString();
                if (candidate != null && !candidate.isBlank()) {
                    packages.add(candidate.trim());
                }
            }
        }
        if (packages.isEmpty()) {
            throw new IllegalStateException("Mediator definition on " + classInfo.name() + " must specify at least one package");
        }
        return Set.copyOf(packages);
    }

    private String resolveMediatorName(AnnotationInstance definition) {
        AnnotationValue nameValue = definition.value("name");
        if (nameValue == null) {
            return null;
        }
        String name = nameValue.asString();
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private String generatedClassName(ClassInfo classInfo, int ordinal) {
        String sanitized = classInfo.name().toString().replace('.', '_').replace('$', '_');
        return "com.tiddev.quarkus.mediator.generated.GeneratedMediator_" + sanitized + "_" + ordinal;
    }

    private String fieldName(Class<?> beanClass, String suffix) {
        return beanClass.getName().replace('.', '_').replace('$', '_') + "_" + suffix;
    }

    private String scopeLabel(MediatorScope scope) {
        if (scope.mediatorName != null) {
            return "'" + scope.mediatorName + "'";
        }
        if (scope.definitionClassName != null) {
            return scope.definitionClassName;
        }
        return "the default mediator scope";
    }

    private String packageName(ClassInfo classInfo) {
        String className = classInfo.name().toString();
        int lastDot = className.lastIndexOf('.');
        return lastDot < 0 ? "" : className.substring(0, lastDot);
    }

    private Class<?> loadClass(String className) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader != null) {
                return Class.forName(className, false, loader);
            }
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load class " + className, e);
        }
    }

    private static final class MediatorScope {
        private final String generatedClassName;
        private final String definitionClassName;
        private final String mediatorName;
        private final Set<String> packages;
        private final boolean allPackages;
        private final LinkedHashMap<String, RequestHandlerModel> requestHandlers = new LinkedHashMap<>();
        private final LinkedHashMap<String, List<NotificationHandlerModel>> notificationHandlers = new LinkedHashMap<>();
        private final LinkedHashMap<String, List<RequestMiddlewareModel>> requestMiddlewares = new LinkedHashMap<>();

        private MediatorScope(String generatedClassName, String definitionClassName, String mediatorName, Set<String> packages, boolean allPackages) {
            this.generatedClassName = generatedClassName;
            this.definitionClassName = definitionClassName;
            this.mediatorName = mediatorName;
            this.packages = Set.copyOf(packages);
            this.allPackages = allPackages;
        }

        private boolean matches(String packageName) {
            if (allPackages) {
                return true;
            }
            for (String candidate : packages) {
                if (packageName.equals(candidate) || packageName.startsWith(candidate + ".")) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class RequestHandlerModel {
        private final Class<?> beanClass;
        private final Class<?> requestType;
        private FieldDescriptor field;

        private RequestHandlerModel(Class<?> beanClass, Class<?> requestType) {
            this.beanClass = beanClass;
            this.requestType = requestType;
        }

        private Class<?> beanClass() {
            return beanClass;
        }

        private Class<?> requestType() {
            return requestType;
        }

        private void setField(FieldDescriptor field) {
            this.field = field;
        }

        private FieldDescriptor field() {
            return field;
        }
    }

    private static final class NotificationHandlerModel {
        private final Class<?> beanClass;
        private final Class<?> notificationType;
        private FieldDescriptor field;

        private NotificationHandlerModel(Class<?> beanClass, Class<?> notificationType) {
            this.beanClass = beanClass;
            this.notificationType = notificationType;
        }

        private Class<?> beanClass() {
            return beanClass;
        }

        private Class<?> notificationType() {
            return notificationType;
        }

        private void setField(FieldDescriptor field) {
            this.field = field;
        }

        private FieldDescriptor field() {
            return field;
        }
    }

    private static final class RequestMiddlewareModel {
        private final Class<?> beanClass;
        private final Class<?> requestType;
        private final int order;
        private FieldDescriptor field;

        private RequestMiddlewareModel(Class<?> beanClass, Class<?> requestType, int order) {
            this.beanClass = beanClass;
            this.requestType = requestType;
            this.order = order;
        }

        private Class<?> beanClass() {
            return beanClass;
        }

        private Class<?> requestType() {
            return requestType;
        }

        private int order() {
            return order;
        }

        private void setField(FieldDescriptor field) {
            this.field = field;
        }

        private FieldDescriptor field() {
            return field;
        }
    }
}
