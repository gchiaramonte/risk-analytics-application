package org.pillarone.riskanalytics.application.ui.main.action.exportimport

import org.apache.poi.ss.usermodel.Cell

class ImportResult {
    String sheetName
    Integer rowIndex
    Integer columnIndex
    String message
    Type type

    ImportResult(String message, Type type) {
        this(null, null, message, type)
    }

    ImportResult(String sheetName, Integer rowIndex, String message, Type type) {
        this(sheetName, rowIndex, null, message, type)
    }

    ImportResult(Cell cell, String message, Type type) {
        this(cell.sheet.sheetName, cell.rowIndex, cell.columnIndex, message, type)
    }

    ImportResult(String sheetName, Integer rowIndex, Integer columnIndex, String message, Type type) {
        this.sheetName = sheetName
        this.rowIndex = rowIndex
        this.columnIndex = columnIndex
        this.message = message
        this.type = type
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer()
        result.append("$type, ")
        if (sheetName) {
            result.append("sheet=$sheetName, ")
        }
        if (rowIndex != null) {
            result.append("row=$rowIndex, ")
        }
        if (columnIndex != null) {
            result.append("col=$columnIndex, ")
        }
        if (message) {
            result.append(message)
        }
        return result.toString()
    }




    enum Type {
        SUCCESS, ERROR, WARNING
    }
}
