/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sagebionetworks.research.domain;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Adapts values whose runtime type may differ from their declaration type. This is necessary when a field's type is
 * not the same type that GSON should create when deserializing that field. For example, consider these types:
 * <pre>   {@code
 *   abstract class Shape {
 *     int x;
 *     int y;
 *   }
 *   class Circle extends Shape {
 *     int radius;
 *   }
 *   class Rectangle extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Diamond extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Drawing {
 *     Shape bottomShape;
 *     Shape topShape;
 *   }
 * }</pre>
 * <p>Without additional type information, the serialized JSON is ambiguous. Is
 * the bottom shape in this drawing a rectangle or a diamond? <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * This class addresses this problem by adding type information to the serialized JSON and honoring that type
 * information when the JSON is
 * deserialized: <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "type": "Diamond",
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "type": "Circle",
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * Both the type field name ({@code "type"}) and the type labels ({@code "Rectangle"}) are configurable.
 * <p>
 * <h3>Registering Types</h3> Create a {@code RuntimeTypeAdapterFactory} by passing the base type and type field name
 * to the {@link #of} factory method. If you don't supply an explicit type
 * field name, {@code "type"} will be used. <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 *       = RuntimeTypeAdapterFactory.of(Shape.class, "type");
 * }</pre>
 * Next register all of your subtypes. Every subtype must be explicitly registered. This protects your application
 * from injection attacks. If you don't supply an explicit type label, the type's simple name will be used.
 * <pre>   {@code
 *   shapeAdapter.registerSubtype(Rectangle.class, "Rectangle");
 *   shapeAdapter.registerSubtype(Circle.class, "Circle");
 *   shapeAdapter.registerSubtype(Diamond.class, "Diamond");
 * }</pre>
 * Finally, register the type adapter factory in your application's GSON builder:
 * <pre>   {@code
 *   Gson gson = new GsonBuilder()
 *       .registerTypeAdapterFactory(shapeAdapterFactory)
 *       .create();
 * }</pre>
 * Like {@code GsonBuilder}, this API supports chaining: <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 *       .registerSubtype(Rectangle.class)
 *       .registerSubtype(Circle.class)
 *       .registerSubtype(Diamond.class);
 * }</pre>
 */
@SuppressWarnings({})
public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private final Type baseType;

    private Type defaultType = null;

    private final Map<String, Type> labelToSubtype = new LinkedHashMap<>();

    private final Map<Type, Set<String>> subtypeToLabel = new LinkedHashMap<>();

    private final String typeFieldName;

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code typeFieldName} as the type field
     * name. Type field names are case sensitive.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Type baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<T>(baseType, typeFieldName);
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code "type"} as the type field name.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Type baseType) {
        return new RuntimeTypeAdapterFactory<T>(baseType, "type");
    }

    private RuntimeTypeAdapterFactory(Type baseType, String typeFieldName) {
        if (typeFieldName == null || baseType == null) {
            throw new NullPointerException();
        }
        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, com.google.gson.reflect.TypeToken<R> type) {
        if (type.getRawType() != baseType) {
            return null;
        }

        final Map<String, TypeAdapter<?>> labelToDelegate
                = new LinkedHashMap<>();
        final Map<Type, TypeAdapter<?>> subtypeToDelegate
                = new LinkedHashMap<>();
        for (Entry<String, Type> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this,
                    com.google.gson.reflect.TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }
        @SuppressWarnings("unchecked") final TypeAdapter<R> defaultDelegate = defaultType == null ? null
                : (TypeAdapter<R>) gson.getDelegateAdapter(this,
                        com.google.gson.reflect.TypeToken.get(defaultType));

        return new TypeAdapter<R>() {
            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                // Because we have allowed multiple labels to map to the same type, if multiple labels are
                // registered getting the label for a given type is ambiguous. As a result in these cases it
                // is up to the class to store it's own type field.
                String label = null;
                if (subtypeToLabel.containsKey(srcType)) {
                    Set<String> labels = subtypeToLabel.get(srcType);
                    if (labels.size() == 1) {
                        label = labels.iterator().next();
                    }
                }

                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                        TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    delegate = defaultDelegate;
                }
                if (delegate == null) {
                    throw new JsonParseException("cannot serialize " + srcType.getName()
                            + "; did you forget to register a subtype?");
                }
                JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();
                JsonObject clone = new JsonObject();
                for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                    clone.add(e.getKey(), e.getValue());
                }

                if (label != null) {
                    // If we found a label earlier we add it to the clone.
                    clone.remove(typeFieldName);
                    clone.addProperty(typeFieldName, label);
                }

                Streams.write(clone, out);
            }

            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement jsonElement = Streams.parse(in);
                JsonElement labelJsonElement = jsonElement.getAsJsonObject().get(typeFieldName);
                if (labelJsonElement == null) {
                    throw new JsonParseException("cannot deserialize " + baseType
                            + " because it does not define a field named " + typeFieldName);
                }
                String label = labelJsonElement.getAsString();
                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                        TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
                if (delegate == null) {
                    delegate = defaultDelegate;
                }
                if (delegate == null) {
                    throw new JsonParseException("cannot deserialize " + baseType + " subtype named "
                            + label + "; did you forget to register a subtype?");
                }
                R result = delegate.fromJsonTree(jsonElement);
                if (result != null) {
                    return result;
                }

                return (R)new Object();
            }
        }.nullSafe();
    }

    /**
     * Registers {@code type} as the default type for this factory.
     *
     * @param type
     *         The new default type for this factory.
     * @return This factory.
     * @throws IllegalArgumentException
     *         if {@code type} is not a subtype of the base type.
     */
    public RuntimeTypeAdapterFactory<T> registerDefaultType(Type type) {
        if (type == null) {
            throw new NullPointerException();
        }
        if (!TypeToken.of(type).isSubtypeOf(baseType)) {
            throw new IllegalArgumentException("Type " + type + " is not a subtype of base type " + baseType);
        }
        defaultType = type;
        return this;
    }

    /**
     * Registers {@code type} identified by {@code label}. Labels are case sensitive.
     *
     * @throws IllegalArgumentException
     *         if {@code type} is not a subtype of the base type.
     */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Type type, String label) {
        if (type == null || label == null) {
            throw new NullPointerException();
        }
        if (!TypeToken.of(type).isSubtypeOf(baseType)) {
            throw new IllegalArgumentException("Type " + type + " is not a subtype of base type " + baseType);
        }

        labelToSubtype.put(label, type);
        if (!subtypeToLabel.containsKey(type)) {
            subtypeToLabel.put(type, new HashSet<String>());
        }
        Set<String> currentLabels = subtypeToLabel.get(type);
        currentLabels.add(label);
        return this;
    }

    /**
     * Registers {@code type} identified by its {@link Class#getSimpleName simple name}. Labels are case sensitive.
     *
     * @throws IllegalArgumentException
     *         if {@code type} is not a subtype of the base type.
     */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Type type) {
        return registerSubtype(type, TypeToken.of(type).getRawType().getSimpleName());
    }
}