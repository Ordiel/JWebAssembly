/*
 * Copyright 2017 Volker Berlin (i-net software)
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
package de.inetsoftware.jwebassembly.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.inetsoftware.jwebassembly.module.ModuleWriter;
import de.inetsoftware.jwebassembly.module.ValueType;

/**
 * Module Writer for binary format. http://webassembly.org/docs/binary-encoding/
 * 
 * @author Volker Berlin
 */
public class BinaryModuleWriter extends ModuleWriter implements InstructionOpcodes {

    private static final byte[] WASM_BINARY_MAGIC   = { 0, 'a', 's', 'm' };

    private static final int    WASM_BINARY_VERSION = 1;

    private WasmOutputStream    wasm;

    private WasmOutputStream    codeStream          = new WasmOutputStream();

    private List<FunctionType>  functionTypes = new ArrayList<>();

    private Map<String,Function> functions = new LinkedHashMap<>();
    
    private Function            function;
    private FunctionType        functionType;

    /**
     * Create new instance.
     * 
     * @param output
     *            the target for the module data.
     * @throws IOException
     *             if any I/O error occur
     */
    public BinaryModuleWriter( OutputStream output ) throws IOException {
        wasm = new WasmOutputStream( output );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        wasm.write( WASM_BINARY_MAGIC );
        wasm.writeInt32( WASM_BINARY_VERSION );

        writeTypeSection();
        writeFunctionSection();
        writeCodeSection();

        wasm.close();
    }

    private void writeTypeSection() throws IOException {
        int count = functionTypes.size();
        if( count > 0 ) {
            WasmOutputStream stream = new WasmOutputStream();
            stream.writeVaruint32( count );
            for( FunctionType type : functionTypes ) {
                stream.writeVarint32( ValueType.func.getCode() );
                stream.writeVaruint32( type.params.size() );
                for( ValueType valueType : type.params ) {
                    stream.writeVarint32( valueType.getCode() );
                }
                if( type.result == null ) {
                    stream.writeVaruint32( 0 );
                } else {
                    stream.writeVaruint32( 1 );
                    stream.writeVarint32( type.result.getCode() );
                }
            }
            wasm.writeSection( SectionType.Type, stream, null );
        }
    }

    private void writeFunctionSection() throws IOException {
        int count = functions.size();
        if( count > 0 ) {
            WasmOutputStream stream = new WasmOutputStream();
            stream.writeVaruint32( count );
            for( Function func : functions.values() ) {
                stream.writeVaruint32( func.typeId );
            }
            wasm.writeSection( SectionType.Function, stream, null );
        }
    }

    private void writeCodeSection() throws IOException {
        WasmOutputStream stream = new WasmOutputStream();
        stream.writeVaruint32( functions.size() );
        codeStream.writeTo( stream );
        wasm.writeSection( SectionType.Code, stream, null );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodStart( String name ) throws IOException {
        function = new Function();
        function.id = functions.size();
        functions.put( name, function );
        functionType = new FunctionType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodParam( String kind, ValueType valueType ) throws IOException {
        switch( kind ) {
            case "param":
                functionType.params.add( valueType );
                return;
            case "return":
                functionType.result = valueType;
                return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeMethodFinish() throws IOException {
        // TODO optimize and search for duplicates
        function.typeId = functionTypes.size();
        functionTypes.add( functionType );
        codeStream.write( END );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeConstInt( int value ) throws IOException {
        codeStream.write( I32_CONST );
        codeStream.writeVarint32( value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeLoadInt( int idx ) throws IOException {
        codeStream.write( GET_LOCAL );
        codeStream.writeVaruint32( idx );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeStoreInt( int idx ) throws IOException {
        codeStream.write( SET_LOCAL );
        codeStream.writeVaruint32( idx );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAddInt() throws IOException {
        codeStream.write( I32_ADD );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeReturn() throws IOException {
        codeStream.write( RETURN );
    }
}
