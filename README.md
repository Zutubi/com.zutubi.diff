Zutubi Diff
===========

Introduction
------------

Zutubi Diff is a Java library for parsing and applying patch files in unified
diff and other formats.  You can think of it as the library equivalent of the
patch(1) utility, with the beginnings of support for more than just unified
diffs.

Home Page
---------

Zutubi diff has home on the web at:

http://zutubi.com/source/projects/com.zutubi.diff/

License
-------

Zutubi diff is licensed under the Apache License, version 2.0.  See the LICENSE
file for details.

Quick Start
-----------

The best way to illustrate usage is with a simple example:

    PatchFileParser parser = new PatchFileParser(new UnifiedPatchParser());
    PatchFile patchFile = parser.parse(new FileReader("/path/to/my.patch"));
    patchFile.apply(toDirectory, 3);

This code parses a patch from the file `my.patch` and applies it to the
directory `toDirectory`, stripping 3 slashes from the paths.  It is analogous
to running:

    $ cd toDirectory
    $ patch -p3 < /path/to/my.patch

Full code for this example can be found in the `examples/patch` directory.  See
below for build instructions.

Supported Formats
-----------------

Currently full support is provided for unified diffs in the package
`com.zutubi.diff.unified`.  Limited support for git diffs, an extension of the
unified format used by git, Mercurial and other tools, is provided in the
`com.zutubi.diff.git` package.  At present application of git diffs is not
implemented.

Building
--------

A simple Gradle (http://gradle.org/) build is included.  To build the library as
a jar run:

    $ gradle jar

The jar will appear in `build/libs`.  There are no dependencies - the jar
stands alone.  The build has only been tested with Gradle 1.2.

### Example

After building the main jar, you can build the example in a similar way.  You
can then run it as a very-limited version of patch:

    $ cd examples/patch
    $ gradle jar
    $ java -jar build/libs/patch.jar -p3 < my.patch

The patch can only be applied to the working directory.

Javadoc
-------

You can generate full Javadoc for the library by running:

    $ gradle javadoc

The documentation will appear in `build/docs/javascript`.

Feedback
--------

Feedback and contributions are welcome!  Please contact:

jason@zutubi.com

or simply fork away!
