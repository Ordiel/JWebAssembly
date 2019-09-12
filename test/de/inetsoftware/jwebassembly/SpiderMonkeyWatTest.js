load( "{test}.wasm.js" );
var wasm = wasmTextToBinary( read( "{test}.wat" ) );
var testData = JSON.parse( read( "testdata.json" ) );

function callExport(instance) {
    var result = {};
    for (var method in testData) {
        try{
            result[method] = instance.exports[method]( ...testData[method] ).toString();
        }catch(err){
            result[method] = err.toString();
        }
    }
    // save the test result
    const original = redirect( "testresult.json" );
    putstr( JSON.stringify(result) );
    redirect( original );
}

WebAssembly.instantiate( wasm, wasmImports ).then(
  obj => callExport(obj.instance),
  reason => console.log(reason)
);
