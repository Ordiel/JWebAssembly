/*
   Copyright 2018 - 2022 Volker Berlin (i-net software)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/
package de.inetsoftware.jwebassembly.module;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.inetsoftware.jwebassembly.WasmException;
import de.inetsoftware.jwebassembly.javascript.JavaScriptSyntheticFunctionName;
import de.inetsoftware.jwebassembly.module.TypeManager.StructType;
import de.inetsoftware.jwebassembly.wasm.AnyType;
import de.inetsoftware.jwebassembly.wasm.NamedStorageType;
import de.inetsoftware.jwebassembly.wasm.StructOperator;
import de.inetsoftware.jwebassembly.wasm.ValueType;

/**
 * WasmInstruction for struct operation. A struct is like a Java class without methods.
 * 
 * @author Volker Berlin
 *
 */
class WasmStructInstruction extends WasmInstruction {

    @Nonnull
    private final StructOperator   op;

    private       StructType       type;

    private final NamedStorageType fieldName;

    private SyntheticFunctionName functionName;

    private final WasmOptions     options;

    /**
     * Create an instance of numeric operation.
     * 
     * @param op
     *            the struct operation
     * @param typeName
     *            the type name of the parameters
     * @param fieldName
     *            the name of field if needed for the operation
     * @param javaCodePos
     *            the code position/offset in the Java method
     * @param lineNumber
     *            the line number in the Java source code
     * @param types
     *            the type manager
     */
    WasmStructInstruction( @Nonnull StructOperator op, @Nonnull String typeName, @Nullable NamedStorageType fieldName, int javaCodePos, int lineNumber, TypeManager types ) {
        this( op, types.valueOf( typeName ), fieldName, javaCodePos, lineNumber, types );
    }

    /**
     * Create an instance of numeric operation.
     * 
     * @param op
     *            the struct operation
     * @param type
     *            the type of the parameters
     * @param fieldName
     *            the name of field if needed for the operation
     * @param javaCodePos
     *            the code position/offset in the Java method
     * @param lineNumber
     *            the line number in the Java source code
     * @param types
     *            the type manager
     */
    WasmStructInstruction( @Nonnull StructOperator op, @Nonnull StructType type, @Nullable NamedStorageType fieldName, int javaCodePos, int lineNumber, TypeManager types ) {
        super( javaCodePos, lineNumber );
        this.op = op;
        this.type = type;
        this.fieldName = fieldName;
        if( type != null && fieldName != null ) {
            type.useFieldName( fieldName );
        }
        this.options = types.options;
    }

    /**
     * Create the synthetic polyfill function of this instruction for nonGC mode.
     * 
     * @return the function or null if not needed
     */
    SyntheticFunctionName createNonGcFunction() {
        switch( op ) {
            case NEW:
            case NEW_DEFAULT:
                functionName = new JavaScriptSyntheticFunctionName( "NonGC", "new_" + type.getName().replace( '/', '_' ), () -> {
                    // create the default values of a new type
                    StringBuilder js = new StringBuilder("() => Object.seal({");
                    List<NamedStorageType> list = type.getFields();
                    for( int i = 0; i < list.size(); i++ ) {
                        if( i > 0 ) {
                            js.append( ',' );
                        }
                        js.append( i ).append( ':' );
                        NamedStorageType storageType = list.get( i );
                        if( TypeManager.FIELD_VTABLE == storageType.getName() ) {
                            js.append( type.getVTable() );
                        } else {
                            AnyType fieldType = storageType.getType();
                            if( fieldType instanceof ValueType && fieldType != ValueType.externref ) {
                                js.append( '0' );
                            } else {
                                js.append( "null" );
                            }
                        }
                    }
                    js.append( "})" );
                    return js.toString();
                }, null, type );
                break;
            case SET:
                AnyType fieldType = fieldName.getType();
                functionName = new JavaScriptSyntheticFunctionName( "NonGC", "set_" + validJsName( fieldType ), () -> "(a,v,i) => a[i]=v", ValueType.externref, fieldType, ValueType.i32, null, null );
                break;
            case GET:
                fieldType = fieldName.getType();
                functionName = new JavaScriptSyntheticFunctionName( "NonGC", "get_" + validJsName( fieldType ), () -> "(a,i) => a[i]", ValueType.externref, ValueType.i32, null, fieldType );
                break;
            case INSTANCEOF:
                functionName = options.getInstanceOf();
                break;
            case CAST:
                functionName = options.getCast();
                break;
            default:
        }
        return functionName;
    }

