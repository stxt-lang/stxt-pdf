package dev.stxt.pdf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public API that the jar promises is the table of "What is promised" in {@code reference/implementation/api.md}
 * (the Spanish page is the original; the English one has to say the same). A type promised whole promises every public
 * member; a type promised in part, only the members the row names. This test reads the table and checks that every
 * promised type and member exists and is public, and that the promised signatures (parameters, return types, record
 * components, public fields, supertypes and nested public types) name no type of the engine that is not promised and no
 * foreign type other than those of {@code stxt-core} and the PDFBox document and page. A new public method on a
 * promised type that returns an internal type fails here: the boundary does not move without touching the page.
 */
class PublicApiTest {

    private static final Path SPANISH = Path.of("docs", "es", "reference", "implementation", "api.md");
    private static final Path ENGLISH = Path.of("docs", "en", "reference", "implementation", "api.md");
    private static final String SPANISH_HEADING = "## Lo que se promete";
    private static final String ENGLISH_HEADING = "## What is promised";
    private static final String PREFIX = "dev.stxt.pdf.";
    /** Foreign types a promised signature may name, by prefix: {@code stxt-core}, and the PDFBox document and page. */
    private static final List<String> FOREIGN = List.of("dev.stxt.", "org.apache.pdfbox.pdmodel.PDDocument",
            "org.apache.pdfbox.pdmodel.PDPage");
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");

    /**
     * A row of the table: a type and the members it promises, by simple name (a constructor is named like its class),
     * or {@code null} when it promises all of them.
     */
    private record Promise(String type, Set<String> members) {

        boolean whole() {
            return members == null;
        }
    }

    /** The promises of a page, by the name of the type as the table writes it ({@code fragment.TextLine.Run}). */
    private static Map<String, Promise> promises(Path page, String heading) throws IOException {
        Map<String, Promise> promises = new LinkedHashMap<>();
        boolean inSection = false;
        for (String line : Files.readAllLines(page)) {
            if (line.startsWith("## ")) {
                inSection = line.strip().equals(heading);
            } else if (inSection && line.startsWith("| `")) {
                String[] cells = line.substring(1, line.lastIndexOf('|')).split("\\|");
                assertEquals(2, cells.length, line);
                Set<String> members = null;
                String what = cells[1].strip();
                if (!what.equalsIgnoreCase("todo") && !what.equalsIgnoreCase("all")) {
                    members = new TreeSet<>();
                    Matcher m = CODE.matcher(what);
                    while (m.find()) {
                        String member = m.group(1);
                        int paren = member.indexOf('(');
                        members.add(paren < 0 ? member : member.substring(0, paren));
                    }
                }
                Matcher m = CODE.matcher(cells[0]);
                while (m.find()) {
                    promises.put(m.group(1), new Promise(m.group(1), members));
                }
            }
        }
        assertTrue(promises.containsKey("convert.Converter"), "The table of " + heading + " was not found in " + page);
        return promises;
    }

    /** The class of a type as the table names it: the packages in lowercase, then the class and its nested classes. */
    private static Class<?> load(String name) throws ClassNotFoundException {
        StringBuilder binary = new StringBuilder(PREFIX);
        boolean inClass = false;
        for (String part : name.split("\\.")) {
            if (!inClass && Character.isLowerCase(part.charAt(0))) {
                binary.append(part).append('.');
            } else {
                binary.append(inClass ? "$" : "").append(part);
                inClass = true;
            }
        }
        return Class.forName(binary.toString());
    }

    /** The name of a class as the table writes it. */
    private static String tableName(Class<?> c) {
        return c.getName().substring(PREFIX.length()).replace('$', '.');
    }

    private static boolean isPublic(Class<?> c) {
        return Modifier.isPublic(c.getModifiers())
                && (c.getEnclosingClass() == null || isPublic(c.getEnclosingClass()));
    }

    @Test
    void everyPromisedTypeExistsAndIsPublic() throws Exception {
        for (Promise promise : promises(SPANISH, SPANISH_HEADING).values()) {
            Class<?> c = load(promise.type());
            assertTrue(isPublic(c), promise.type() + " is promised and is not public");
        }
    }

    /** Every member a row names is a public method, field or constructor of its type, so the table has no typos. */
    @Test
    void everyPromisedMemberExistsAndIsPublic() throws Exception {
        for (Promise promise : promises(SPANISH, SPANISH_HEADING).values()) {
            if (promise.whole()) {
                continue;
            }
            Class<?> c = load(promise.type());
            Set<String> found = new TreeSet<>();
            for (Method m : c.getMethods()) {
                found.add(m.getName());
            }
            for (Field f : c.getFields()) {
                found.add(f.getName());
            }
            for (Constructor<?> k : c.getConstructors()) {
                found.add(c.getSimpleName());
            }
            Set<String> missing = new TreeSet<>(promise.members());
            missing.removeAll(found);
            assertTrue(missing.isEmpty(), promise.type() + " promises " + missing + ", which it does not have");
        }
    }

