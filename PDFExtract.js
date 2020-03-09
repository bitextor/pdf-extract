//Analyzes the lines in a chunk/paragraph to deterine if a line should be joined to the line that follows it.
function analyzeJoins(lines, lang) {

  //<span id="page1c1p1l1" joinScore="100.00" style="top:0px;left:0px;width:0px;height:0px;">
  switch(lang.toLower()) {
    case "en":
      // code block
      break;
    case "fr":
      // code block
      break;
    default:
      // code block
   }
   return lines;
}

//Adjusts the line to repair poorly rendered objects that may be split, but are one.
function repairObjectSequence(line) {

   return line;
}

//Custom detection that would identify a section as a header. This is only called if internal logic has not already identified the content as a header and is within the first 5 paragraphs on the page.
function isHeader(lines, pageWidth, pageHeight) {
   //code block
   return false;
}

//Custom detection that would identify a section as a footer. This is only called if internal logic has not already identified the content as a footer and is within the last 5 paragraphs on the page.
function isFooter(lines, pageWidth, pageHeight) {
  //code block
  return false;
}
