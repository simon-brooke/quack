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
  -i, --input SOURCE    standard in   The file or URL to validate.
  -o, --output DEST     standard out  The file to write to.
  -f, --format FORMAT   :edn          The format to output, one of `csv`, `edn`, `json`, `html`.
  -l, --language LANG   en-GB         The ISO 639-1 code for the language to output.
  -s, --severity LEVEL  :info         The minimum severity of faults to report.
  -r, --reify                         If set, reify objects referenced by URIs and check them.
  -h, --help                          Print this message and exit.
```

Note, though, that internationalisation files for languages other than British English have not yet been written, and that one is not complete.

Note also that reification is not, at the time of writing, working.

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

Many. Ducks like bugs.

## License

Copyright © 2023 Simon Brooke

This Source Code is made available under GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version.
