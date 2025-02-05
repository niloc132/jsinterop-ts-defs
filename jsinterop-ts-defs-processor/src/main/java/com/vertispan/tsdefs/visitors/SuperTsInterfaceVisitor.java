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
package com.vertispan.tsdefs.visitors;

import com.vertispan.tsdefs.HasProcessorEnv;
import com.vertispan.tsdefs.builders.HasInterfaces;
import com.vertispan.tsdefs.builders.IsClassBuilder;
import com.vertispan.tsdefs.builders.TsElement;
import com.vertispan.tsdefs.model.TsInterface;
import com.vertispan.tsdefs.model.TsMethod;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class SuperTsInterfaceVisitor<T> extends TsElement {
  private final TypeMirror type;

  public SuperTsInterfaceVisitor(TypeMirror typeMirror, HasProcessorEnv env) {
    super(typeMirror, env);
    this.type = typeMirror;
  }

  public void visit(IsClassBuilder<T> builder) {
    if (isTsInterface()) {
      addInterface(builder, type);
      element.getEnclosedElements().stream()
          .filter(e -> ElementKind.METHOD.equals(e.getKind()))
          .map(e -> TsElement.of(e, env))
          .filter(TsElement::isJsMethod)
          .map(e -> (ExecutableElement) e.element())
          .forEach(e -> addFunction(builder, e));

      ((TypeElement) element)
          .getInterfaces()
          .forEach(
              interfaceType -> {
                new InheritedMethodsVisitor<T>(env.types().asElement(interfaceType), env)
                    .visit(builder);
              });
    }
  }

  public void addFunction(IsClassBuilder<T> parent, ExecutableElement method) {
    TsElement tsMethodElement = TsElement.of(method, env);
    TsMethod.TsMethodBuilder builder =
        TsMethod.builder(tsMethodElement.getName(), tsMethodElement.getType())
            .addModifiers(tsMethodElement.getJsModifiers())
            .setDocs(tsMethodElement.getDocs())
            .setDeprecated(tsMethodElement.isDeprecated());

    method
        .getParameters()
        .forEach(
            param -> new ParameterVisitor<TsMethod.TsMethodBuilder>(param, env).visit(builder));
    parent.addFunction(builder.build());
  }

  private void addInterface(HasInterfaces<T> builder, TypeMirror typeMirror) {
    TsElement tsElement = TsElement.of(typeMirror, env);
    if (!tsElement.isTsIgnored()) {
      TsInterface.TsInterfaceBuilder interfaceBuilder =
          TsInterface.builder(tsElement.getName(), tsElement.getNamespace());
      new TypeArgumentsVisitor<TsInterface.TsInterfaceBuilder>(typeMirror, env)
          .visit(interfaceBuilder);
      builder.addInterface(interfaceBuilder.build());
    }
  }
}
