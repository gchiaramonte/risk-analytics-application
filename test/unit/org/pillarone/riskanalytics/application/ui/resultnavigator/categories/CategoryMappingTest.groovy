package org.pillarone.riskanalytics.application.ui.resultnavigator.categories

import org.pillarone.riskanalytics.application.ui.resultnavigator.model.OutputElement
import org.pillarone.riskanalytics.application.ui.resultnavigator.categories.resolver.*

/**
 * @author martin.melchior
 */
class CategoryMappingTest extends GroovyTestCase {

    void testCategoryMapping() {
        def mappingClosure = {
            lob {
                or {
                    enclosedBy(prefix: ['linesOfBusiness:sub'], suffix: [':'])
                    conditionedOn (value: 'Aggregate') {
                        matching(toMatch: ["linesOfBusiness:(?!sub)"])
                    }
                }
            }
            peril {
                enclosedBy(prefix: ["claimsGenerators:sub"], suffix: [":"])
            }
            reinsuranceContractType {
                enclosedBy(prefix: ["subContracts:sub","reinsuranceContracts:sub"], suffix: [":"])
            }
            accountBasis {
                matching(toMatch: ["Gross", "Ceded", "Net"])
            }
            keyfigure {
                synonymousTo(category : "Field")
            }
        }

        Map<String,ICategoryResolver> map = MapCategoriesBuilder.getCategories(mappingClosure)
        
        CategoryMapping mapping = new CategoryMapping(map)

        assertTrue mapping.matcherMap.containsKey("lob")
        assertTrue mapping.matcherMap["lob"] instanceof OrResolver
        List<ICategoryResolver> orChildren = ((OrResolver)mapping.matcherMap["lob"]).children
        assertTrue orChildren[0] instanceof EnclosingMatchResolver
        assertEquals(((EnclosingMatchResolver)orChildren[0]).prefix, ['linesOfBusiness:sub'])
        assertEquals(((EnclosingMatchResolver)orChildren[0]).suffix, [':'])
        assertTrue orChildren[1] instanceof ConditionalAssignmentResolver
        assertEquals(((ConditionalAssignmentResolver)orChildren[1]).value, "Aggregate")
        assertTrue(((ConditionalAssignmentResolver)orChildren[1]).condition instanceof WordMatchResolver)

        assertTrue mapping.matcherMap.containsKey("peril")
        assertTrue mapping.matcherMap["peril"] instanceof EnclosingMatchResolver

        assertTrue mapping.matcherMap.containsKey("reinsuranceContractType")
        assertTrue mapping.matcherMap["reinsuranceContractType"] instanceof EnclosingMatchResolver

        assertTrue mapping.matcherMap.containsKey("accountBasis")
        assertTrue mapping.matcherMap["accountBasis"] instanceof WordMatchResolver
        assertEquals(((WordMatchResolver)mapping.matcherMap["accountBasis"]).toMatch, ["Gross", "Ceded", "Net"])

        assertTrue mapping.matcherMap.containsKey("keyfigure")
        assertTrue mapping.matcherMap["keyfigure"] instanceof SynonymToCategoryResolver
    }