    /**
     * No promised signature exposes a type of the engine that is not promised, nor a foreign type other than those of
     * {@link #FOREIGN}: what the caller can reach from the contract is the contract.
     */
    @Test
    void promisedSignaturesExposeOnlyPromisedTypes() throws Exception {
        Map<String, Promise> promises = promises(SPANISH, SPANISH_HEADING);
        List<String> leaks = new ArrayList<>();
        for (Promise promise : promises.values()) {
            Class<?> c = load(promise.type());
            for (Map.Entry<String, Type> ref : references(c, promise).entrySet()) {
                Set<Class<?>> classes = new LinkedHashSet<>();
                collect(ref.getValue(), classes);
                for (Class<?> r : classes) {
                    String name = r.getName();
                    if (name.startsWith(PREFIX)) {
                        if (!promises.containsKey(tableName(r))) {
                            leaks.add(ref.getKey() + " exposes " + tableName(r) + ", which is not promised");
                        }
                    } else if (!name.startsWith("java.") && !r.isPrimitive()
                            && FOREIGN.stream().noneMatch(name::startsWith)) {
                        leaks.add(ref.getKey() + " exposes " + name + ", of another library");
                    }
                }
            }
        }
        assertEquals(List.of(), leaks);
    }

    /** The English page promises the same types with the same members as the Spanish original. */
    @Test
    void theEnglishPageSaysTheSame() throws IOException {
        Map<String, Promise> spanish = promises(SPANISH, SPANISH_HEADING);
        Map<String, Promise> english = promises(ENGLISH, ENGLISH_HEADING);
        assertEquals(spanish.keySet(), english.keySet());
        spanish.forEach((type, promise) -> assertEquals(promise.members(), english.get(type).members(), type));
    }

    /**
     * The types a promise exposes, each with where it appears: the supertypes always, and the public methods, fields,
     * constructors and nested types that are promised (all of them in a type promised whole; in the others, those the
     * row names, and the constants of an enum).
     */
    private static Map<String, Type> references(Class<?> c, Promise promise) {
        Map<String, Type> refs = new LinkedHashMap<>();
        String where = promise.type();
        if (c.getGenericSuperclass() != null) {
            refs.put(where + " extends", c.getGenericSuperclass());
        }
        for (Type t : c.getGenericInterfaces()) {
            refs.put(where + " implements " + t.getTypeName(), t);
        }
        for (Method m : c.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic() && promised(promise, m.getName())) {
                String member = where + "." + m.getName() + "(...)";
                refs.put(member + " returns", m.getGenericReturnType());
                for (Type t : m.getGenericParameterTypes()) {
                    refs.put(member + " takes " + t.getTypeName(), t);
                }
            }
        }
        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && !f.isSynthetic()
                    && (f.isEnumConstant() || promised(promise, f.getName()))) {
                refs.put(where + "." + f.getName(), f.getGenericType());
            }
        }
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            if (Modifier.isPublic(k.getModifiers()) && promised(promise, c.getSimpleName())) {
                for (Type t : k.getGenericParameterTypes()) {
                    refs.put(where + "(...) takes " + t.getTypeName(), t);
                }
            }
        }
        if (promise.whole()) {
            for (Class<?> inner : c.getDeclaredClasses()) {
                if (Modifier.isPublic(inner.getModifiers())) {
                    refs.put(where + " contains " + inner.getSimpleName(), inner);
                }
            }
        }
        return refs;
    }

    private static boolean promised(Promise promise, String member) {
        return promise.whole() || promise.members().contains(member);
    }

    /** The classes a type names, through its type arguments, bounds and array components. */
    private static void collect(Type t, Set<Class<?>> out) {
        if (t instanceof Class<?> c) {
            while (c.isArray()) {
                c = c.getComponentType();
            }
            out.add(c);
        } else if (t instanceof ParameterizedType p) {
            collect(p.getRawType(), out);
            for (Type arg : p.getActualTypeArguments()) {
                collect(arg, out);
            }
        } else if (t instanceof GenericArrayType g) {
            collect(g.getGenericComponentType(), out);
        } else if (t instanceof WildcardType w) {
            for (Type b : w.getUpperBounds()) {
                collect(b, out);
            }
            for (Type b : w.getLowerBounds()) {
                collect(b, out);
            }
        } else if (t instanceof TypeVariable<?> v) {
            for (Type b : v.getBounds()) {
                collect(b, out);
            }
        }
    }
}
