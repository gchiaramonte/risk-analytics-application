package org.pillarone.riskanalytics.application.ui.main.action.exportimport

import models.application.ApplicationModel
import models.core.CoreModel
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.joda.time.DateTime
import org.pillarone.riskanalytics.application.example.component.ExampleDynamicComponent
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.example.parameter.ExampleResourceConstraints
import org.pillarone.riskanalytics.core.parameterization.ConstraintsFactory

class ExcelImportHandlerTests extends GroovyTestCase {
    File exportFile

    @Override
    protected void setUp() throws Exception {
        super.setUp()
        LocaleResources.setTestMode()
        ConstraintsFactory.registerConstraint(new ExampleResourceConstraints())
        exportFile = File.createTempFile('excel', '.xlsx')
        exportFile.bytes = new ExcelExportHandler(new ApplicationModel()).exportModel()
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown()
        LocaleResources.clearTestMode()
    }

    void testIncorrectModelClass() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(1).getCell(1).setCellValue(CoreModel.class.name)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testInvalidModelClass() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(1).getCell(1).setCellValue("NONEXISTINGCLASS")
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testModelInfoNotFound() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        handler.workbook.getSheet(AbstractExcelHandler.META_INFO_SHEET).getRow(1).getCell(1).setCellValue(null as String)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMissingMetaInfoSheet() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        int sheetIndex = handler.workbook.getSheetIndex(AbstractExcelHandler.META_INFO_SHEET)
        handler.workbook.removeSheetAt(sheetIndex)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMissingMDPSheet() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('tableName')
        int sheetIndex = handler.workbook.getSheetIndex('MDP0-ExampleResourceConstraints')
        handler.workbook.removeSheetAt(sheetIndex)
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMissingMDPTableReference() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMissingMDPTableIdentifier() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testIncorrectParameterObjectClassifier() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('UNKNOWN_CLASSIFIER')
        dataRow.createCell(1).setCellValue('FIRST_VALUE')
        dataRow.createCell(10).setCellValue('')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMDPValues() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet parmComponentSheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = parmComponentSheet.createRow(2)
        dataRow.createCell(2).setCellValue('RESOURCE')
        dataRow.createCell(10).setCellValue('tableName')
        XSSFSheet mdpSheet = handler.workbook.getSheet('MDP0-ExampleResourceConstraints')
        mdpSheet.getRow(0).getCell(0).setCellValue('tableName')
        XSSFRow mdpRow = mdpSheet.createRow(2)
        mdpRow.createCell(0).setCellValue('ONE')
        mdpRow = mdpSheet.createRow(3)
        mdpRow.createCell(0).setCellValue('TWO')
        mdpRow = mdpSheet.createRow(4)
        mdpRow.createCell(0).setCellValue('THREE')
        handler.validate(new ApplicationModel())
        assert ['ONE', 'TWO', 'THREE'] == handler.modelInstance.parameterComponent.parmNestedMdp.parameters['resource'].values[0]
    }

    void testIncorrectCellData() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        dataRow.createCell(2).setCellValue('TYPE0')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testMissingCellData() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('parameter Component')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(2).setCellValue('TYPE0')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testAddSubComponent() {
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamic Component')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('DUMMY')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
    }

    void testImportExistingComponent() {
        ApplicationModel model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        ExampleDynamicComponent dynamicComponent = model.dynamicComponent
        Component subComponent = dynamicComponent.createDefaultSubComponent()
        subComponent.name = 'subComponentA'
        dynamicComponent.addSubComponent(subComponent)

        ExcelImportHandler handler = new ExcelImportHandler()

        handler.parameterizedModel = model
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamic Component')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('componentA')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert "Parameterization for component 'componentA' already present. Will be ignored." == result[0].message
    }

    void testImportToNonExistingComponent() {
        ApplicationModel model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        ExampleDynamicComponent dynamicComponent = model.dynamicComponent
        Component subComponent = dynamicComponent.createDefaultSubComponent()
        subComponent.name = 'subComponentA'
        dynamicComponent.addSubComponent(subComponent)

        ExcelImportHandler handler = new ExcelImportHandler()

        handler.parameterizedModel = model
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('dynamic Component')
        XSSFRow dataRow = sheet.createRow(2)
        dataRow.createCell(1).setCellValue('componentB')
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 1 == result.size()
        assert "Component 'componentB' processed." == result[0].message
    }

    void testImportDateFunction() {
        int year = 2012
        int day = 23
        int month = 1
        ExcelImportHandler handler = new ExcelImportHandler()
        handler.loadWorkbook(new FileInputStream(exportFile), "test.xlsx")
        XSSFSheet sheet = handler.workbook.getSheet('Global Parameter Component')
        XSSFRow dataRow = sheet.createRow(2)
        XSSFCell cell = dataRow.createCell(0)
        cell.setCellType(Cell.CELL_TYPE_FORMULA)
        cell.setCellFormula("DATE($year,$month,$day)")
        List<ImportResult> result = handler.validate(new ApplicationModel())
        assert 0 == result.size()
        DateTime date = (handler.modelInstance as ApplicationModel).globalParameterComponent.parmProjectionStartDate
        assert year == date.getYear()
        assert month == date.getMonthOfYear()
        assert day == date.getDayOfMonth()
    }
}