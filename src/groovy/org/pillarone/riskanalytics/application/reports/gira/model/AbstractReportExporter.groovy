package org.pillarone.riskanalytics.application.reports.gira.model

import net.sf.jasperreports.engine.JRExporter
import net.sf.jasperreports.engine.export.JRPdfExporter
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import net.sf.jasperreports.engine.JRExporterParameter
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.util.JRLoader

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
abstract class AbstractReportExporter {

    JRExporter exporter

    public OutputStream export(Map parameters, JRBeanCollectionDataSource collectionDataSource) {
        String reportName = parameters["_file"] + ".jasper"
        URL reportUrl = new URL(GiraReportHelper.getReportFolder().toExternalForm() + "/" + reportName)

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream()
        getExporter().setParameter(JRExporterParameter.OUTPUT_STREAM, byteArray)

        JasperPrint jasperPrint
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportUrl);
            jasperReport.setWhenNoDataType(jasperReport.WHEN_NO_DATA_TYPE_ALL_SECTIONS_NO_DETAIL);
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, collectionDataSource)

        } catch (Exception ex) {
            ex.printStackTrace()
        }

        exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint)
        exporter.exportReport()
        return byteArray

    }


}