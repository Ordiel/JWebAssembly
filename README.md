# JWebAssembly

[![Build Status](https://api.travis-ci.com/i-net-software/JWebAssembly.svg)](https://app.travis-ci.com/github/i-net-software/JWebAssembly)
[![License](https://img.shields.io/github/license/i-net-software/jwebassembly.svg)](https://github.com/i-net-software/jwebassembly/blob/master/LICENSE.txt)
[![Coverage Status](https://coveralls.io/repos/github/i-net-software/JWebAssembly/badge.svg?branch=master)](https://coveralls.io/github/i-net-software/JWebAssembly?branch=master)
[![Maven](https://img.shields.io/maven-central/v/de.inetsoftware/jwebassembly-compiler.svg)](https://mvnrepository.com/artifact/de.inetsoftware/jwebassembly-compiler)
[![JitPack](https://jitpack.io/v/i-net-software/jwebassembly.svg)](https://jitpack.io/#i-net-software/jwebassembly/master-SNAPSHOT)


JWebAssembly is a Java bytecode to [WebAssembly](http://webassembly.org/) compiler. It uses Java class files as input. That it can compile any language that compile to Java bytecode like Clojure, Groovy, JRuby, Jython, Kotlin and Scala.
As output it generates the binary format (.wasm file) or the text format (.wat file). The target is to run Java natively in the browser with WebAssembly.

The difference to similar projects is that not a complete VM with GC and memory management should be ported. It's more like a 1: 1 conversion. The generated WebAssembly code is similar in size to the original Java class files.

## Documentation
The documentation can be found in the [wiki](https://github.com/i-net-software/JWebAssembly/wiki).

## Roadmap
The project is currently not production ready but you can run already some tests.

### Version 1.0 (Milestone 1)

#### Desired Features

* [x] Java byte code parser
* [x] test framework
* [x] Public API of the Compiler see [class JWebAssembly](src/de/inetsoftware/jwebassembly/JWebAssembly.java)
* [x] [Gradle Plugin](https://github.com/i-net-software/JWebAssembly-Gradle)
* [x] [Emulator](https://github.com/i-net-software/JWebAssembly/wiki/Debugging)
* [x] Binary format file writer and Text format file writer
* [x] Support for native methods [#2](https://github.com/i-net-software/JWebAssembly/issues/2)
* [x] Memory Management - currently with a polyfill on JavaScript side
* [x] invoke static method calls
* [x] invoke instance method calls
* [x] invoke interface method calls
* [x] invoke dynamic method calls (lambdas)
* [x] invoke default method calls
* [x] String support
* [x] Simple Class object support
* [x] static constructors
* [ ] Enum support
* [x] Optimizer - Optimize the WASM output of a single method after transpiling before writing to output
* [x] Hello World sample [(live)](https://i-net-software.github.io/JWebAssembly/samples/HelloWorld/HelloWorld.html), [(source code)](https://github.com/i-net-software/JWebAssembly/blob/master/docs/samples/HelloWorld/HelloWorld.java)

#### Status of Required WebAssembly Features
The following table shows the status of future WebAssembly features required by JWebAssembly in nightly builds in various implementations. These features are already used by the trunk version of JWebAssembly. If you want know the status of your current browser then look for [your browser support](https://wasm-feature-detect.surma.technology/).

| Feature                 | Importance | V8/Chrome | SpiderMonkey/FF | WABT   |
| ----------------------- |----------- | --------- | --------------- | ------ |
| [Mutable Globals][1]    | high       | yes       | yes             | yes    |
| [float-to-int][2]       | high       | yes       | yes             | yes    |
| [Sign-extension][3]     | high       | yes       | yes             | yes    |
| [Reference Types][4]    | high       | yes       | yes             | yes    |
| [JavaScript BigInt][5]  | medium     | yes       | yes             | yes    |
| ~~[Multi-value][6]~~    | medium     | yes       | -               | yes    |

- For V8 it based on the [V8 - node.js integrations builds](https://ci.chromium.org/p/v8/builders/luci.v8.ci/V8%20Linux64%20-%20node.js%20integration).
- For SpiderMonkey it based on the nightly build of [jsshell](https://archive.mozilla.org/pub/firefox/nightly/latest-mozilla-central/).
- For WABT it based on [libwabt.js](https://github.com/WebAssembly/wabt/blob/master/demo/libwabt.js) via node module wabt@nightly.

To use it also some flags and switches are currently needed.

Importance: All with high marked features are required for a hello word sample. For a first version that can be used for production.

### Version 2.0 (Milestone 2)

#### Desired Features

* [ ] Full featured [library for accessing the DOM](https://github.com/i-net-software/JWebAssembly-API/tree/master/src/de/inetsoftware/jwebassembly/web).
* [ ] Resources (properties, images, etc.)

### Version 3.0 (Milestone 3)

#### Desired Features

* [ ] Exception handling - required the next version of WebAssembly
* [ ] Multiple threads - required the next version of WebAssembly
* [ ] Memory Management with build in GC without JavaScript polyfill
* [ ] More Optimize like Tail Calls

#### Status of Required WebAssembly Features

| Feature                 | Importance | V8/Chrome | SpiderMonkey/FF | WABT   |
| ----------------------- |----------- | --------- | --------------- | ------ |
| [Garbage collection][7] | medium     | -         | [partly][11]    | -      |
| [Exceptions][8]         | low        | partly    | -               | partly |
| [Threads][9]            | low        | yes       | ?               | yes    |
| [Tail call][10]         | very low   | yes       | ?               | yes    |

Required Java Version
----
The JWebAssembly compiler requires Java SE 8 or higher. It is tested with Java SE 8 on [travis-ci.org](https://travis-ci.org/i-net-software/jwebassembly).

## Usage

### Exporting functions
To export a Java function to make it accessible from JavaScript, you must add the annotation de.inetsoftware.jwebassembly.api.annotation.Export.

```java
import de.inetsoftware.jwebassembly.api.annotation.Export;

@Export
public static int add( int a, int b ) {
    return a + b;
}
```

### importing functions
To import a JavaScript function to make it accessible from Java, you must add the annotation de.inetsoftware.jwebassembly.api.annotation.Import.
The method can be declared native or can have a Java implementation which will be ignored on compiling.

```java
import de.inetsoftware.jwebassembly.api.annotation.Import;

@Import( module = "global.Math", name = "max" )
static int max( int a, int b) {
    return Math.max( a, b );
}
```


### Java Limits
In version 1 of WebAssembly you can only compile:
* static methods
* use the data types int, long float and double

This is state of JWebAssembly 0.1. With version 0.2 you can use Objects via JavaScript polyfill.

### Alternatives
* [TeaVM](https://github.com/konsoletyper/teavm)
* [CheerpJ](https://www.leaningtech.com/pages/cheerpj.html)

## For Tool Developer

If you want to develop some tools like plugins for a build system or an IDE, then you need
* to include the full contents of the packages [de.inetsoftware.jwebassembly](https://github.com/i-net-software/JWebAssembly/tree/master/src/de/inetsoftware/jwebassembly) and [de.inetsoftware.classparser](https://github.com/i-net-software/JWebAssembly/tree/master/src/de/inetsoftware/classparser) and its subpackages.
* Create an instance of [de.inetsoftware.jwebassembly.JWebAssembly](https://github.com/i-net-software/JWebAssembly/blob/master/src/de/inetsoftware/jwebassembly/JWebAssembly.java) class and use its API.

[1]: https://github.com/WebAssembly/mutable-global
[2]: https://github.com/WebAssembly/nontrapping-float-to-int-conversions
[3]: https://github.com/WebAssembly/sign-extension-ops
[4]: https://github.com/WebAssembly/reference-types
[5]: https://github.com/WebAssembly/JS-BigInt-integration
[6]: https://github.com/WebAssembly/multi-value
[7]: https://github.com/webassembly/gc
[8]: https://github.com/WebAssembly/exception-handling
[9]: https://github.com/WebAssembly/threads
[10]: https://github.com/webassembly/tail-call
[11]: https://github.com/lars-t-hansen/moz-gc-experiments
