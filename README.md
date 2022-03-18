tinyjson
========
[WIP] A lightweight and modern json parser for Java 8+.

## Features
- Very lightweight
   - Single file
- Modern API
- Streaming parsing (missing from mjson)
- Preserves order
- No reflection
- Should be very fast (I have not benchamrked it)

## See also
- [Jackson](https://github.com/FasterXML/jackson)
   - One serialization library to rule them all
- [mjson](https://github.com/bolerio/mjson)
  - Library with similar goals & modern API
  - very slightly more bloated
  - No support for [streaming](https://github.com/bolerio/mjson/issues/31)
  - Does not preserve order [See PR #35](https://github.com/bolerio/mjson/pull/35)
- [gson](https://github.com/google/gson)
  - Middle ground between jackson and json
- [json\_simple](https://github.com/cliftonlabs/json-simple)
  - Fairly outdated deesign
  - Battle tested
  - Not single file  

## TODO
- Finish parsing
- Implement serialization -> StringBuffer
- Actually test/document

