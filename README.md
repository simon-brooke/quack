# quack

> If it walks like a duck, and it quacks like a duck, it's a duck.

A validator for [ActivityStreams](https://www.w3.org/TR/activitystreams-core/) documents.

Part of the [dog-and-duck](https://github.com/simon-brooke/dog-and-duck) project, q.v.

## Installation

Download from http://example.com/FIXME.

## Usage

```
java -jar target/dog-and-duck-0.1.0-standalone.jar -i resources/activitystreams-test-documents/vocabulary-ex10-jsonld.json -f html -o report.html -s info
```

Note that it is almost certain that in some places I have misinterpreted the spec. Of all 205 documents in the [activitystreams-test-documents repository](https://github.com/w3c-social/activitystreams-test-documents), not a single one passes validation, and that must be wrong.

Nevertheless I think that this is a basis on which a useful validator can be built. Feedback and contributions welcome.

## Options

The full range of command-line switches is as follows:
```
    -i, --input SOURCE    standard input   The file or URL to validate
    -o, --output DEST     standard output  The file to write to, defaults to standard out
    -f, --format FORMAT   :edn             The format to output, one of `edn` `csv` `html`
    -l, --language LANG   en-GB            The ISO 639-1 code for the language to output
    -s, --severity LEVEL  :info            The minimum severity of faults to report
    -h, --help                             Print this message and exit
```

Note, though, that internationalisation files for languages other than British English have not yet been written, and that one is not complete.

The following severity levels are understood:

   0. `info` things which are not actuallys fault, but issues noted during
      validation;
   1. `minor` things which I consider to be faults, but which
      don't actually breach the spec;
   2. `should` instances where the spec says something *SHOULD*
      be done, which isn't;
   3. `must` instances where the spec says something *MUST*
      be done, which isn't;
   4. `critical` instances where I believe the fault means that
      the object cannot be meaningfully processed.


## Examples

...

## Documentation

Full documentation is [here](https://simon-brooke.github.io/dog-and-duck/).

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2023 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
