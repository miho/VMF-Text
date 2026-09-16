package eu.mihosoft.vmf.vmftext;

import java.io.File;

/**
 * Options that influence VMF-Text code generation.
 */
public final class GenerationOptions {

    private boolean autoLabel;
    private boolean emitAutoLabelReport = true;
    private File autoLabelReportFile;

    public GenerationOptions() {
        //
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions();
    }

    public boolean isAutoLabel() {
        return autoLabel;
    }

    public GenerationOptions setAutoLabel(boolean autoLabel) {
        this.autoLabel = autoLabel;
        return this;
    }

    public boolean isEmitAutoLabelReport() {
        return emitAutoLabelReport;
    }

    public GenerationOptions setEmitAutoLabelReport(boolean emitAutoLabelReport) {
        this.emitAutoLabelReport = emitAutoLabelReport;
        return this;
    }

    /**
     * Optional destination for the auto-label report. When {@code null} and
     * {@link #isEmitAutoLabelReport()} is true, the report is logged only.
     */
    public File getAutoLabelReportFile() {
        return autoLabelReportFile;
    }

    public GenerationOptions setAutoLabelReportFile(File autoLabelReportFile) {
        this.autoLabelReportFile = autoLabelReportFile;
        return this;
    }
}
