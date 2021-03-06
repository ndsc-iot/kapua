/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kapua.service.datastore.internal.mediator;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.kapua.commons.util.KapuaDateUtils;
import org.eclipse.kapua.model.id.KapuaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

/**
 * Datastore utility class
 *
 * @since 1.0.0
 */
public class DatastoreUtils {

    private static final Logger logger = LoggerFactory.getLogger(DatastoreUtils.class);

    private DatastoreUtils() {
    }

    private static final char SPECIAL_DOT = '.';
    private static final String SPECIAL_DOT_ESC = "$2e";

    private static final char SPECIAL_DOLLAR = '$';
    private static final String SPECIAL_DOLLAR_ESC = "$24";

    public static final CharSequence ILLEGAL_CHARS = "\"\\/*?<>|,. ";

    public static final String CLIENT_METRIC_TYPE_STRING = "string";
    public static final String CLIENT_METRIC_TYPE_INTEGER = "integer";
    public static final String CLIENT_METRIC_TYPE_LONG = "long";
    public static final String CLIENT_METRIC_TYPE_FLOAT = "float";
    public static final String CLIENT_METRIC_TYPE_DOUBLE = "double";
    public static final String CLIENT_METRIC_TYPE_DATE = "date";
    public static final String CLIENT_METRIC_TYPE_BOOLEAN = "boolean";
    public static final String CLIENT_METRIC_TYPE_BINARY = "binary";

    public static final String CLIENT_METRIC_TYPE_STRING_ACRONYM = "str";
    public static final String CLIENT_METRIC_TYPE_INTEGER_ACRONYM = "int";
    public static final String CLIENT_METRIC_TYPE_LONG_ACRONYM = "lng";
    public static final String CLIENT_METRIC_TYPE_FLOAT_ACRONYM = "flt";
    public static final String CLIENT_METRIC_TYPE_DOUBLE_ACRONYM = "dbl";
    public static final String CLIENT_METRIC_TYPE_DATE_ACRONYM = "dte";
    public static final String CLIENT_METRIC_TYPE_BOOLEAN_ACRONYM = "bln";
    public static final String CLIENT_METRIC_TYPE_BINARY_ACRONYM = "bin";

    private static final DateTimeFormatter DATA_INDEX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-ww", Locale.US);
    
    /**
     * Return the hash code for the provided components (typically components are a sequence of account - client id - channel ...)
     * 
     * @param components
     * @return
     */
    public static String getHashCode(String... components) {
        String concatString = "";
        for (String str : components) {
            concatString = concatString.concat(str);
        }

        byte[] hashCode = Hashing.sha256()
                .hashString(concatString, StandardCharsets.UTF_8)
                .asBytes();

        // ES 5.2 FIX
        // return Base64.encodeBytes(hashCode);
        return Base64.getEncoder().encodeToString(hashCode);
    }

    private static String normalizeIndexName(String name) {
        String normName = null;
        try {
            DatastoreUtils.checkIdxAliasName(name);
            normName = name;
        } catch (IllegalArgumentException exc) {
            logger.trace(exc.getMessage(), exc);
            normName = name.toLowerCase().replace(ILLEGAL_CHARS, "_");
            DatastoreUtils.checkIdxAliasName(normName);
        }
        return normName;
    }

    /**
     * Normalize the metric name to be compliant to Kapua/Elasticserach constraints.<br>
     * It escapes the '$' and '.'
     *
     * @param name
     * @return
     * @since 1.0.0
     */
    public static String normalizeMetricName(String name) {
        String newName = name;
        if (newName.contains(".")) {
            newName = newName.replace(String.valueOf(SPECIAL_DOLLAR), SPECIAL_DOLLAR_ESC);
            newName = newName.replace(String.valueOf(SPECIAL_DOT), SPECIAL_DOT_ESC);
            logger.trace(String.format("Metric %s contains a special char '%s' that will be replaced with '%s'", name, String.valueOf(SPECIAL_DOT), SPECIAL_DOT_ESC));
        }
        return newName;
    }

