package com.progressoft.brix.domino.apt.client.processors.group;


import com.progressoft.brix.domino.api.client.annotations.Path;
import com.progressoft.brix.domino.api.client.annotations.Request;
import com.progressoft.brix.domino.api.client.annotations.RequestFactory;
import com.progressoft.brix.domino.api.client.request.Response;
import com.progressoft.brix.domino.api.client.request.ServerRequest;
import com.progressoft.brix.domino.api.shared.request.RequestBean;
import com.progressoft.brix.domino.api.shared.request.VoidRequest;
import com.progressoft.brix.domino.apt.commons.DominoTypeBuilder;
import com.progressoft.brix.domino.apt.commons.JavaSourceWriter;
import com.progressoft.brix.domino.apt.commons.ProcessorElement;
import com.squareup.javapoet.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class RequestFactorySourceWriter extends JavaSourceWriter {

    private final String requestsServiceRoot;

    public RequestFactorySourceWriter(ProcessorElement processorElement) {
        super(processorElement);
        requestsServiceRoot = processorElement.getAnnotation(RequestFactory.class).value();
    }

    @Override
    public String write() throws IOException {

        String factoryName = processorElement.simpleName() + "Factory";
        FieldSpec instanceField = FieldSpec.builder(ClassName.bestGuess(factoryName), "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new " + factoryName + "()")
                .build();

        List<TypeSpec> requests = processorElement.methodsStream().map(this::makeRequestClass).collect(toList());
        List<MethodSpec> overrideMethods = processorElement.methodsStream().map(this::makeOverrideMethod).collect(toList());

        TypeSpec factory = DominoTypeBuilder.build(factoryName, RequestFactoryProcessor.class)
                .addSuperinterface(ClassName.bestGuess(processorElement.simpleName()))
                .addField(instanceField)
                .addTypes(requests)
                .addMethods(overrideMethods).build();

        StringBuilder asString = new StringBuilder();
        JavaFile.builder(processorElement.elementPackage(), factory).skipJavaLangImports(true).build().writeTo(asString);
        return asString.toString();
    }

    private MethodSpec makeOverrideMethod(ExecutableElement method) {
        TypeName requestTypeName = getRequestClassName(method);
        DeclaredType responseReturnType = (DeclaredType) method.getReturnType();
        TypeMirror responseBean = responseReturnType.getTypeArguments().get(0);


        MethodSpec.Builder request = MethodSpec.methodBuilder(method.getSimpleName().toString())
                .returns(ParameterizedTypeName.get(ClassName.get(Response.class), ClassName.get(responseBean)))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class);

        String initializeStatement = "return new " + processorElement.simpleName() + "_" + method.getSimpleName();
        if (!method.getParameters().isEmpty()) {
            request.addParameter(requestTypeName, "request");
            request.addStatement(initializeStatement + "(request)");
        } else
            request.addStatement(initializeStatement + "($T.VOID_REQUEST)", RequestBean.class);


        return request.build();
    }

    private TypeSpec makeRequestClass(ExecutableElement method) {

        TypeName requestTypeName = getRequestClassName(method);
        DeclaredType responseReturnType = (DeclaredType) method.getReturnType();
        TypeMirror responseBean = responseReturnType.getTypeArguments().get(0);


        TypeSpec.Builder requestBuilder = TypeSpec.classBuilder(processorElement.simpleName() + "_" + method.getSimpleName())
                .addModifiers(Modifier.PUBLIC)
                .superclass(ParameterizedTypeName.get(ClassName.get(ServerRequest.class),
                        requestTypeName,
                        ClassName.get(responseBean)))
                .addMethod(constructor(requestTypeName));

        requestBuilder.addAnnotation(buildRequestAnnotation(method));
        requestBuilder.addAnnotation(pathAnnotation(method.getAnnotation(Path.class)));

        return requestBuilder.build();
    }

    private AnnotationSpec buildRequestAnnotation(ExecutableElement method) {
        if (needsClassifier(method))
            return requestAnnotation(method.getSimpleName().toString());

        return requestAnnotation();
    }

    private boolean needsClassifier(ExecutableElement method) {
        if (method.getParameters().isEmpty())
            return needsClassifier(method, m -> m.getParameters().isEmpty());

        return needsClassifier(method, m -> !m.getParameters().isEmpty() && isSameParameterType(method, m));
    }

    private boolean isSameParameterType(ExecutableElement method, ExecutableElement m) {
        return m.getParameters().get(0).asType().toString().equals(method.getParameters().get(0).asType().toString());
    }

    private boolean needsClassifier(ExecutableElement method, Function<ExecutableElement, Boolean> anyMatchFilter) {
        return processorElement.methodsStream()
                .filter(m -> !m.getSimpleName().equals(method.getSimpleName()))
                .anyMatch(anyMatchFilter::apply);
    }

    private TypeName getRequestClassName(ExecutableElement method) {
        if (method.getParameters().isEmpty())
            return ClassName.get(VoidRequest.class);

        return ClassName.get(method.getParameters().get(0).asType());
    }

    private MethodSpec constructor(TypeName requestBean) {
        return MethodSpec.constructorBuilder().addParameter(requestBean, "request")
                .addStatement("super(request)")
                .build();
    }

    private AnnotationSpec pathAnnotation(Path path) {
        AnnotationSpec pathAnnotation = AnnotationSpec.get(path);
        if (requestsServiceRoot.isEmpty())
            return pathAnnotation;

        return makeNewPath(path, pathAnnotation);
    }

    private AnnotationSpec makeNewPath(Path path, AnnotationSpec pathAnnotation) {
        AnnotationSpec.Builder newPath = AnnotationSpec.builder(Path.class);

        pathAnnotation.members.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("value"))
                .forEach(entry -> addValues(newPath, entry.getKey(), entry.getValue()));

        return newPath.addMember("value", "\"" + requestsServiceRoot + path.value() + "\"")
                .build();
    }

    private void addValues(AnnotationSpec.Builder newPath, String key, List<CodeBlock> blocks) {
        blocks.forEach(codeBlock -> newPath.addMember(key, codeBlock));
    }

    private AnnotationSpec requestAnnotation() {
        return requestAnnotation("");
    }

    private AnnotationSpec requestAnnotation(String classifier) {
        AnnotationSpec.Builder requestAnnotationBuilder = AnnotationSpec.builder(Request.class);

        if (!classifier.isEmpty())
            requestAnnotationBuilder.addMember("classifier", "\"" + classifier + "\"");
        return requestAnnotationBuilder.build();
    }
}