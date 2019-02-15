/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicecomb.foundation.protobuf.internal.bean;

import com.fasterxml.jackson.databind.JavaType;

public class PropertyDescriptor {
  private String name;

  private JavaType javaType;

  private Object getter;

  private Object setter;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JavaType getJavaType() {
    return javaType;
  }

  public void setJavaType(JavaType javaType) {
    this.javaType = javaType;
  }

  @SuppressWarnings("unchecked")
  public <T> T getGetter() {
    return (T) getter;
  }

  public void setGetter(Object getter) {
    this.getter = getter;
  }

  @SuppressWarnings("unchecked")
  public <T> T getSetter() {
    return (T) setter;
  }

  public void setSetter(Object setter) {
    this.setter = setter;
  }
}