    /**
     * Restore the metric name, so switch back to the 'not escaped' values for '$' and '.'
     *
     * @param normalizedName
     * @return
     * @since 1.0.0
     */
    public static String restoreMetricName(String normalizedName) {
        String oldName = normalizedName;
        oldName = oldName.replace(SPECIAL_DOT_ESC, String.valueOf(SPECIAL_DOT));
        oldName = oldName.replace(SPECIAL_DOLLAR_ESC, String.valueOf(SPECIAL_DOLLAR));
        return oldName;
    }

    /**
     * Return the metric parts for the composed metric name (split the metric name by '.')
     *
     * @param fullName
     * @return
     */
    public static String[] getMetricParts(String fullName) {
        return fullName == null ? null : fullName.split(Pattern.quote("."));
    }

    /**
     * Check the index alias correctness.<br>
     * The alias cnnot be null, starts with '_', contains uppercase character or contains {@link DatastoreUtils#ILLEGAL_CHARS}
     *
     * @param alias
     * @since 1.0.0
     */
    public static void checkIdxAliasName(String alias) {
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException(String.format("Alias name cannot be %s", alias == null ? "null" : "empty"));
        }
        if (alias.startsWith("_")) {
            throw new IllegalArgumentException(String.format("Alias name cannot start with _"));
        }
        for (int i = 0; i < alias.length(); i++) {
            if (Character.isUpperCase(alias.charAt(i))) {
                throw new IllegalArgumentException(String.format("Alias name cannot contain uppercase chars [found %s]", alias.charAt(i)));
            }
        }
        if (alias.contains(ILLEGAL_CHARS)) {
            throw new IllegalArgumentException(String.format("Alias name cannot contain special chars [found oneof %s]", ILLEGAL_CHARS));
        }
    }

    /**
     * Check the index name ({@link DatastoreUtils#checkIdxAliasName(String index)}
     *
     * @param index
     * @since 1.0.0
     */
    public static void checkIdxName(String index) {
        DatastoreUtils.checkIdxAliasName(index);
    }

    /**
     * Normalize the index alias name and replace the '-' with '_'
     *
     * @param alias
     * @return
     * @since 1.0.0
     */
    public static String normalizeIndexAliasName(String alias) {
        String aliasName = normalizeIndexName(alias);
        aliasName = aliasName.replace("-", "_");
        return aliasName;
    }

    /**
     * Normalize the account index name and and the suffix '-*'
     *
     * @param scopeId
     * @return
     */
    public static String getDataIndexName(KapuaId scopeId) {
        String indexName = DatastoreUtils.normalizedIndexName(scopeId.toStringId());
        indexName = String.format("%s-*", indexName);
        return indexName;
    }

    /**
     * Get the data index for the specified base name and timestamp
     *
     * @param scopeId
     * @param timestamp
     * @return
     */
    public static String getDataIndexName(KapuaId scopeId, long timestamp) {
        final String actualName = DatastoreUtils.normalizedIndexName(scopeId.toStringId());
        final StringBuilder sb = new StringBuilder(actualName).append('-');
        DATA_INDEX_FORMATTER.formatTo(Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC), sb);
        return sb.toString();
    }

    /**
     * Get the Kapua index name for the specified base name
     *
     * @param scopeId
     * @return
     * @since 1.0.0
     */
    public static String getRegistryIndexName(KapuaId scopeId) {
        String actualName = DatastoreUtils.normalizedIndexName(scopeId.toStringId());
        actualName = String.format(".%s", actualName);
        return actualName;
    }

    /**
     * Normalize the index ({@link DatastoreUtils#normalizeIndexName(String index)}
     *
     * @param index
     * @return
     */
    public static String normalizedIndexName(String index) {
        return normalizeIndexName(index);
    }

    /**
     * Get the full metric name used to store the metric in Elasticsearch.<br>
     * The full metric name is composed by the metric and the type acronym as suffix ('.' is used as separator between the 2 parts)
     *
     * @param name
     * @param type
     * @return
     */
    public static String getMetricValueQualifier(String name, String type) {
        String shortType = DatastoreUtils.getClientMetricFromAcronym(type);
        return String.format("%s.%s", name, shortType);
    }

    /**
     * Get the client metric type from the metric value type
     *
     * @param clazz
     * @return
     * @since 1.0.0
     */
    public static String getClientMetricFromType(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("Metric value must not be null");
        }
        String value;
        if (clazz == String.class) {
            value = CLIENT_METRIC_TYPE_STRING;
        } else if (clazz == Integer.class) {
            value = CLIENT_METRIC_TYPE_INTEGER;
        } else if (clazz == Long.class) {
            value = CLIENT_METRIC_TYPE_LONG;
        } else if (clazz == Float.class) {
            value = CLIENT_METRIC_TYPE_FLOAT;
        } else if (clazz == Double.class) {
            value = CLIENT_METRIC_TYPE_DOUBLE;
        } else if (clazz == Boolean.class) {
            value = CLIENT_METRIC_TYPE_BOOLEAN;
        } else if (clazz == Date.class) {
            value = CLIENT_METRIC_TYPE_DATE;
        } else if (clazz == byte[].class) {
            value = CLIENT_METRIC_TYPE_BINARY;
        } else {
            throw new IllegalArgumentException(String.format("Metric value type for "));
        }
        return value;
    }

    /**
     * Get the client metric type acronym for the given client metric type full name
     *
     * @param acronym
     * @return
     * @since 1.0.0
     */
    public static String getClientMetricFromAcronym(String acronym) {
        if (CLIENT_METRIC_TYPE_STRING.equals(acronym)) {
            return CLIENT_METRIC_TYPE_STRING_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_INTEGER.equals(acronym)) {
            return CLIENT_METRIC_TYPE_INTEGER_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_LONG.equals(acronym)) {
            return CLIENT_METRIC_TYPE_LONG_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_FLOAT.equals(acronym)) {
            return CLIENT_METRIC_TYPE_FLOAT_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_DOUBLE.equals(acronym)) {
            return CLIENT_METRIC_TYPE_DOUBLE_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_BOOLEAN.equals(acronym)) {
            return CLIENT_METRIC_TYPE_BOOLEAN_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_DATE.equals(acronym)) {
            return CLIENT_METRIC_TYPE_DATE_ACRONYM;
        }
        if (CLIENT_METRIC_TYPE_BINARY.equals(acronym)) {
            return CLIENT_METRIC_TYPE_BINARY_ACRONYM;
        }
        throw new IllegalArgumentException(String.format("Unknown type [%s]", acronym));
    }

    /**
     * Check if the metric type is date
     *
     * @param acronym
     * @return
     */
    public static boolean isDateMetric(String acronym) {
        return CLIENT_METRIC_TYPE_DATE_ACRONYM.equals(acronym);
    }

    /**
     * Convert the metric value class type (Kapua side) to the proper string type description (client side)
     *
     * @param aClass
     * @return
     * @since 1.0.0
     */
    public static <T> String convertToClientMetricType(Class<T> aClass) {
        if (aClass == String.class) {
            return CLIENT_METRIC_TYPE_STRING;
        }
        if (aClass == Integer.class) {
            return CLIENT_METRIC_TYPE_INTEGER;
        }
        if (aClass == Long.class) {
            return CLIENT_METRIC_TYPE_LONG;
        }
        if (aClass == Float.class) {
            return CLIENT_METRIC_TYPE_FLOAT;
        }
        if (aClass == Double.class) {
            return CLIENT_METRIC_TYPE_DOUBLE;
        }
        if (aClass == Boolean.class) {
            return CLIENT_METRIC_TYPE_BOOLEAN;
        }
        if (aClass == Date.class) {
            return CLIENT_METRIC_TYPE_DATE;
        }
        if (aClass == byte[].class) {
            return CLIENT_METRIC_TYPE_BINARY;
        }
        throw new IllegalArgumentException(String.format("Unknown type [%s]", aClass.getName()));
    }

    /**
     * Convert the client metric type to the corresponding Kapua type
     *
     * @param clientType
     * @return
     * @since 1.0.0
     */
    public static Class<?> convertToKapuaType(String clientType) {
        Class<?> clazz;
        if (CLIENT_METRIC_TYPE_STRING.equals(clientType)) {
            clazz = String.class;
        } else if (CLIENT_METRIC_TYPE_INTEGER.equals(clientType)) {
            clazz = Integer.class;
        } else if (CLIENT_METRIC_TYPE_LONG.equals(clientType)) {
            clazz = Long.class;
        } else if (CLIENT_METRIC_TYPE_FLOAT.equals(clientType)) {
            clazz = Float.class;
        } else if (CLIENT_METRIC_TYPE_DOUBLE.equals(clientType)) {
            clazz = Double.class;
        } else if (CLIENT_METRIC_TYPE_BOOLEAN.equals(clientType)) {
            clazz = Boolean.class;
        } else if (CLIENT_METRIC_TYPE_DATE.equals(clientType)) {
            clazz = Date.class;
        } else if (CLIENT_METRIC_TYPE_BINARY.equals(clientType)) {
            clazz = byte[].class;
        } else {
            throw new IllegalArgumentException(String.format("Unknown type [%s]", clientType));
        }
        return clazz;
    }

    /**
     * Convert the metric value to the correct type using the metric acronym type
     *
     * @param acronymType
     * @param value
     * @return
     * @since 1.0.0
     */
    public static Object convertToCorrectType(String acronymType, Object value) {
        Object convertedValue = null;
        if (CLIENT_METRIC_TYPE_DOUBLE_ACRONYM.equals(acronymType)) {
            if (value instanceof Number) {
                convertedValue = new Double(((Number) value).doubleValue());
            } else if (value instanceof String) {
                convertedValue = Double.parseDouble((String) value);
            } else {
                throw new IllegalArgumentException(String.format("Type [%s] cannot be converted to Double!", getValueClass(value)));
            }
        } else if (CLIENT_METRIC_TYPE_FLOAT_ACRONYM.equals(acronymType)) {
            if (value instanceof Number) {
                convertedValue = new Float(((Number) value).floatValue());
            } else if (value instanceof String) {
                convertedValue = Float.parseFloat((String) value);
            } else {
                throw new IllegalArgumentException(String.format("Type [%s] cannot be converted to Double!", getValueClass(value)));
            }
        } else if (CLIENT_METRIC_TYPE_INTEGER_ACRONYM.equals(acronymType)) {
            if (value instanceof Number) {
                convertedValue = new Integer(((Number) value).intValue());
            } else if (value instanceof String) {
                convertedValue = Integer.parseInt((String) value);
            } else {
                throw new IllegalArgumentException(String.format("Type [%s] cannot be converted to Double!", getValueClass(value)));
            }
        } else if (CLIENT_METRIC_TYPE_LONG_ACRONYM.equals(acronymType)) {
            if (value instanceof Number) {
                convertedValue = new Long(((Number) value).longValue());
            } else if (value instanceof String) {
                convertedValue = Long.parseLong((String) value);
            } else {
                throw new IllegalArgumentException(String.format("Type [%s] cannot be converted to Long!", getValueClass(value)));
            }
        } else if (CLIENT_METRIC_TYPE_DATE_ACRONYM.equals(acronymType)) {
            if (value instanceof Date) {
                convertedValue = (Date) value;
            } else if (value instanceof String) {
                try {
                    convertedValue = KapuaDateUtils.parseDate((String) value);
                } catch (ParseException e) {
                    throw new IllegalArgumentException(
                            String.format("Type [%s] cannot be converted to Date. Allowed format [%s] - Value to convert [%s]!", getValueClass(value), KapuaDateUtils.ISO_DATE_PATTERN,
                                    value));
                }
            } else {
                throw new IllegalArgumentException(String.format("Type [%s] cannot be converted to Date!", getValueClass(value)));
            }
        } else {
            // no need to translate for others field type
            convertedValue = value;
        }
        return convertedValue;
    }

    private static String getValueClass(Object value) {
        return value != null ? value.getClass().toString() : "null";
    }

}