    List<OutputElement> getTestOutputElements() {
        List<OutputElement> elements = []
        // a single wild card --> lob
        elements.add new OutputElement(path : "model:lines:property:premium")
        elements.add new OutputElement(path : "model:lines:property:claims")
        elements.add new OutputElement(path : "model:lines:casualty:premium")
        elements.add new OutputElement(path : "model:lines:casualty:claims")
        // a single wild card --> peril
        elements.add new OutputElement(path : "model:claims:eq:claims")
        elements.add new OutputElement(path : "model:claims:flood:claims")
        // a single wild card --> contracts
        elements.add new OutputElement(path : "model:reinsurance:WXL:claims")
        elements.add new OutputElement(path : "model:reinsurance:CXL:claims")
        // a two wild cards --> peril, lob
        elements.add new OutputElement(path : "model:claims:flood:lines:property:claims")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:claims")
        // a two wild cards --> peril, contracts
        elements.add new OutputElement(path : "model:claims:flood:reinsurance:CXL:claims")
        // a three wild cards --> peril, lob, contracts
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:WXL:claims")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:WXL:premium")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:CXL:claims")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:CXL:premium")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:WXL:claims")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:WXL:premium")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:CXL:claims")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:CXL:premium")

        for (OutputElement e : elements) {
            e.addCategoryValue(OutputElement.PATH, e.path)
        }
        return elements
    }

    CategoryMapping getTestCategoryMapping() {
        CategoryMapping mapping = new CategoryMapping()
        mapping.addCategory("lob", new EnclosingMatchResolver(":lines:",":",OutputElement.PATH))
        mapping.addCategory("contracts", new EnclosingMatchResolver(":reinsurance:",":",OutputElement.PATH))
        mapping.addCategory("perils", new EnclosingMatchResolver(":claims:",":",OutputElement.PATH))
        return mapping
    }

    List<OutputElement> getTestOutputElementsWithField() {
        List<OutputElement> elements = []
        // a single wild card --> lob
        elements.add new OutputElement(path : "model:lines:property:premium", field: "AA")
        elements.add new OutputElement(path : "model:lines:property:claims", field: "AA")
        elements.add new OutputElement(path : "model:lines:casualty:premium", field: "BB")
        elements.add new OutputElement(path : "model:lines:casualty:claims", field: "BB")
        // a single wild card --> peril
        elements.add new OutputElement(path : "model:claims:eq:claims", field: "BB")
        elements.add new OutputElement(path : "model:claims:flood:claims", field: "BB")
        // a single wild card --> contracts
        elements.add new OutputElement(path : "model:reinsurance:WXL:claims", field: "CC")
        elements.add new OutputElement(path : "model:reinsurance:CXL:claims", field: "BB")
        // a two wild cards --> peril, lob
        elements.add new OutputElement(path : "model:claims:flood:lines:property:claims", field: "CC")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:claims", field: "CC")
        // a two wild cards --> peril, contracts
        elements.add new OutputElement(path : "model:claims:flood:reinsurance:CXL:claims", field: "AA")
        // a three wild cards --> peril, lob, contracts
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:WXL:claims", field: "AA")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:WXL:premium", field: "AA")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:CXL:claims", field: "AA")
        elements.add new OutputElement(path : "model:claims:flood:lines:property:reinsurance:CXL:premium", field: "CC")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:WXL:claims", field: "CC")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:WXL:premium", field: "CC")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:CXL:claims", field: "CC")
        elements.add new OutputElement(path : "model:claims:eq:lines:property:reinsurance:CXL:premium", field: "CC")

        for (OutputElement e : elements) {
            e.addCategoryValue(OutputElement.PATH, e.path)
            e.addCategoryValue(OutputElement.FIELD, e.field)
        }
        return elements
    }

    CategoryMapping getTestCategoryMappingInclField() {
        CategoryMapping mapping = new CategoryMapping()
        mapping.addCategory("lob", new EnclosingMatchResolver(":lines:",":",OutputElement.PATH))
        mapping.addCategory("contracts", new EnclosingMatchResolver(":reinsurance:",":",OutputElement.PATH))
        mapping.addCategory("perils", new EnclosingMatchResolver(":claims:",":",OutputElement.PATH))
        mapping.addCategory("keyfigure", new SynonymToCategoryResolver(OutputElement.FIELD))
        return mapping
    }

    public void testCategorize() {
        CategoryMapping mapping = getTestCategoryMapping()
        List<OutputElement> elements = getTestOutputElements()
        mapping.categorize elements
        assertEquals 8, mapping.getWildCardPaths().size()

        String path = 'model:lines:${lob}:premium'
        List<String> wildCards = mapping.wildCardPaths[path].getPathWildCards()
        assertEquals 1, wildCards.size()
        assertTrue wildCards.contains("lob")
        List<String> values = mapping.wildCardPaths[path].getWildCardValues("lob")
        assertEquals 2, values.size()
        assertTrue values.contains("property")
        assertTrue values.contains("casualty")


        path = 'model:claims:${perils}:lines:${lob}:reinsurance:${contracts}:claims'
        wildCards = mapping.wildCardPaths[path].getPathWildCards()
        assertEquals 3, wildCards.size()
        assertTrue wildCards.contains("perils")
        assertTrue wildCards.contains("lob")
        assertTrue wildCards.contains("contracts")
        values = mapping.wildCardPaths[path].getWildCardValues("lob")
        assertEquals 1, values.size()
        assertTrue values.contains("property")
        values = mapping.wildCardPaths[path].getWildCardValues("perils")
        assertEquals 2, values.size()
        assertTrue values.contains("flood")
        assertTrue values.contains("eq")
        values = mapping.wildCardPaths[path].getWildCardValues("contracts")
        assertEquals 2, values.size()
        assertTrue values.contains("CXL")
        assertTrue values.contains("WXL")

        path = 'model:lines:${lob}:premium'
        assertEquals elements[0].wildCardPath, mapping.wildCardPaths[path]

        path = 'model:claims:${perils}:lines:${lob}:reinsurance:${contracts}:claims'
        assertEquals elements[-2].wildCardPath, mapping.wildCardPaths[path]

        for (OutputElement e : elements) {
            assertEquals e.path, e.getWildCardPath().getSpecificPath(e.getCategoryMap())
        }

        assertEquals 'model:lines:AAA:premium', elements[0].getWildCardPath().getSpecificPath(["lob":"AAA"])
    }

    public testCategorizeInclFields() {
        CategoryMapping mapping = getTestCategoryMappingInclField()
        List<OutputElement> elements = getTestOutputElementsWithField()
        mapping.categorize elements
        assertEquals 8, mapping.getWildCardPaths().size()

        String path = 'model:lines:${lob}:premium'
        List<String> wildCards = mapping.wildCardPaths[path].getPathWildCards()
        assertEquals 1, wildCards.size()
        assertTrue wildCards.contains("lob")
        List<String> values = mapping.wildCardPaths[path].getWildCardValues("lob")
        assertEquals 2, values.size()
        assertTrue values.contains("property")
        assertTrue values.contains("casualty")
        wildCards = mapping.wildCardPaths[path].getAllWildCards()
        assertEquals 2, wildCards.size()
        assertTrue wildCards.contains("lob")
        assertTrue wildCards.contains("keyfigure")
        values = mapping.wildCardPaths[path].getWildCardValues("keyfigure")
        assertEquals 2, values.size()
        assertTrue values.contains("AA")
        assertTrue values.contains("BB")

        path = 'model:claims:${perils}:lines:${lob}:reinsurance:${contracts}:claims'
        wildCards = mapping.wildCardPaths[path].getPathWildCards()
        assertEquals 3, wildCards.size()
        assertTrue wildCards.contains("perils")
        assertTrue wildCards.contains("lob")
        assertTrue wildCards.contains("contracts")
        values = mapping.wildCardPaths[path].getWildCardValues("lob")
        assertEquals 1, values.size()
        assertTrue values.contains("property")
        values = mapping.wildCardPaths[path].getWildCardValues("perils")
        assertEquals 2, values.size()
        assertTrue values.contains("flood")
        assertTrue values.contains("eq")
        values = mapping.wildCardPaths[path].getWildCardValues("contracts")
        assertEquals 2, values.size()
        assertTrue values.contains("CXL")
        assertTrue values.contains("WXL")
        wildCards = mapping.wildCardPaths[path].getAllWildCards()
        assertEquals 4, wildCards.size()
        assertTrue wildCards.contains("perils")
        assertTrue wildCards.contains("lob")
        assertTrue wildCards.contains("contracts")
        assertTrue wildCards.contains("keyfigure")
        values = mapping.wildCardPaths[path].getWildCardValues("keyfigure")
        assertEquals 2, values.size()
        assertTrue values.contains("AA")
        assertTrue values.contains("CC")

        path = 'model:lines:${lob}:premium'
        assertEquals elements[0].wildCardPath, mapping.wildCardPaths[path]

        path = 'model:claims:${perils}:lines:${lob}:reinsurance:${contracts}:claims'
        assertEquals elements[-2].wildCardPath, mapping.wildCardPaths[path]

        for (OutputElement e : elements) {
            assertEquals e.path, e.getWildCardPath().getSpecificPath(e.getCategoryMap())
        }

        assertEquals 'model:lines:AAA:premium', elements[0].getWildCardPath().getSpecificPath(["lob":"AAA"])
    }

    public void testWithTrivialMapping() {
        CategoryMapping mapping = new CategoryMapping()
        List<OutputElement> elements = getTestOutputElements()
        mapping.categorize elements
        assertEquals elements.size(), mapping.getWildCardPaths().size()

        // todo fix problems with mapping.getWildCardPaths().get(0).getPathWildCards != null , etc.
    }
}
