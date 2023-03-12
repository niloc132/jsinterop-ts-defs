package com.vertispan.tsdefs.jsfunctions;

import elemental2.core.JsArray;
import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface GenericArgsFunction<T> {
    void go(JsArray<T> value);
}
