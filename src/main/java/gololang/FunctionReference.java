/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package gololang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static java.lang.invoke.MethodHandles.filterReturnValue;

/**
 * A reference to a function / closure.
 *
 * This class essentially boxes {@code MethodHandle} references, and provides as many delegations as possible.
 * Previous versions of Golo used direct {@code MethodHandle} objects to deal with functions by reference, but that
 * class does not provide any mean to attach local state, as required for, say, implementing named arguments.
 *
 * This boxed representation provides a sound abstraction while not hurting performance, as
 * {@code fr.insalyon.citi.golo.runtime.ClosureCallSupport} still dispatches through a method handle.
 *
 * @see java.lang.invoke.MethodHandle
 * @see fr.insalyon.citi.golo.runtime.ClosureCallSupport
 */
public class FunctionReference {

  private final MethodHandle handle;

  private final String[] parameterNames;

  /**
   * Makes a function reference from a method handle.
   *
   * @param handle the method handle.
   * @param parameterNames the target method parameter's names.
   * @throws IllegalArgumentException if {@code handle} is {@code null}.
   */
  public FunctionReference(MethodHandle handle, String[] parameterNames) {
    if (handle == null) {
      throw new IllegalArgumentException("A method handle cannot be null");
    }
    this.handle = handle;
    this.parameterNames = parameterNames;
  }

  /**
   * Makes a function reference from a method handle.
   * The parameter names will be {@code null}.
   *
   * @param handle the method handle.
   * @throws IllegalArgumentException if {@code handle} is {@code null}.
   */
  public FunctionReference(MethodHandle handle) {
    this(handle, null);
  }

  /**
   * Unboxes the method handle.
   *
   * @return the (boxed) method handle.
   */
  public MethodHandle handle() {
    return handle;
  }

  /**
   * Get the target function parameter's names
   *
   * @return the array of parameter's names
   */
  public String[] parameterNames() {
    return parameterNames;
  }

  public MethodType type() {
    return handle.type();
  }

  public FunctionReference asCollector(Class<?> arrayType, int arrayLength) {
    return new FunctionReference(handle.asCollector(arrayType, arrayLength), this.parameterNames);
  }

  public FunctionReference asFixedArity() {
    return new FunctionReference(handle.asFixedArity(), this.parameterNames);
  }

  public FunctionReference asType(MethodType newType) {
    return new FunctionReference(handle.asType(newType), this.parameterNames);
  }

  public FunctionReference asVarargsCollector(Class<?> arrayType) {
    return new FunctionReference(handle.asVarargsCollector(arrayType), this.parameterNames);
  }

  public FunctionReference bindTo(Object x) {
    return new FunctionReference(handle.bindTo(x), dropParameterNames(0, 1));
  }

  public boolean isVarargsCollector() {
    return handle.isVarargsCollector();
  }

  public FunctionReference asSpreader(Class<?> arrayType, int arrayLength) {
    return new FunctionReference(handle.asSpreader(arrayType, arrayLength));
  }

  public Object invoke(Object... args) throws Throwable {
    return handle.invokeWithArguments(args);
  }

  @Override
  public String toString() {
    return "FunctionReference{" +
        "handle=" + handle +
        ", parameterNames=" + Arrays.toString(parameterNames) +
        '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FunctionReference that = (FunctionReference) obj;
    return handle.equals(that.handle);
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }

  /**
   * Converts a function reference to an instance of an interface.
   *
   * @param interfaceClass the interface,
   * @return a proxy object that satisfies {@code interfaceClass} and delegates to {@code this}.
   */
  public Object to(Class<?> interfaceClass) {
    return Predefined.asInterfaceInstance(interfaceClass, this);
  }

  /**
   * Compose a function with another function.
   *
   * @param fun the function that processes the results of {@code this} function.
   * @return a composed function.
   */
  public FunctionReference andThen(FunctionReference fun) {
    if (fun.type().parameterCount() != 1) {
      throw new IllegalArgumentException("andThen requires a function with exactly 1 parameter");
    }
    return new FunctionReference(filterReturnValue(this.handle, fun.handle), this.parameterNames);
  }

  /**
   * Partial application.
   *
   * @param position the argument position (0-indexed).
   * @param value the argument value.
   * @return a partially applied function.
   */
  public FunctionReference bindAt(int position, Object value) {
    return new FunctionReference(MethodHandles.insertArguments(this.handle, position, value), dropParameterNames(position, 1));
  }

  /**
   * Partial application based on parameter's names.
   *
   * @param parameterName the parameter to bind.
   * @param value the argument value.
   * @return a partially applied function.
   */
  public FunctionReference bindAt(String parameterName, Object value) {
    int position = -1;
    if (this.parameterNames == null) {
      throw new RuntimeException("Can't bind on parameter name, " + this.toString() + " has none");
    }
    for (int i = 0; i < this.parameterNames.length; i++) {
      if (this.parameterNames[i].equals(parameterName)) {
        position = i;
        break;
      }
    }
    if (position == -1) {
      throw new IllegalArgumentException("'" + parameterName + "' not in the parameter list " + Arrays.toString(parameterNames));
    }
    return bindAt(position, value);
  }

  /**
   * Partial application.
   *
   * @param position the first argument position.
   * @param values the values of the arguments from {@code position}.
   * @return a partially applied function.
   * @see java.lang.invoke.MethodHandles#insertArguments(MethodHandle, int, Object...)
   */
  public FunctionReference insertArguments(int position, Object... values) {
    return new FunctionReference(MethodHandles.insertArguments(handle, position, values), dropParameterNames(position, values.length));
  }

  /**
   * Spread arguments over this function parameters.
   *
   * @param arguments arguments as an array.
   * @return a return value.
   * @throws Throwable ...because an exception can be thrown.
   */
  public Object spread(Object... arguments) throws Throwable {
    int arity = this.handle.type().parameterCount();
    if (this.handle.isVarargsCollector() && (arity > 0) && (arguments[arity - 1] instanceof Object[])) {
      return this.handle
          .asFixedArity()
          .asSpreader(Object[].class, arguments.length)
          .invoke(arguments);
    }
    return this.handle
        .asSpreader(Object[].class, arguments.length)
        .invoke(arguments);
  }

  private String[] dropParameterNames(int from, int size) {
    if (this.parameterNames == null) {
      return null;
    }
    String[] filtered = new String[this.parameterNames.length - size];
    if(filtered.length > 0) {
      System.arraycopy(parameterNames, 0, filtered, 0, from);
      System.arraycopy(parameterNames, from + size, filtered, from, this.parameterNames.length - size - from);
    }
    return filtered;
  }
}
