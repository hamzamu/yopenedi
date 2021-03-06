const _ = require('lodash');
import './grammar.js'


parseEDI = {}
parseEDI.regex = {
    line: /['\n\r]+/,
    segment: /(\?.|[^\+])+/g,
    element: /(\?.|[^\+])/g,
    component: /(\?.|[^:])+/g
}

/* -------------------------------------------------------------------------- */

// var fileName = 'noname_order_sample_from_REXEL_02062020_1'
// var doc = Assets.getText(fileName)

/* -------------------------------------------------------------------------- */

var keys = [];
var tags = [];
var ediData = [];
var linesArr = []



var dataToReplace = [{
        replace: "<PARTY_ROLE>SU</PARTY_ROLE>",
        value: "<PARTY_ROLE>supplier</PARTY_ROLE>"
    },
    {
        replace: "<PARTY_ROLE>DP</PARTY_ROLE>",
        value: "<PARTY_ROLE>delivery</PARTY_ROLE>"
    },
    {
        replace: "<PARTY_ROLE>BY</PARTY_ROLE>",
        value: "<PARTY_ROLE>buyer</PARTY_ROLE>"
    }
]

/* -------------------------------------------------------------------------- */

parse = {}


parse.parseEdiDoc = function (doc) {
    console.log('______')
    setKeys(doc)
    var jsonReady = generateStructuredArr()
    var newXML = jsonToXML(jsonReady)
    var newXML = replaceTags(newXML)
    return newXML;
}


/* -------------------------------------------------------------------------- */



function setKeys(doc) {
    var lines = doc.split(/['\n\r]+/);
    lines = lines.map(function (line) {
        if (line) {
            var key = line.substring(0, 3)
            keys.push({
                [key]: line
            })
            tags.push(key)
            linesArr.push(line)
            var segs = getSegment(line)
            ediData.push(segs);
        }
    });
    console.log(ediData[3])
    return lines;
    //
}

/* -------------------------------------------------------------------------- */
function getSegment(line) {
    var segs = line.match(parseEDI.regex.segment)
    var key = line.substring(0, 3)
    var elements = [
        [],
        []
    ]
    var segs = segs.map((seg) => {
        if (seg !== key) {
            if (seg.indexOf(":") > -1) {
                elements[1] = seg.split(':');
            } else {
                elements[0].push(seg)
            }
        } else {
            elements[0].push("")
        }
    })
    ////
    var segs = _.compact(segs);
    var lineData = _.flatten(elements)
    var lineData = elements[1]
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    var grammar = grammar ? grammar : null;
    var matchedData = null;
    //
    if (grammar && grammar.render && grammar.match) {
        // var matchedData = matchData(lineData, grammar.match)
        console.log('{getSegment}: Matching Data: Rendering Segement Data: ',key)
        var matchedData = matchDataBlock(elements, grammar.match)
    } else {
        console.error('{getSegment}::No Matched Data Grammar keys.{Render:Match}',)
        
    }
    var out = {
        key,
        line,
        segs,
        elements,
        lineData,
        matchedData
    }
    var out = _.assign(out, grammar)
    return out;
}


/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */
// Render XML // REPLACE THE DATA 
// MATCH DATA WITH RENDERED LINE
function getXMLElement(index) {
    // console.log('getXMLElement',ediData[index].key)
    if (!ediData[index]) {
        return
    }
    var elementsAll = ediData[index].elements
    var line = ediData[index].render
    var data = ediData[index].matchedData
    if (!data) {
        console.error('{getXMLElement}::There is No Data for:', ediData[index].key)
        return
    }
    // console.log(ediData[index])
    if (ediData[index].cases && ediData[index].cases[0]) {
        var casee = ediData[index].cases
        var find = _.find(data, (o) => {
            if (o[casee[0]]) {
                return o[casee[0]]
            }
        })
        var key = casee[0];
        var line = ediData[index][find[key]]
        // console.log({
        //     casee,
        //     data,
        //     key,
        //     find,
        //     line
        // })
    }
    // Looping and replacing line:
    for (i = 0; i < data.length; i++) {
        var key = _.keys(data[i])[0]
        if (ediData[index].cases && !key == ediData[index].cases[0]) {
            var line = ediData[index].exc(data[i][key])
        }
        var line = line.replace(key, data[i][key])
    }
    return line
}
/* -------------------------------------------------------------------------- */
/**
 * matchDataBlock
 * GENERATE MatchedBlock for the Data.
 *  USED for SIMPLE TAGS

 */
// matchDataBlock( [ [ '', '220', '0351485311' ], [] ], [["","$code","$id"],[]])
// matchDataBlock( [ [ '', '220', '0351485311' ], ["EU","EURO", "CH"] ], [["","$code","$id"],["$re","$curr", "$country"]])
function matchDataBlock(dataArr, grammarArr) {
    if (!grammarArr || !dataArr) {
        console.log('matchDataBlock: ERROR')
        throw new Meteor.Error('grammarArr Match has error', "error")
    }
    var output = []
    //
    _.each(grammarArr, (elementsArr, index) => {
        _.each(elementsArr, (ele, i) => {
            if (ele.length) {
                output.push({
                    [ele]: dataArr[index][i] ? dataArr[index][i] : ""
                })
            }
        })
    })
    //
    // console.log('matchDataBlock: Output', output)
    return output;
}

/* -------------------------------------------------------------------------- */

function setEnclosedTags(arr, tag, enclosed) {
    var newArr = []
    _.each(arr, (el, index) => {
        if (el.tag == tag) {
            console.log('______ HEAD', tag)
            if (arr[index - 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed
                })
            }
            newArr.push(el)
            if (arr[index + 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed,
                    close: enclosed
                })
            }
        } else {
            newArr.push(el)
        }
    })
    return newArr;
}


/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
function generateStructuredArr() {
    var structuredArr = []
    strucJSON = [] //
    var orderJSON = []
    var parent = ["UNB", "UNG", "UNH", "LIN", "NAD", "CUX"]
    var start = ["UNB", "UNG", "UNH", "LIN"]
    var skip = ["UNT", "UNE", "UNZ"]
    var looped = ["LIN", "NAD"] //
    var skipRendering = ["RFF", ""]
    _.each(tags, (tag, index) => {
        var prev = tag;
        var line = linesArr[index].substring(4, 100)
        var i = index;
        // console.log(i, tag, line)
        /* -------------------------------------------------------------------------- */
        // DATA
        // TAG CONTROLS 
        var element = ediData[index]
        if (element) {
            var elementsAll = ediData[index].elements
            // var line = ediData[index].render
            var data = ediData[index].matchedData
            // var rendered = getXMLElement(index) ? getXMLElement(index) : ediData[index].render
            // var key = elementsAll['key']
            // if has children 
        }
        /* -------------------------------------------------------------------------- */
        // SETTING UP PARENT STRUCTURE
        if (_.includes(start, tag)) {
            if (strucJSON.length == 0 && tag) {
                strucJSON.push("Root")
            }
            if (strucJSON[strucJSON.length - 1] !== tag) {
                // console.log("New Tag", tag, strucJSON[strucJSON.length - 1])
                console.log('PARENT: ', strucJSON[strucJSON.length - 1], '> ', tag)
                strucJSON.push(tag)
            } else if (strucJSON[strucJSON.length - 1] === tag) {
                // console.log("LOOP: =======================", tag, strucJSON[strucJSON.length - 1])
            }
        }
        /* -------------------------------------------------------------------------- */
        // PUSHING THE DATA.....
        // SKIP CLOSING TAG
        if (_.includes(skip, tag)) {
            // console.log('SKIP START', tag, strucJSON)
            strucJSON.splice(strucJSON.lastIndexOf(tag), 1);
            // console.log('SKIP END RM', tag, strucJSON)
            orderJSON.push(tag)
            // Adding Skipp Tags 
            structuredArr.push({
                tag: tag,
                line: line,
                close: strucJSON[strucJSON.length - 1],
                skip: true,
                index: index,
                // rendered: rendered,
                data: data
            })
            return
        }
        // PARENT -> CHILDREN // 
        if (_.includes(parent, tag)) {
            // console.log('MATCHED', tag)
            orderJSON.push(tag)
            structuredArr.push({
                tag: tag,
                line: line,
                index: index,
                children: [],
                parent: strucJSON[strucJSON.length - 2],
                // rendered: rendered,
                data: data,
                isParent: true
            })
        } else {
            var parentTag = structuredArr[structuredArr.length - 1]
            // SKIP if there no Parent Tag or the Tag does not contact children
            if (!parentTag || !parentTag.children) {
                return
            }
            // console.log({parentTag}, parentTag.children)
            parentTag.children.push({
                tag: tag,
                line: line,
                index: index,
                parent: structuredArr[structuredArr.length - 1].tag,
                data: data,
                // rendered: rendered
            })
        }
    })
    // Inject enclose tags looped
    var arr = setEnclosedTags(structuredArr, "NAD", "PARTIES")
    var arr = setEnclosedTags(arr, "LIN", "PRODUCTS")
    var arr = generatePriceLineAmount(arr)
    // var arr = setTagBeforeAfter(arr, "ORDER_HEADER", "BGM", "PRODUCTS")
    // console.log({
    //     strucJSON,
    //     orderJSON,
    //     // structuredArr
    // }, structuredArr)
    return arr;
}

/* -------------------------------------------------------------------------- */
// var arr = ["UNH", "UNH", "NAD", "NAD", "NAD", "CUX", "LIN", "LIN", "LIN", "UNH"]
// setEnclosedTags(arr, 'NAD', 'Pareties')
// setEnclosedTags(arr, 'LIN', 'PRODUCTS')
function setEnclosedTags(arr, tag, enclosed) {
    var newArr = []
    _.each(arr, (el, index) => {
        if (el.tag == tag) {
            console.log('______ HEAD', tag)
            if (arr[index - 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed
                })
            }
            newArr.push(el)
            if (arr[index + 1]["tag"] !== tag) {
                newArr.push({
                    tag: enclosed,
                    close: enclosed
                })
            }
        } else {
            newArr.push(el)
        }
    })
    return newArr;
}

