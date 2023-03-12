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
package com.vertispan.tsdefs.jsfunctions;

import com.vertispan.tsdefs.annotations.TsInterface;
import jsinterop.annotations.JsType;

@JsType
public class JsFunctionsClient {

  public void useVoidFunction(TakesVoidReturnsVoidJsFunction takesVoidReturnsVoidJsFunction) {}

  public void useFunctionWithArgs(TakesArgsReturnsVoidJsFunction takesArgsReturnsVoidJsFunction) {}

  public void useFunctionWithReturnType(
      TakesArgsReturnsTypeJsFunction takesArgsReturnsTypeJsFunction) {}

  public TakesArgsReturnsTypeJsFunction useFunctionAndReturnFunction(
      TakesArgsReturnsTypeJsFunction takesArgsReturnsTypeJsFunction) {
    return (id, name) -> true;
  };

  public void useGenericFuncWithParam(GenericArgsFunction<String> func) {}

  public <T> void useGenericFuncWithTypeParam(GenericArgsFunction<T> func) {}

  public void useWildcardGenericFunc(GenericArgsFunction<?> func) {}
}

