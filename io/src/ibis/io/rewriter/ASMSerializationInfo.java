/* $Id: SerializationInfo.java 13105 2011-03-15 14:48:18Z ceriel $ */

package ibis.io.rewriter;

import ibis.compile.ASMRepository;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * SerializationInfo contains static methods for determining information about a particular
 * class as well as an interface to determine the proper method names for serialization of
 * a particular type.
  */
class ASMSerializationInfo implements ASMRewriterConstants, Opcodes {

    private static class FieldComparator implements Comparator<FieldNode> {
        public int compare(FieldNode f1, FieldNode f2) {
            return f1.name.compareTo(f2.name);
        }
    }

    static FieldComparator fieldComparator = new FieldComparator();

    static HashMap<String, Long> serialversionids = new HashMap<String, Long>();

    static HashMap<Type, ASMSerializationInfo> primitiveSerialization = new HashMap<Type, ASMSerializationInfo> ();

    static ASMSerializationInfo referenceSerialization = new ASMSerializationInfo(METHOD_WRITE_OBJECT,
            METHOD_READ_OBJECT, METHOD_READ_FIELD_OBJECT, "Ljava/lang/Object;", false);

    static {
        primitiveSerialization.put(Type.BOOLEAN_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_BOOLEAN, METHOD_READ_BOOLEAN, METHOD_READ_FIELD_BOOLEAN,
                TYPE_BOOLEAN, true));

