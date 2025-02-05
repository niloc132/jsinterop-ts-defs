/*
 * Copyright © 2023 Vertispan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vertispan.tsdefs.builders;

import static com.vertispan.tsdefs.Formatting.capitalizeFirstLetter;
import static com.vertispan.tsdefs.model.TsModifier.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.vertispan.tsdefs.Formatting;
import com.vertispan.tsdefs.HasProcessorEnv;
import com.vertispan.tsdefs.annotations.*;
import com.vertispan.tsdefs.model.TsDoc;
import com.vertispan.tsdefs.model.TsModifier;
import com.vertispan.tsdefs.model.TsType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import jsinterop.annotations.*;

public class TsElement {

  protected final Element element;
  protected final HasProcessorEnv env;
  private final JavaToTsTypeConverter typeConverter;

  public TsElement(Element element, HasProcessorEnv env) {
    this.element = element;
    this.env = env;
    this.typeConverter = new JavaToTsTypeConverter(element, env);
  }

  public TsElement(TypeMirror typeMirror, HasProcessorEnv env) {
    this.element = env.types().asElement(typeMirror);
    this.env = env;
    this.typeConverter = new JavaToTsTypeConverter(element, env);
  }

  public static TsElement of(Element element, HasProcessorEnv env) {
    return new TsElement(element, env);
  }

  public static TsElement of(TypeMirror typeMirror, HasProcessorEnv env) {
    return new TsElement(env.types().asElement(typeMirror), env);
  }

  public Element element() {
    return element;
  }

  public String elementName() {
    return element.getSimpleName().toString();
  }

  public TsDoc getDocs() {
    String docComment = env.elements().getDocComment(element);
    return Optional.ofNullable(docComment).map(TsDoc::of).orElse(TsDoc.empty());
  }

  public List<? extends TypeMirror> getInterfaces() {
    return ((TypeElement) element).getInterfaces();
  }

  public boolean isGetter() {
    return isMethod()
        && isJsProperty()
        && (elementName().startsWith("get") || elementName().startsWith("is"));
  }

  public boolean isSetter() {
    return isMethod() && isJsProperty() && (elementName().startsWith("set"));
  }

  public String getName() {
    return getDeclaredJsName().orElse(elementName());
  }

  /**
   * The name specified in TsName annotation takes highest priority Then we check each one of the
   * JsInterop annotations to find the element name
   *
   * @return {@link Optional} String name.
   */
  public Optional<String> getDeclaredJsName() {
    TsName tsNameAnnotation = element.getAnnotation(TsName.class);
    JsType jsTypeAnnotation = element.getAnnotation(JsType.class);
    JsProperty jsPropertyAnnotation = element.getAnnotation(JsProperty.class);
    JsMethod jsMethodAnnotation = element.getAnnotation(JsMethod.class);
    if (nonNull(tsNameAnnotation)
        && nonNull(tsNameAnnotation.name())
        && !tsNameAnnotation.name().trim().isEmpty()
        && !"<auto>".equals(tsNameAnnotation.name())) {
      return Optional.of(tsNameAnnotation.name());
    }
    if (nonNull(jsTypeAnnotation)
        && nonNull(jsTypeAnnotation.name())
        && !jsTypeAnnotation.name().trim().isEmpty()
        && !"<auto>".equals(jsTypeAnnotation.name())) {
      return Optional.of(jsTypeAnnotation.name());
    }
    if (nonNull(jsPropertyAnnotation)
        && nonNull(jsPropertyAnnotation.name())
        && !jsPropertyAnnotation.name().trim().isEmpty()
        && !"<auto>".equals(jsPropertyAnnotation.name())) {
      return Optional.of(jsPropertyAnnotation.name());
    }
    if (nonNull(jsMethodAnnotation)
        && nonNull(jsMethodAnnotation.name())
        && !jsMethodAnnotation.name().trim().isEmpty()
        && !"<auto>".equals(jsMethodAnnotation.name())) {
      return Optional.of(jsMethodAnnotation.name());
    }
    return Optional.empty();
  }

  /**
   * Return the name and make sure it does not include the getter/setter prefixes.
   *
   * @return String name
   */
  public String nonGetSetName() {
    return getDeclaredJsName().orElse(Formatting.nonGetSetName(elementName()));
  }

  /**
   * The namespace specified in TsName annotation takes highest priority Then we check each one of
   * the JsInterop annotations to find the element namespace if the namespace not found onm the
   * element we check the parent element namespace.
   *
   * @return String namespace.
   */
  public String getNamespace() {
    return getDeclaredNamespace().orElse(parentNamespace());
  }

  public Optional<String> getDeclaredNamespace() {
    TsName tsNameAnnotation = element.getAnnotation(TsName.class);
    JsType jsTypeAnnotation = element.getAnnotation(JsType.class);
    JsProperty jsPropertyAnnotation = element.getAnnotation(JsProperty.class);
    JsMethod jsMethodAnnotation = element.getAnnotation(JsMethod.class);
    if (nonNull(tsNameAnnotation)
        && nonNull(tsNameAnnotation.namespace())
        && !"<auto>".equals(tsNameAnnotation.namespace())) {
      return Optional.of(
          tsNameAnnotation.namespace().equals(JsPackage.GLOBAL)
              ? ""
              : tsNameAnnotation.namespace());
    }

    if (nonNull(jsTypeAnnotation)
        && nonNull(jsTypeAnnotation.namespace())
        && !"<auto>".equals(jsTypeAnnotation.namespace())) {
      return Optional.of(
          jsTypeAnnotation.namespace().equals(JsPackage.GLOBAL)
              ? ""
              : jsTypeAnnotation.namespace());
    }
    if (nonNull(jsPropertyAnnotation)
        && nonNull(jsPropertyAnnotation.namespace())
        && !"<auto>".equals(jsPropertyAnnotation.namespace())) {
      return Optional.of(
          jsPropertyAnnotation.namespace().equals(JsPackage.GLOBAL)
              ? ""
              : jsPropertyAnnotation.namespace());
    }
    if (nonNull(jsMethodAnnotation)
        && nonNull(jsMethodAnnotation.namespace())
        && !"<auto>".equals(jsMethodAnnotation.namespace())) {
      return Optional.of(
          jsMethodAnnotation.namespace().equals(JsPackage.GLOBAL)
              ? ""
              : jsMethodAnnotation.namespace());
    }

    return Optional.empty();
  }

  private String parentNamespace() {
    if (ElementKind.PACKAGE.equals(element.getKind())) {
      return getDeclaredNamespace()
          .orElse(env.elements().getPackageOf(element).getQualifiedName().toString());
    }
    if (nonNull(parent().element)) {
      return parent().getNamespace();
    }
    return env.elements().getPackageOf(element).getQualifiedName().toString();
  }

  public TsElement parent() {
    return TsElement.of(element.getEnclosingElement(), env);
  }

  public TsType getType() {
    if (isTsTypeRef()) {
      Optional<TypeMirror> typeDefClass = getClassValueFromAnnotation(TsTypeRef.class, "value");
      if (typeDefClass.isPresent()) {
        Optional<TsType> tsType =
            typeConverter.fromTsTypeDef(env.types().asElement(typeDefClass.get()));
        if (tsType.isPresent()) {
          return tsType.get();
        } else {
          return typeConverter.toTsType(typeDefClass.get());
        }
      } else {
        env.messager().printMessage(Diagnostic.Kind.ERROR, "Referenced type not found", element);
        return TsType.of("unknown");
      }
    } else {
      if (element instanceof ExecutableElement) {
        return typeConverter.toTsType(((ExecutableElement) element).getReturnType());
      } else {
        return typeConverter.toTsType(element.asType());
      }
    }
  }

  public List<ExecutableElement> nonInheritedMethods() {
    List<ExecutableElement> superMethods = superMethods();
    return ElementFilter.methodsIn(element.getEnclosedElements()).stream()
        .filter(method -> isNull(method.getAnnotation(Override.class)))
        .filter(method -> !overridesOneOf(method, superMethods))
        .collect(Collectors.toList());
  }

  public boolean isInheritedMethod(ExecutableElement method) {
    List<ExecutableElement> superMethods = superMethods();
    return overridesOneOf(method, superMethods);
  }

  private boolean overridesOneOf(ExecutableElement method, List<ExecutableElement> superMethods) {
    return superMethods.stream()
        .anyMatch(
            superMethod -> env.elements().overrides(method, superMethod, (TypeElement) element));
  }

  public List<ExecutableElement> superMethods() {
    List<ExecutableElement> methods = new ArrayList<>();

    superElement()
        .ifPresent(
            superElement -> {
              if (!superElement.superClass().getKind().equals(TypeKind.NONE)) {
                methods.addAll(ElementFilter.methodsIn(superElement.element.getEnclosedElements()));
                methods.addAll(superElement.superMethods());
              }
            });

    return methods;
  }

  public List<ExecutableElement> allMethodsAndSuperClassesMethods() {
    List<ExecutableElement> methods =
        new ArrayList<>(ElementFilter.methodsIn(element.getEnclosedElements()));
    superElement()
        .ifPresent(tsElement -> methods.addAll(tsElement.allMethodsAndSuperClassesMethods()));

    return methods;
  }

  public List<ExecutableElement> allSuperInterfacesMethods() {
    return allSuperInterfacesMethods(typeMirror -> true);
  }

  public List<ExecutableElement> allSuperInterfacesMethods(Predicate<TypeMirror> predicate) {
    List<ExecutableElement> methods = new ArrayList<>();
    ((TypeElement) element)
        .getInterfaces().stream()
            .filter(predicate::test)
            .map(typeMirror -> (TypeElement) env.types().asElement(typeMirror))
            .forEach(typeElement -> methods.addAll(allMethodsInInterface(typeElement)));

    return methods;
  }

  private List<ExecutableElement> allMethodsInInterface(TypeElement interfaceElement) {
    List<ExecutableElement> methods = new ArrayList<>();
    methods.addAll(ElementFilter.methodsIn(interfaceElement.getEnclosedElements()));

    interfaceElement.getInterfaces().stream()
        .map(typeMirror -> (TypeElement) env.types().asElement(typeMirror))
        .forEach(typeElement -> methods.addAll(allMethodsInInterface(typeElement)));

    return methods;
  }

  public boolean override(ExecutableElement method) {
    return allMethodsAndSuperClassesMethods().stream()
        .anyMatch(
            elementMethod ->
                env.elements().overrides(elementMethod, method, (TypeElement) element));
  }

  public Optional<TsElement> superElement() {
    TypeMirror superclass = ((TypeElement) element).getSuperclass();
    if (nonNull(superclass) && !superclass.getKind().equals(TypeKind.NONE)) {
      return Optional.of(TsElement.of(superclass, env));
    }
    return Optional.empty();
  }

  public TypeMirror superClass() {
    return ((TypeElement) element).getSuperclass();
  }

  public boolean isExportable() {
    return (parent().isTsInterface() && (isJsType() || isJsMember() || hasJsMembers()))
        || (!parent().isTsInterface()
            && (isJsType()
                || isJsMember()
                || hasJsMembers()
                || (isPublic() && (parent().isJsType()))));
  }

  public boolean isJsMember() {
    return isJsProperty() || isJsMethod() || isJsConstructor();
  }

  public Boolean isReadOnly() {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  public Optional<TsModifier> readonlyModifier() {
    return isReadOnly() ? Optional.of(READONLY) : Optional.empty();
  }

  public Boolean isStatic() {
    return element.getModifiers().contains(Modifier.STATIC);
  }

  public Boolean isAbstract() {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private Optional<TsModifier> staticModifier() {
    return isStatic() ? Optional.of(STATIC) : Optional.empty();
  }

  private Optional<TsModifier> abstractModifier() {
    return (isAbstract() && isMethod() && !parent().isInterface())
        ? Optional.of(ABSTRACT)
        : Optional.empty();
  }

  public Boolean isPrivate() {
    return element.getModifiers().contains(Modifier.PRIVATE);
  }

  private Optional<TsModifier> privateModifier() {
    return isPrivate() ? Optional.of(PRIVATE) : Optional.empty();
  }

  public Boolean isProtected() {
    return element.getModifiers().contains(Modifier.PROTECTED);
  }

  public Boolean isPublic() {
    return element.getModifiers().contains(Modifier.PUBLIC);
  }

  public Boolean isFinal() {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  private Optional<TsModifier> protectedModifier() {
    return isProtected() ? Optional.of(PROTECTED) : Optional.empty();
  }

  public Boolean isOptional() {
    return nonNull(element.getAnnotation(JsOptional.class));
  }

  public boolean isJsType() {
    return nonNull(element.getAnnotation(JsType.class));
  }

  public boolean hasJsMembers() {
    return element.getEnclosedElements().stream().anyMatch(e -> TsElement.of(e, env).isJsMember());
  }

  public boolean isIgnored() {
    return nonNull(element.getAnnotation(JsIgnore.class));
  }

  public boolean isTsIgnored() {
    return nonNull(element) && nonNull(element.getAnnotation(TsIgnore.class));
  }

  public boolean isOverlay() {
    return nonNull(element.getAnnotation(JsOverlay.class));
  }

  public boolean isJsProperty() {
    return nonNull(element.getAnnotation(JsProperty.class));
  }

  public boolean isJsOptional() {
    return nonNull(element.getAnnotation(JsOptional.class));
  }

  public boolean isTsTypeDef() {
    return nonNull(element.getAnnotation(TsTypeDef.class));
  }

  public boolean isTsTypeRef() {
    return nonNull(element) && nonNull(element.getAnnotation(TsTypeRef.class));
  }

  public boolean isTsInterface() {
    return nonNull(element) && nonNull(element.getAnnotation(TsInterface.class));
  }

  public Optional<TypeMirror> getJavaSuperClass() {
    TypeMirror superclass = ((TypeElement) element).getSuperclass();

    if (TypeKind.NONE.equals(superclass.getKind())
        || ((TypeElement) env.types().asElement(superclass))
            .getSuperclass()
            .getKind()
            .equals(TypeKind.NONE)) {
      return Optional.empty();
    }
    return Optional.of(superclass);
  }

  public boolean isInterface() {
    return element.getKind().isInterface() || isTsInterface();
  }

  public boolean isClass() {
    return element.getKind().isClass() && !isTsInterface();
  }

  public boolean isMethod() {
    return ElementKind.METHOD.equals(element.getKind());
  }

  public boolean isJsMethod() {
    return nonNull(element.getAnnotation(JsMethod.class));
  }

  public boolean isJsConstructor() {
    return nonNull(element.getAnnotation(JsConstructor.class));
  }

  public boolean isField() {
    return element.getKind().isField();
  }

  public boolean isJsFunction() {
    return nonNull(element) && nonNull(element.getAnnotation(JsFunction.class));
  }

  public boolean isConstructor() {
    return nonNull(element) && ElementKind.CONSTRUCTOR.equals(element.getKind());
  }

  public boolean isPrimitiveBoolean() {
    TypeMirror type = element.asType();
    return type.getKind().isPrimitive() && "boolean".equals(type.toString());
  }

  public boolean isDeprecated() {
    return nonNull(element.getAnnotation(Deprecated.class));
  }

  public String getGetterName() {
    return (isPrimitiveBoolean() ? "is" : "get")
        + capitalizeFirstLetter(element.getSimpleName().toString());
  }

  public String getSetterName() {
    return "set" + capitalizeFirstLetter(element.getSimpleName().toString());
  }

  public TsModifier[] getJsModifiers() {
    return Stream.of(protectedModifier(), abstractModifier(), staticModifier(), readonlyModifier())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toArray(TsModifier[]::new);
  }

  /**
   * Finds the type mirror of a class defined as an annotation parameter.
   *
   * <p>For example:
   *
   * <pre>
   * interface &#64;MyAnnotation {
   *  Class&#60;?&#62; myClass();
   * }
   * </pre>
   *
   * <p>
   *
   * @param annotation the annotation
   * @param paramName the class parameter name
   * @return The type mirror of the class, {@link Optional#empty()} otherwise
   */
  public Optional<TypeMirror> getClassValueFromAnnotation(
      Class<? extends Annotation> annotation, String paramName) {
    for (AnnotationMirror am : element.getAnnotationMirrors()) {
      if (env.types()
          .isSameType(
              am.getAnnotationType(),
              env.elements().getTypeElement(annotation.getCanonicalName()).asType())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
            am.getElementValues().entrySet()) {
          if (paramName.equals(entry.getKey().getSimpleName().toString())) {
            AnnotationValue annotationValue = entry.getValue();
            return Optional.of((DeclaredType) annotationValue.getValue());
          }
        }
      }
    }
    return Optional.empty();
  }

  public boolean requiresPrivateConstructor() {
    if (isJsType()) {
      List<TsElement> constructors =
          element.getEnclosedElements().stream()
              .map(enclosedElement -> TsElement.of(enclosedElement, env))
              .filter(TsElement::isConstructor)
              .filter(
                  tsElement -> !((ExecutableElement) tsElement.element()).getParameters().isEmpty())
              .collect(Collectors.toList());
      boolean allIgnored =
          !constructors.isEmpty() && constructors.stream().allMatch(TsElement::isIgnored);

      if (allIgnored) {
        return true;
      }

    } else {
      Optional<TsElement> jsConstructor =
          element.getEnclosedElements().stream()
              .map(enclosedElement -> TsElement.of(enclosedElement, env))
              .filter(TsElement::isConstructor)
              .filter(
                  tsElement ->
                      !((ExecutableElement) tsElement.element()).getParameters().isEmpty()
                          || tsElement.isJsConstructor())
              .filter(TsElement::isJsConstructor)
              .findAny();
      if (!jsConstructor.isPresent()) {
        return true;
      }
    }
    return false;
  }
}
