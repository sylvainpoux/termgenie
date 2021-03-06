// @requires rules/common.js

function metazoan_location_specific_cell() {
  var c = getSingleTerm("cell", CL);
  var a = getSingleTerm("location", CL);
  if (c === null || a === null) {
    // check branch
    error("The specified terms do not correspond to the pattern");
    return;
  }
  var label = termname(a, CL) + " " + termname(c, CL);
  var definition = "Any "+termname(c, CL)+" that is part of a "+termname(a, CL)+".";
  var synonyms = null; // TODO
  var mdef = createMDef("?C and 'part of' some ?A");
  mdef.addParameter('C', c, CL);
  mdef.addParameter('A', a, CL);
  createTerm(label, definition, synonyms, mdef);
}
