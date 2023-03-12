package com.vertispan.tsdefs.jsfunctions;

import com.vertispan.tsdefs.annotations.TsInterface;
import jsinterop.annotations.JsType;

@TsInterface
@JsType
public class JsFunctionsInterface {
    public void useVoidFunction(TakesVoidReturnsVoidJsFunction takesVoidReturnsVoidJsFunction) {
    }

    public void useFunctionWithArgs(TakesArgsReturnsVoidJsFunction takesArgsReturnsVoidJsFunction) {
    }

    public void useFunctionWithReturnType(
            TakesArgsReturnsTypeJsFunction takesArgsReturnsTypeJsFunction) {
    }

    public TakesArgsReturnsTypeJsFunction useFunctionAndReturnFunction(
            TakesArgsReturnsTypeJsFunction takesArgsReturnsTypeJsFunction) {
        return (id, name) -> true;
    }

    ;

    public void useGenericFuncWithParam(GenericArgsFunction<String> func) {
    }

    public <T> void useGenericFuncWithTypeParam(GenericArgsFunction<T> func) {
    }

    public void useWildcardGenericFunc(GenericArgsFunction<?> func) {
    }
}
