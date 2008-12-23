/*
 *  Copyright 2006 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dirmi.core;

import java.lang.reflect.InvocationTargetException;

import java.io.DataOutput;

import java.rmi.Remote;

import java.rmi.server.Unreferenced;

import java.util.Collection;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.TypeDesc;

import org.cojen.util.ClassInjector;

import dirmi.Pipe;
import dirmi.UnimplementedMethodException;

import dirmi.info.RemoteInfo;
import dirmi.info.RemoteMethod;
import dirmi.info.RemoteParameter;

import dirmi.core.Identifier;

/**
 * 
 *
 * @author Brian S O'Neill
 */
class CodeBuilderUtil {
    static final String FACTORY_FIELD = "factory";

    static final String METHOD_ID_FIELD_PREFIX = "method_";

    // Method name ends with '$' so as not to conflict with user method.
    static final String FACTORY_REF_METHOD_NAME = "factoryRef$";

    static final TypeDesc IDENTIFIER_TYPE;
    static final TypeDesc VERSIONED_IDENTIFIER_TYPE;
    static final TypeDesc STUB_SUPPORT_TYPE;
    static final TypeDesc SKEL_SUPPORT_TYPE;
    static final TypeDesc INV_CHANNEL_TYPE;
    static final TypeDesc INV_IN_TYPE;
    static final TypeDesc INV_OUT_TYPE;
    static final TypeDesc NO_SUCH_METHOD_EX_TYPE;
    static final TypeDesc UNIMPLEMENTED_EX_TYPE;
    static final TypeDesc ASYNC_INV_EX_TYPE;
    static final TypeDesc THROWABLE_TYPE;
    static final TypeDesc CLASS_TYPE;
    static final TypeDesc FUTURE_TYPE;
    static final TypeDesc TIME_UNIT_TYPE;
    static final TypeDesc PIPE_TYPE;
    static final TypeDesc DATA_OUTPUT_TYPE;
    static final TypeDesc UNREFERENCED_TYPE;

    static {
        IDENTIFIER_TYPE = TypeDesc.forClass(Identifier.class);
        VERSIONED_IDENTIFIER_TYPE = TypeDesc.forClass(VersionedIdentifier.class);
        STUB_SUPPORT_TYPE = TypeDesc.forClass(StubSupport.class);
        SKEL_SUPPORT_TYPE = TypeDesc.forClass(SkeletonSupport.class);
        INV_CHANNEL_TYPE = TypeDesc.forClass(InvocationChannel.class);
        INV_IN_TYPE = TypeDesc.forClass(InvocationInputStream.class);
        INV_OUT_TYPE = TypeDesc.forClass(InvocationOutputStream.class);
        NO_SUCH_METHOD_EX_TYPE = TypeDesc.forClass(NoSuchMethodException.class);
        UNIMPLEMENTED_EX_TYPE = TypeDesc.forClass(UnimplementedMethodException.class);
        ASYNC_INV_EX_TYPE = TypeDesc.forClass(AsynchronousInvocationException.class);
        THROWABLE_TYPE = TypeDesc.forClass(Throwable.class);
        CLASS_TYPE = TypeDesc.forClass(Class.class);
        FUTURE_TYPE = TypeDesc.forClass(Future.class);
        TIME_UNIT_TYPE = TypeDesc.forClass(TimeUnit.class);
        PIPE_TYPE = TypeDesc.forClass(Pipe.class);
        DATA_OUTPUT_TYPE = TypeDesc.forClass(DataOutput.class);
        UNREFERENCED_TYPE = TypeDesc.forClass(Unreferenced.class);
    }

    static boolean equalTypes(RemoteParameter a, RemoteParameter b) {
        return a == null ? b == null : (a.equalTypes(b));
    }

    static String cleanClassName(String name) {
        if (name.startsWith("java.")) {
            // Rename to avoid SecurityException.
            name = "java$" + name.substring(4);
        }
        return name;
    }

    /**
     * Generates code to read a parameter from an InvocationInput, cast it, and
     * leave it on the stack. Generated code may throw an IOException,
     * NoSuchObjectException, or ClassNotFoundException.
     *
     * @param param type of parameter to read
     * @param invInVar variable which references an InvocationInput instance
     */
    static void readParam(CodeBuilder b,
                          RemoteParameter param,
                          LocalVariable invInVar)
    {
        TypeDesc type = getTypeDesc(param);

        String methodName;
        TypeDesc methodType;
        TypeDesc castType;

        if (type.isPrimitive()) {
            methodName = type.getRootName();
            methodName = "read" +
                Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
            methodType = type;
            castType = null;
        } else if (param.isUnshared()) {
            if (TypeDesc.STRING == type) {
                methodName = "readUnsharedString";
                methodType = type;
                castType = null;
            } else {
                methodName = "readUnshared";
                methodType = TypeDesc.OBJECT;
                castType = type;
            }
        } else {
            methodName = "readObject";
            methodType = TypeDesc.OBJECT;
            castType = type;
        }

        b.loadLocal(invInVar);
        b.invokeVirtual(invInVar.getType(), methodName, methodType, null);

        if (castType != null && castType != TypeDesc.OBJECT) {
            b.checkCast(type);
        }
    }