        primitiveSerialization.put(Type.BYTE_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_BYTE, METHOD_READ_BYTE, METHOD_READ_FIELD_BYTE, TYPE_BYTE, true));

        primitiveSerialization.put(Type.SHORT_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_SHORT, METHOD_READ_SHORT, METHOD_READ_FIELD_SHORT, TYPE_SHORT, true));

        primitiveSerialization.put(Type.CHAR_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_CHAR, METHOD_READ_CHAR, METHOD_READ_FIELD_CHAR, TYPE_CHAR, true));

        primitiveSerialization.put(Type.INT_TYPE, new ASMSerializationInfo(METHOD_WRITE_INT,
                METHOD_READ_INT, METHOD_READ_FIELD_INT, TYPE_INT, true));

        primitiveSerialization.put(Type.LONG_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_LONG, METHOD_READ_LONG, METHOD_READ_FIELD_LONG, TYPE_LONG, true));

        primitiveSerialization.put(Type.FLOAT_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_FLOAT, METHOD_READ_FLOAT, METHOD_READ_FIELD_FLOAT, TYPE_FLOAT, true));

        primitiveSerialization.put(Type.DOUBLE_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_DOUBLE, METHOD_READ_DOUBLE, METHOD_READ_FIELD_DOUBLE, TYPE_DOUBLE,
                true));
        primitiveSerialization.put(STRING_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_STRING, METHOD_READ_STRING, METHOD_READ_FIELD_STRING, STRING_TYPE.getDescriptor(),
                true));
        primitiveSerialization.put(CLASS_TYPE, new ASMSerializationInfo(
                METHOD_WRITE_CLASS, METHOD_READ_CLASS, METHOD_READ_FIELD_CLASS,
                CLASS_TYPE.getDescriptor(), true));
    }

    String write_name;

    String read_name;

    String final_read_name;

    String signature;

    boolean primitive;

    ASMSerializationInfo(String wn, String rn, String frn, String signature,
            boolean primitive) {
        this.write_name = wn;
        this.read_name = rn;
        this.final_read_name = frn;
        this.signature = signature;
        this.primitive = primitive;
    }

    static ASMSerializationInfo getSerializationInfo(Type tp) {
        ASMSerializationInfo temp = primitiveSerialization.get(tp);
        return (temp == null ? referenceSerialization : temp);
    }

    static boolean directImplementationOf(ClassNode clazz,
            String name) {
        @SuppressWarnings("unchecked")
        List<String> names = clazz.interfaces;
        String supername = clazz.superName;

        if (supername != null && supername.equals(name)) {
            return true;
        }

        if (names == null) {
            return false;
        }
        for (String n : names) {
            if (n.equals(name)) {
                return true;
            }
        }
        return false;
    }

    static boolean predecessor(String c1, ClassNode c2) {
        String n = c2.superName;

        // System.out.println("comparing " + c1 + ", " + n);
        if (n.equals(c1)) {
            return true;
        }
        if (n.equals(TYPE_JAVA_LANG_OBJECT)) {
            return false;
        }
        return predecessor(c1, ASMCodeGenerator.lookupClass(n));
    }

    static boolean isFinal(Type t) {
        if (t.getSort() == Type.ARRAY) {
            return isFinal(t.getElementType());
        } else if (t.getSort() == Type.OBJECT) {
            ClassNode cl = ASMCodeGenerator.lookupClass(t.getInternalName());
            return (cl.access & ACC_FINAL) == ACC_FINAL;
        } else {
            return true;
        }
    }

    static boolean isIbisSerializable(ClassNode clazz) {
        return directImplementationOf(clazz, TYPE_IBIS_IO_SERIALIZABLE);
    }
 
    static boolean isExternalizable(ClassNode clazz) {
        try {
            return ASMRepository.implementationOf(clazz, TYPE_JAVA_IO_EXTERNALIZABLE);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    static boolean isSerializable(ClassNode clazz) {
        try {
            return ASMRepository.implementationOf(clazz, TYPE_JAVA_IO_SERIALIZABLE);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    static boolean hasSerialPersistentFields(List<FieldNode> fields) {
        for (FieldNode f : fields) {
            if (f.name.equals(FIELD_SERIAL_PERSISTENT_FIELDS)
                    && ((f.access & (ACC_FINAL | ACC_STATIC | ACC_PRIVATE)) == (ACC_FINAL | ACC_STATIC | ACC_PRIVATE))
                     && f.desc.equals(
                            TYPE_LJAVA_IO_OBJECT_STREAM_FIELD)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFinalFields(List<FieldNode> fields) {
        for (FieldNode f : fields) {
            if ((f.access & ACC_FINAL) == ACC_FINAL) {
                return true;
            }
        }
        return false;
    }

    static boolean hasIbisConstructor(ClassNode cl) {
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = cl.methods;
        MethodNode[] clMethods = methods.toArray(new MethodNode[methods.size()]);

        for (int i = 0; i < clMethods.length; i++) {
            if (clMethods[i].name.equals(METHOD_INIT)
                    && clMethods[i].desc.equals(
                            SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes the serial version UID value for the given class.
     */
    static long computeSUID(ClassNode clazz) {
        if (!isSerializable(clazz)) {
            return 0L;
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            // 1. The class name written using UTF encoding.
            dout.writeUTF(clazz.name);

            // 2. The class modifiers written as a 32-bit integer.
            int classModifiers = clazz.access
            & (ACC_PUBLIC | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT);

            // Only set ABSTRACT for an interface when it has methods.
            @SuppressWarnings("unchecked")
            List<MethodNode> methods = clazz.methods;
            MethodNode[] cMethods = methods.toArray(new MethodNode[methods.size()]);
            if ((classModifiers & ACC_INTERFACE) != 0) {
                if (cMethods.length > 0) {
                    classModifiers |= ACC_ABSTRACT;
                } else {
                    classModifiers &= ~ACC_ABSTRACT;
                }
            }
            dout.writeInt(classModifiers);

            // 3. The name of each interface sorted by name written using
            //    UTF encoding.
            @SuppressWarnings("unchecked")
            List<String> l = clazz.interfaces;
            String[] interfaceNames = l.toArray(new String[l.size()]);
            Arrays.sort(interfaceNames);
            for (int i = 0; i < interfaceNames.length; i++) {
                dout.writeUTF(interfaceNames[i]);
            }

            // 4. For each field of the class sorted by field name (except
            //    private static and private transient fields).
            @SuppressWarnings("unchecked")
            List<FieldNode> fields = clazz.fields;
            FieldNode[] cFields = fields.toArray(new FieldNode[fields.size()]);
            Arrays.sort(cFields, fieldComparator);
            for (int i = 0; i < cFields.length; i++) {
                int mods = cFields[i].access;
                if (((mods & ACC_PRIVATE) == 0)
                        || ((mods & (ACC_STATIC | ACC_TRANSIENT)) == 0)) {
                    // 4.1. The name of the field in UTF encoding.
                    dout.writeUTF(cFields[i].name);
                    // 4.2. The modifiers of the field written as a
                    //      32-bit integer.
                    dout.writeInt(mods);
                    // 4.3. The descriptor of the field in UTF encoding
                    dout.writeUTF(cFields[i].desc);
                }
            }

            // This is where the trouble starts for serialver.

            // 5. If a class initializer exists, write out the following:
            for (int i = 0; i < cMethods.length; i++) {
                if (cMethods[i].name.equals(METHOD_CLINIT)) {
                    // 5.1. The name of the method, <clinit>, in UTF
                    //      encoding.
                    dout.writeUTF(METHOD_CLINIT);
                    // 5.2. The modifier of the method,
                    //      java.lang.reflect.Modifier.STATIC, written as
                    //      a 32-bit integer.
                    dout.writeInt(ACC_STATIC);
                    // 5.3. The descriptor of the method, ()V, in UTF
                    //      encoding.
                    dout.writeUTF("()V");
                    break;
                }
            }

            Arrays.sort(cMethods, new Comparator<MethodNode>() {
                public int compare(MethodNode o1, MethodNode o2) {
                    String name1 = o1.name;
                    String name2 = o2.name;
                    if (name1.equals(name2)) {
                        String sig1 = o1.desc;
                        String sig2 = o2.desc;
                        return sig1.compareTo(sig2);
                    }
                    return name1.compareTo(name2);
                }
            });

            // 6. For each non-private constructor sorted by method name
            //    and signature:
            for (int i = 0; i < cMethods.length; i++) {
                if (cMethods[i].name.equals(METHOD_INIT)) {
                    int mods = cMethods[i].access;
                    if ((mods & ACC_PRIVATE) == 0) {
                        // 6.1. The name of the method, <init>, in UTF
                        //      encoding.
                        dout.writeUTF(METHOD_INIT);
                        // 6.2. The modifiers of the method written as a
                        //      32-bit integer.
                        dout.writeInt(mods);
                        // 6.3. The descriptor of the method in UTF
                        //      encoding.
                        dout.writeUTF(cMethods[i].desc.replace(
                                '/', '.'));
                    }
                }
            }

            // 7. For each non-private method sorted by method name and
            //    signature:
            for (int i = 0; i < cMethods.length; i++) {
                if (!cMethods[i].name.equals(METHOD_INIT)
                        && !cMethods[i].name.equals(METHOD_CLINIT)) {
                    int mods = cMethods[i].access;
                    if ((mods & ACC_PRIVATE) == 0) {
                        // 7.1. The name of the method in UTF encoding.
                        dout.writeUTF(cMethods[i].name);
                        // 7.2. The modifiers of the method written as a
                        //      32-bit integer.
                        dout.writeInt(mods);
                        // 7.3. The descriptor of the method in UTF
                        //      encoding.
                        dout.writeUTF(cMethods[i].desc.replace(
                                '/', '.'));
                    }
                }
            }

            dout.flush();

            // 8. The SHA-1 algorithm is executed on the stream of bytes
            //    produced by DataOutputStream and produces five 32-bit
            //    values sha[0..4].
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());

            long hash = 0;
            // Use the first 8 bytes.
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (Exception ex) {
            System.err.println("Warning: could not get serialVersionUID "
                    + "for class " + clazz.name);
            ex.printStackTrace(System.err);
            return 0L;
        }
    }

    static long getSerialVersionUID(String classname, ClassNode clazz) {
        long uid;
        Long ui = serialversionids.get(classname);
        if (ui == null) {
            uid = ASMSerializationInfo.computeSUID(clazz);
            serialversionids.put(classname, new Long(uid));
        } else {
            uid = ui.longValue();
        }
        return uid;
    }

    static MethodNode findMethod(List<MethodNode> methods, String name, String signature) {
        for (MethodNode m : methods) {
            if (m.name.equals(name)
                    && m.desc.equals(signature)) {
                return m;
            }
        }
        return null;
    }

    static boolean hasWriteObject(List<MethodNode> methods) {
        return findMethod(methods, METHOD_WRITE_OBJECT, SIGNATURE_LJAVA_IO_OBJECT_OUTPUT_STREAM_V) != null;
    }

    static boolean hasReadObject(List<MethodNode> methods) {
        return findMethod(methods, METHOD_READ_OBJECT, SIGNATURE_LJAVA_IO_OBJECT_INPUT_STREAM_V) != null;
    }
}