/* -------------------------------------------------------------------------- */
// check LENGTH 
// STRUCJSON //
// PARENT KEY
//  GENERATE PRICE {PRICE_ITEM}
function generatePriceLineAmount(arr) {
    _.each(arr, (jsonElem, index) => {
        // console.log(jsonElem)
        var price, qty, priceLinePrice;
        if (jsonElem.tag == "LIN") {
            _.each(jsonElem.children, (child) => {
                if (child.tag == "QTY") {
                    var data = ediData[child.index].matchedData;
                    var find = _.find(data, (o) => {
                        if (o["$qty"]) {
                            return o["$qty"]
                        }
                    })
                    qty = find["$qty"]
                }
                if (child.tag == "PRI") {
                    var data = ediData[child.index].matchedData;
                    var find = _.find(data, (o) => {
                        if (o["$PRICE"]) {
                            return o["$PRICE"]
                        }
                    })
                    price = find['$PRICE']
                    // console.log({price})
                }
            })
            if (qty && price) {
                priceLinePrice = qty * price;
                priceLinePrice = priceLinePrice.toFixed(2)
                var newTag = {
                    name: "PRICE_LINE_AMOUNT",
                    tag: "PRICE_LINE_AMOUNT",
                    data: [
                        [priceLinePrice],
                        []
                    ],
                    render: '<PRICE_LINE_AMOUNT>' + priceLinePrice + '</PRICE_LINE_AMOUNT>',
                    isRendered: true
                }
                arr[index].children.push(newTag)
                // console.log('_______________________D', {
                //     qty,
                //     price,
                //     priceLinePrice,
                //     newTag
                // })
            }
            // console.log(jsonElem.children)
        }
    })
    return arr
}
/* -------------------------------------------------------------------------- */
function jsonToXML(jsonArr) {
    console.log('------------------')
    var keepOpen = ["UNB", "UNG", "UNH"]
    var start = ["UNB", "UNG", "UNH"]
    var skip = ["UNT", "UNE", "UNZ"]
    var xml = []
    var struc = []
    xml.push('<?xml version="1.0" encoding="utf-8" standalone="yes"?>')
    xml.push('<ORDER type="standard" xmlns="http://www.opentrans.org/XMLSchema/2.1" xmlns:bmecat="http://www.bmecat.org/bmecat/2005" version="2.1">')
    // ORDER_HEADER
    xml.push('<ORDER_HEADER>')
    xml.push(`<CONTROL_INFO>
    <GENERATOR>Yopenedi</GENERATOR>
    <GENERATION_DATE>` + new Date() + `</GENERATION_DATE>
    </CONTROL_INFO>`)
    //
    // ORDER_INFO
    xml.push('<ORDER_INFO>')
    _.each(jsonArr, (jsonElem) => {
        var tag = jsonElem.tag;
        console.log('Processing:', tag)
        if (struc.length == 0 && tag) {
            struc.push("Root")
        }
        if (_.includes(start, tag)) {
            if (struc[struc.length - 1] !== tag) {
                struc.push(tag)
            }
        }
        // EXCEPTION !!!!
        // if (tag == "PRODUCTS" && !tag.close) {
        //     xml.push('</ORDER_INFO></ORDER_HEADER>')
        //     return
        // }
        //---------------- Handle Close Tags -----------------------//
        /** 
         * 
         * 1- SKIP TAG  remove from the controller array
         * 2- Add end tag
         * */
        if (_.includes(skip, tag)) {
            // console.log('Skipping:', tag)
            // console.log('Removing ', struc[struc.length - 1], struc)
            // console.log('===============', getGrammar(struc[struc.length - 1], "tag"))
            // xml.push("</" + struc[struc.length - 1] + ">")
            if (getGrammar(struc[struc.length - 1], "tag")) {
                xml.push("</" + getGrammar(struc[struc.length - 1], "tag") + ">");
            }
            struc.splice(struc.lastIndexOf(tag), 1);
            return
        }
        //----------------------------------------------------------
        // PRODUCTS // SHOULD FIX
        if (tag == "PRODUCTS") {
            console.log('++++++++++++++ PRODUCTS')
            if (jsonElem.close) {
                xml.push("</" + getGrammar(tag, 'tag') + ">")
            } else {
                xml.push('</ORDER_INFO></ORDER_HEADER>')
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            return
        }
        if (tag == "PARTIES") {
            console.log('++++++++++++++ PARTIES')
            if (jsonElem.close) {
                xml.push("</" + getGrammar(tag, 'tag') + ">")
            } else {
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            return
        }
        /* -------------------------------------------------------------------------- */
        // IF HAS CHILDREN
        if (jsonElem.children) {
            if (getGrammar(tag, 'tag')) {
                xml.push("<" + getGrammar(tag, 'tag') + ">")
            }
            var parent = jsonElem
            // console.log("Children Renderring", jsonElem.children.length, jsonElem.tag)
            if (jsonElem.children.length) {
                xml.push(getXMLElement(jsonElem.index))
                _.each(jsonElem.children, (child, i) => {
                    //
                    // SETTING CACULATED DATA: (PRICE_LINE_AMOUNT)
                    if (!child.index) {
                        xml.push(child.render)
                    } else {
                        xml.push(getXMLElement(child.index))
                    }
                    // console.log("PARENT: ", parent.tag, '->  Child: ', child.tag)
                })
            } else {
                // xml.push(jsonElem.line)
                // xml.push('<!-- has no children NOCHILDREN-->')
                xml.push(getXMLElement(jsonElem.index))
            }
            // KEEP THE EDIFACT META TAG OPEN
            if (!_.includes(keepOpen, jsonElem.tag)) {
                // console.log('keepOpen: ', keepOpen, "</" + jsonElem.tag + ">")
                if (getGrammar(parent.tag, 'tag')) {
                    xml.push("</" + getGrammar(parent.tag, 'tag') + ">")
                }
            }
            //
        } else {
            //IF is PARENT AND has NO Children ex. [NAD]
            // Pushing DATA LINE
            // xml.push("LINEEEEEEEEE")
            //
            xml.push("</" + jsonElem.tag + ">")
        }
    })
    // console.log({
    //     struc
    // })
    xml.push(`
    <ORDER_SUMMARY>
        <TOTAL_ITEM_NUM>11</TOTAL_ITEM_NUM>
        <TOTAL_AMOUNT>1080.25</TOTAL_AMOUNT>
    </ORDER_SUMMARY>
    `)
    xml.push('</ORDER>')
    xml.push('')
    var xml = xml.join("")
    return xml;
}

/* -------------------------------------------------------------------------- */

/* -------------------------------------------------------------------------- */


// USED TO REPLACE CERTAIN TAGS 
function replaceTags(xml) {
    for (i = 0; i < dataToReplace.length; i++) {
        var tag = dataToReplace[i].replace
        let re = new RegExp(tag, 'g');
        var xml = xml.replace(re, dataToReplace[i].value);
    }
    return xml
}


/* -------------------------------------------------------------------------- */
function getGrammar(key, object) {
    var grammar = _.find(Grammar, (o) => {
        return o.name == [key]
    })
    if (grammar && grammar[object]) {
        // console.log('getGrammar: ', grammar[object])
        return grammar[object];
    }
}

/* -------------------------------------------------------------------------- */

module.exports = parse