    /**
     * Get a valid JavaScript name.
     * @param type the type
     * @return the identifier that is valid 
     */
    private static String validJsName( AnyType type ) {
        return type instanceof StructType ? "anyref" : type.toString();
    }

    /**
     * Get the StructOperator
     * 
     * @return the operator
     */
    StructOperator getOperator() {
        return op;
    }

    /**
     * Get the struct type of this instruction.
     * 
     * @return the type
     */
    StructType getStructType() {
        return type;
    }

    /**
     * Set a new type for NULL const. 
     * @param type the type
     */
    void setStructType( @Nonnull StructType type ) {
        this.type = Objects.requireNonNull( type );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Type getType() {
        return Type.Struct;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo( @Nonnull ModuleWriter writer ) throws IOException {
        String comment = null;
        int idx = -1;
        switch( op ) {
            case GET:
            case SET:
                // The fieldName of the struct operation does not contain the class name in which the field was declared. It contains the class name of the variable. This can be the class or a subclass.
                List<NamedStorageType> fields = type.getFields();
                boolean classNameMatched = type.getName().equals( fieldName.geClassName() );
                for( int i = fields.size()-1; i >= 0; i-- ) {
                    NamedStorageType field = fields.get( i );
                    if( !classNameMatched && field.geClassName().equals( fieldName.geClassName() ) ) {
                        classNameMatched = true;
                    }
                    if( classNameMatched && field.getName().equals( fieldName.getName() ) ) {
                        idx = i;
                        break;
                    }
                }
                if( !classNameMatched ) {
                    // special case, the type self does not add a needed field, that we search in all fields
                    for( int i = fields.size()-1; i >= 0; i-- ) {
                        NamedStorageType field = fields.get( i );
                        if( field.getName().equals( fieldName.getName() ) ) {
                            idx = i;
                            break;
                        }
                    }
                }
                comment = fieldName.getName();
                break;
            case INSTANCEOF:
            case CAST:
                idx = type.getClassIndex();
                break;
            default:
        }
        if( functionName != null ) { // nonGC
            if( idx >= 0 ) {
                writer.writeConst( idx, ValueType.i32 );
            }
            writer.writeFunctionCall( functionName, comment );
            if( op == StructOperator.CAST && options.useGC() ) {
                writer.writeStructOperator( StructOperator.RTT_CANON, type, null, -1 );
                writer.writeStructOperator( op, type, null, -1 );
            }
        } else {
            writer.writeStructOperator( op, type, fieldName, idx );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    AnyType getPushValueType() {
        switch( op ) {
            case NULL:
                return options.useGC() ? ValueType.eqref : ValueType.externref;
            case NEW:
            case NEW_DEFAULT:
            case CAST:
            case NEW_WITH_RTT:
                return type;
            case GET:
                return fieldName.getType();
            case SET:
                return null;
            case INSTANCEOF:
                return ValueType.i32; // a boolean value
            case RTT_CANON:
                return ValueType.i32; // rtt type
            default:
                throw new WasmException( "Unknown array operation: " + op, -1 );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    int getPopCount() {
        switch( op ) {
            case GET:
            case INSTANCEOF:
            case CAST:
            case NEW_WITH_RTT:
                return 1;
            case SET:
                return 2;
            case NEW:
            case NEW_DEFAULT:
            case NULL:
            case RTT_CANON:
                return 0;
            default:
                throw new WasmException( "Unknown array operation: " + op, -1 );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    AnyType[] getPopValueTypes() {
        switch( op ) {
            case GET:
                return new AnyType[] { type };
            case INSTANCEOF:
            case CAST:
                return new AnyType[] { options.types.valueOf( "java/lang/Object" ) };
            case NEW_WITH_RTT:
                return new AnyType[] { ValueType.i32 };// rtt type
            case SET:
                return new AnyType[] { type, fieldName.getType() };
            case NEW:
            case NEW_DEFAULT:
            case NULL:
            case RTT_CANON:
                return null;
            default:
                throw new WasmException( "Unknown array operation: " + op, -1 );
        }
    }
}