    /**
     * Generates code to write a parameter to an InvocationOutput. Generated
     * code may throw an IOException.
     *
     * @param param type of parameter to write
     * @param invOutVar variable which references a InvocationOutput instance
     * @param paramVar variable which references parameter value
     */
    static void writeParam(CodeBuilder b,
                           RemoteParameter param,
                           LocalVariable invOutVar,
                           LocalVariable paramVar)
    {
        TypeDesc type = getTypeDesc(param);

        String methodName;
        TypeDesc methodType;

        if (type.isPrimitive()) {
            methodName = type.getRootName();
            methodName = "write" +
                Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
            methodType = type;
        } else if (param.isUnshared()) {
            if (TypeDesc.STRING == type) {
                methodName = "writeUnsharedString";
                methodType = type;
            } else {
                methodName = "writeUnshared";
                methodType = TypeDesc.OBJECT;
            }
        } else {
            methodName = "writeObject";
            methodType = TypeDesc.OBJECT;
        }

        b.loadLocal(invOutVar);
        b.loadLocal(paramVar);
        b.invokeVirtual(invOutVar.getType(), methodName, null, new TypeDesc[] {methodType});
    }

    static TypeDesc getTypeDesc(RemoteParameter param) {
        if (param == null) {
            return null;
        }
        return TypeDesc.forClass(param.getType());
    }

    static TypeDesc[] getTypeDescs(Collection<? extends RemoteParameter> params) {
        TypeDesc[] paramDescs = new TypeDesc[params.size()];
        int j = 0;
        for (RemoteParameter param : params) {
            paramDescs[j++] = getTypeDesc(param);
        }
        return paramDescs;
    }

    static void loadMethodID(CodeBuilder b, int methodOrdinal) {
        final TypeDesc identifierType = TypeDesc.forClass(Identifier.class);
        b.loadStaticField(METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
    }

    static void addInitMethodAndFields(ClassFile cf, RemoteInfo info) {
        cf.addField(Modifiers.PRIVATE.toStatic(true).toVolatile(true),
                    FACTORY_FIELD, TypeDesc.OBJECT);
        addMethodIDFields(cf, info);
        addStaticInitMethod(cf, info);
        addFactoryRefMethod(cf);
    }

    private static void addMethodIDFields(ClassFile cf, RemoteInfo info) {
        final TypeDesc identifierType = TypeDesc.forClass(Identifier.class);
        int count = info.getRemoteMethods().size();
        for (int methodOrdinal = 0; methodOrdinal < count; methodOrdinal++) {
            cf.addField(Modifiers.PRIVATE.toStatic(true).toFinal(true),
                        METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
        }
    }

    private static void addStaticInitMethod(ClassFile cf, RemoteInfo info) {
        final TypeDesc identifierType = TypeDesc.forClass(Identifier.class);
        final TypeDesc identifierArrayType = identifierType.toArrayType();

        CodeBuilder b = new CodeBuilder(cf.addInitializer());

        b.invokeStatic(TypeDesc.forClass(IdentifierSet.class), "get", identifierArrayType, null);
        LocalVariable idsVar = b.createLocalVariable(null, identifierArrayType);
        b.storeLocal(idsVar);

        int count = info.getRemoteMethods().size();
        for (int methodOrdinal = 0; methodOrdinal < count; methodOrdinal++) {
            b.loadLocal(idsVar);
            b.loadConstant(methodOrdinal);
            b.loadFromArray(identifierType);
            b.storeStaticField(METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
        }

        b.returnVoid();
    }

    private static void addFactoryRefMethod(ClassFile cf) {
        MethodInfo mi = cf.addMethod
            (Modifiers.PUBLIC.toStatic(true), FACTORY_REF_METHOD_NAME,
             null, new TypeDesc[] {TypeDesc.OBJECT});

        CodeBuilder b = new CodeBuilder(mi);

        b.loadLocal(b.getParameter(0));
        b.storeStaticField(FACTORY_FIELD, TypeDesc.OBJECT);

        b.returnVoid();
    }

    /**
     * @param factory Strong reference is kept to this object. As long as stub
     * or skeleton instances exist, the factory will not get reclaimed.
     */
    static void invokeFactoryRefMethod(Class clazz, Object factory)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        clazz.getMethod(FACTORY_REF_METHOD_NAME, Object.class).invoke(null, factory);
    }

    /**
     * Hidden class for passing identifiers during stub and skeleton code
     * generation. It needs to be public to be accessible by generated
     * code. The purpose of this class is to allow thread safe initialization
     * of the method ids in the generated stub and skeleton classes. Static
     * fields set during class initialization are guaranteed to be visible by
     * all threads according to the java memory model. This class uses a thread
     * local variable to transfer the ids into a static initializer, which
     * cannot define any parameters.
     */
    public static class IdentifierSet {
        private static final ThreadLocal<Identifier[]> localIds = new ThreadLocal<Identifier[]>();

        public static Identifier[] get() {
            return localIds.get();
        }

        static void setMethodIds(RemoteInfo info) {
            Identifier[] ids = new Identifier[info.getRemoteMethods().size()];
            int methodOrdinal = 0;
            for (RemoteMethod method : info.getRemoteMethods()) {
                ids[methodOrdinal++] = method.getMethodID();
            }
            localIds.set(ids);
        }

        static void clearIds() {
            localIds.remove();
        }

        private IdentifierSet() {
        }
    }
}
